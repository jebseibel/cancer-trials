package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.service.CurrentUserService;
import com.seibel.cancer.service.PatientDiagnosisService;
import com.seibel.cancer.web.request.RequestPatientDiagnosisCreate;
import com.seibel.cancer.web.request.RequestPatientDiagnosisUpdate;
import com.seibel.cancer.web.response.ResponsePatientDiagnosis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patientdiagnosis")
@Validated
@Tag(name = "PatientDiagnosis", description = "PatientDiagnosis CRUD endpoints")
@RequiredArgsConstructor
public class PatientDiagnosisController {

    private final PatientDiagnosisService patientDiagnosisService;
    private final CurrentUserService currentUserService;
    private final PatientDiagnosisConverter converter;

    @GetMapping
    @Operation(summary = "List patientDiagnoses (paginated)")
    public Page<ResponsePatientDiagnosis> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return patientDiagnosisService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get patientDiagnosis by extid")
    public ResponsePatientDiagnosis getByExtid(@PathVariable String extid) {
        return converter.toResponse(patientDiagnosisService.findByExtid(extid));
    }

    /**
     * The diagnosis for one patient.
     *
     * <p>Requires VIEW_RECORD: this is clinical detail, so a VIEW_TRIALS grantee - someone
     * helping look for trials - is refused with a 404 like any other caller without access.
     */
    @GetMapping("/by-patient/{patientExtid}")
    @Operation(summary = "List all patientDiagnoses for a patient (unpaginated)")
    public List<ResponsePatientDiagnosis> getByPatientExtid(@PathVariable String patientExtid) {
        Long patientId = currentUserService.requireAccessId(patientExtid, AccessLevel.VIEW_RECORD);
        return converter.toResponse(patientDiagnosisService.findByPatientId(patientId));
    }

    @PostMapping
    @Operation(summary = "Create patientDiagnosis")
    public ResponseEntity<ResponsePatientDiagnosis> create(@Valid @RequestBody RequestPatientDiagnosisCreate request) {
        PatientDiagnosis created = patientDiagnosisService.create(converter.toDomain(request));
        URI location = URI.create("/api/patientdiagnosis/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update patientDiagnosis (full or partial)")
    public ResponsePatientDiagnosis update(@PathVariable String extid, @Valid @RequestBody RequestPatientDiagnosisUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(patientDiagnosisService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update patientDiagnosis")
    public ResponsePatientDiagnosis patch(@PathVariable String extid, @Valid @RequestBody RequestPatientDiagnosisUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete patientDiagnosis")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return patientDiagnosisService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

@Component
@RequiredArgsConstructor
class PatientDiagnosisConverter {

    private final PatientRepository patientRepository;

    Long resolvePatientId(String patientExtid) {
        return patientRepository.findByExtid(patientExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientExtid))
                .getId();
    }

    private String resolvePatientExtid(Long patientId) {
        if (patientId == null) return null;
        return patientRepository.findById(patientId).map(p -> p.getExtid()).orElse(null);
    }

    PatientDiagnosis toDomain(RequestPatientDiagnosisCreate request) {
        return PatientDiagnosis.builder()
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .cancerType(request.getCancerType())
                .stage(request.getStage())
                .stageSystem(request.getStageSystem())
                .isMetastatic(request.getIsMetastatic())
                .metastasisSites(request.getMetastasisSites())
                .receptorSubtype(request.getReceptorSubtype())
                .erStatus(request.getErStatus())
                .prStatus(request.getPrStatus())
                .her2Status(request.getHer2Status())
                .biomarkers(request.getBiomarkers())
                .ecogStatus(request.getEcogStatus())
                .priorChemoRegimens(request.getPriorChemoRegimens())
                .lastChemoEndDate(request.getLastChemoEndDate())
                .priorTreatments(request.getPriorTreatments())
                .hasMeasurableDisease(request.getHasMeasurableDisease())
                .menopausalStatus(request.getMenopausalStatus())
                .diagnosisDate(request.getDiagnosisDate())
                .notes(request.getNotes())
                .build();
    }

    PatientDiagnosis toDomain(RequestPatientDiagnosisUpdate request) {
        return PatientDiagnosis.builder()
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .cancerType(request.getCancerType())
                .stage(request.getStage())
                .stageSystem(request.getStageSystem())
                .isMetastatic(request.getIsMetastatic())
                .metastasisSites(request.getMetastasisSites())
                .receptorSubtype(request.getReceptorSubtype())
                .erStatus(request.getErStatus())
                .prStatus(request.getPrStatus())
                .her2Status(request.getHer2Status())
                .biomarkers(request.getBiomarkers())
                .ecogStatus(request.getEcogStatus())
                .priorChemoRegimens(request.getPriorChemoRegimens())
                .lastChemoEndDate(request.getLastChemoEndDate())
                .priorTreatments(request.getPriorTreatments())
                .hasMeasurableDisease(request.getHasMeasurableDisease())
                .menopausalStatus(request.getMenopausalStatus())
                .diagnosisDate(request.getDiagnosisDate())
                .notes(request.getNotes())
                .build();
    }

    ResponsePatientDiagnosis toResponse(PatientDiagnosis item) {
        return ResponsePatientDiagnosis.builder()
                .extid(item.getExtid())
                .patientExtid(resolvePatientExtid(item.getPatientId()))
                .cancerType(item.getCancerType())
                .stage(item.getStage())
                .stageSystem(item.getStageSystem())
                .isMetastatic(item.getIsMetastatic())
                .metastasisSites(item.getMetastasisSites())
                .receptorSubtype(item.getReceptorSubtype())
                .erStatus(item.getErStatus())
                .prStatus(item.getPrStatus())
                .her2Status(item.getHer2Status())
                .biomarkers(item.getBiomarkers())
                .ecogStatus(item.getEcogStatus())
                .priorChemoRegimens(item.getPriorChemoRegimens())
                .lastChemoEndDate(item.getLastChemoEndDate())
                .priorTreatments(item.getPriorTreatments())
                .hasMeasurableDisease(item.getHasMeasurableDisease())
                .menopausalStatus(item.getMenopausalStatus())
                .diagnosisDate(item.getDiagnosisDate())
                .notes(item.getNotes())
                .build();
    }

    List<ResponsePatientDiagnosis> toResponse(List<PatientDiagnosis> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientDiagnosisUpdate request) {
        if (request.getPatientExtid() == null
                && request.getCancerType() == null
                && request.getStage() == null
                && request.getStageSystem() == null
                && request.getIsMetastatic() == null
                && request.getMetastasisSites() == null
                && request.getReceptorSubtype() == null
                && request.getErStatus() == null
                && request.getPrStatus() == null
                && request.getHer2Status() == null
                && request.getBiomarkers() == null
                && request.getEcogStatus() == null
                && request.getPriorChemoRegimens() == null
                && request.getLastChemoEndDate() == null
                && request.getPriorTreatments() == null
                && request.getHasMeasurableDisease() == null
                && request.getMenopausalStatus() == null
                && request.getDiagnosisDate() == null
                && request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
