package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.CurrentUserService;
import com.seibel.cancer.service.PatientService;
import com.seibel.cancer.web.request.RequestPatientCreate;
import com.seibel.cancer.web.request.RequestPatientUpdate;
import com.seibel.cancer.web.response.ResponsePatient;
import com.seibel.cancer.web.response.ResponsePatientAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Patients, scoped to what the caller may see.
 *
 * <p><strong>Every route here is authorised through {@link CurrentUserService}</strong>, which
 * resolves the caller from the JWT and checks a grant. A caller with no grant to a patient gets
 * a <strong>404, never a 403</strong> - a 403 would confirm the extid names a real person's
 * record, which is a disclosure in itself.
 *
 * <p>Note what is absent: there is no "list every patient" endpoint. {@code /mine} is the
 * listing, and it names nobody.
 *
 * <p>Design and rationale in {@code .claude/access/PATIENT_ACCESS_PLAN.md}.
 */
@RestController
@RequestMapping("/api/patient")
@Validated
@Tag(name = "Patient", description = "Patient CRUD endpoints")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final CurrentUserService currentUserService;
    private final PatientConverter converter = new PatientConverter();

    /**
     * The patients the caller may see, best access first.
     *
     * <p>This is the endpoint the frontend should use - it names nobody, so there is no extid
     * for a caller to substitute. Everything else on this controller still takes a target from
     * the URL.
     */
    @GetMapping("/mine")
    @Operation(summary = "List the patients the signed-in user may see")
    public List<ResponsePatientAccess> getMine() {
        return currentUserService.myPatients().stream()
                .map(pa -> converter.toResponse(pa.patient(), pa.accessLevel()))
                .toList();
    }

    // There is deliberately no "list every patient" endpoint. It would return other people's
    // records to any authenticated caller, which is the exact gap this whole change closes.
    // `/mine` is the listing, and it is scoped to the caller's own grants.

    /** Authorised: a caller with no grant to this patient gets a 404, never a 403. */
    @GetMapping("/{extid}")
    @Operation(summary = "Get a patient by extid")
    public ResponsePatient getByExtid(@PathVariable String extid) {
        return converter.toResponse(currentUserService.requireAccess(extid, AccessLevel.VIEW_TRIALS));
    }

    /**
     * Create a patient, owned by the caller.
     *
     * <p>The OWNER grant is written here rather than left to the caller: a patient with no
     * grant is invisible to everyone including its creator, so creating one without it would
     * silently produce an unreachable record.
     */
    @PostMapping
    @Operation(summary = "Create a patient owned by the signed-in user")
    public ResponseEntity<ResponsePatient> create(@Valid @RequestBody RequestPatientCreate request) {
        Patient created = patientService.createOwnedByCurrentUser(converter.toDomain(request));
        URI location = URI.create("/api/patient/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update a patient")
    public ResponsePatient update(@PathVariable String extid,
                                  @Valid @RequestBody RequestPatientUpdate request) {
        currentUserService.requireAccess(extid, AccessLevel.EDIT_RECORD);
        converter.validateUpdateRequest(request);
        return converter.toResponse(patientService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update a patient")
    public ResponsePatient patch(@PathVariable String extid,
                                 @Valid @RequestBody RequestPatientUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete a patient (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        // OWNER only: deleting a record is not something a granted editor should be able to do.
        currentUserService.requireAccess(extid, AccessLevel.OWNER);
        return patientService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class PatientConverter {

    Patient toDomain(RequestPatientCreate request) {
        return Patient.builder()
                .displayName(request.getDisplayName())
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .sex(request.getSex())
                .notes(request.getNotes())
                .build();
    }

    Patient toDomain(RequestPatientUpdate request) {
        return Patient.builder()
                .displayName(request.getDisplayName())
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .sex(request.getSex())
                .notes(request.getNotes())
                .build();
    }

    ResponsePatient toResponse(Patient item) {
        return ResponsePatient.builder()
                .extid(item.getExtid())
                .displayName(item.getDisplayName())
                .fullName(item.getFullName())
                .dateOfBirth(item.getDateOfBirth())
                .sex(item.getSex())
                .notes(item.getNotes())
                .build();
    }

    ResponsePatientAccess toResponse(Patient item, AccessLevel accessLevel) {
        return ResponsePatientAccess.builder()
                .extid(item.getExtid())
                .displayName(item.getDisplayName())
                .fullName(item.getFullName())
                .dateOfBirth(item.getDateOfBirth())
                .sex(item.getSex())
                .notes(item.getNotes())
                .accessLevel(accessLevel)
                .build();
    }

    List<ResponsePatient> toResponse(List<Patient> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientUpdate request) {
        if (request.getDisplayName() == null
                && request.getFullName() == null
                && request.getDateOfBirth() == null
                && request.getSex() == null
                && request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
