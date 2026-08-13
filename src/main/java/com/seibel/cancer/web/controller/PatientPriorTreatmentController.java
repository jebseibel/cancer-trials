package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.service.CurrentUserService;
import com.seibel.cancer.database.db.repository.PatientDiagnosisRepository;
import com.seibel.cancer.service.PatientPriorTreatmentService;
import com.seibel.cancer.web.request.RequestPatientPriorTreatmentCreate;
import com.seibel.cancer.web.request.RequestPatientPriorTreatmentUpdate;
import com.seibel.cancer.web.response.ResponsePatientPriorTreatment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patientpriortreatment")
@Validated
@Tag(name = "PatientPriorTreatment", description = "PatientPriorTreatment CRUD endpoints")
public class PatientPriorTreatmentController {

    private final PatientPriorTreatmentService patientPriorTreatmentService;
    private final CurrentUserService currentUserService;
    private final PatientPriorTreatmentConverter converter;

    public PatientPriorTreatmentController(PatientPriorTreatmentService patientPriorTreatmentService,
                                           CurrentUserService currentUserService,
                                           PatientRepository patientRepository,
                                           PatientDiagnosisRepository patientDiagnosisRepository) {
        this.patientPriorTreatmentService = patientPriorTreatmentService;
        this.currentUserService = currentUserService;
        this.converter = new PatientPriorTreatmentConverter(patientRepository, patientDiagnosisRepository);
    }

    @GetMapping
    @Operation(summary = "List patientPriorTreatments (paginated)")
    public Page<ResponsePatientPriorTreatment> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active) {
        return patientPriorTreatmentService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get a patientPriorTreatment by extid")
    public ResponsePatientPriorTreatment getByExtid(@PathVariable String extid) {
        return converter.toResponse(patientPriorTreatmentService.findByExtid(extid));
    }

    @GetMapping("/by-patient/{patientExtid}")
    @Operation(summary = "Get the treatment history for one app user")
    public List<ResponsePatientPriorTreatment> getByPatientExtid(@PathVariable String patientExtid) {
        Long patientId = currentUserService.requireAccessId(patientExtid, AccessLevel.VIEW_RECORD);
        return converter.toResponse(patientPriorTreatmentService.findByPatientId(patientId));
    }

    @GetMapping("/by-diagnosis/{patientDiagnosisExtid}")
    @Operation(summary = "Get the treatment history recorded against one diagnosis")
    public List<ResponsePatientPriorTreatment> getByPatientDiagnosisExtid(@PathVariable String patientDiagnosisExtid) {
        Long patientDiagnosisId = converter.resolvePatientDiagnosisId(patientDiagnosisExtid);
        return converter.toResponse(patientPriorTreatmentService.findByPatientDiagnosisId(patientDiagnosisId));
    }

    @PostMapping
    @Operation(summary = "Create a patientPriorTreatment")
    public ResponseEntity<ResponsePatientPriorTreatment> create(
            @Valid @RequestBody RequestPatientPriorTreatmentCreate request) {
        PatientPriorTreatment created = patientPriorTreatmentService.create(converter.toDomain(request));
        URI location = URI.create("/api/patientpriortreatment/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update a patientPriorTreatment")
    public ResponsePatientPriorTreatment update(@PathVariable String extid,
                                                @Valid @RequestBody RequestPatientPriorTreatmentUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(patientPriorTreatmentService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update a patientPriorTreatment")
    public ResponsePatientPriorTreatment patch(@PathVariable String extid,
                                               @Valid @RequestBody RequestPatientPriorTreatmentUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete a patientPriorTreatment")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return patientPriorTreatmentService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class PatientPriorTreatmentConverter {

    private final PatientRepository patientRepository;
    private final PatientDiagnosisRepository patientDiagnosisRepository;

    PatientPriorTreatmentConverter(PatientRepository patientRepository,
                                   PatientDiagnosisRepository patientDiagnosisRepository) {
        this.patientRepository = patientRepository;
        this.patientDiagnosisRepository = patientDiagnosisRepository;
    }

    Long resolvePatientId(String patientExtid) {
        return patientRepository.findByExtid(patientExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientExtid))
                .getId();
    }

    Long resolvePatientDiagnosisId(String patientDiagnosisExtid) {
        return patientDiagnosisRepository.findByExtid(patientDiagnosisExtid)
                .orElseThrow(() -> new ResourceNotFoundException("PatientDiagnosis", patientDiagnosisExtid))
                .getId();
    }

    private String resolvePatientExtid(Long patientId) {
        if (patientId == null) return null;
        return patientRepository.findById(patientId).map(u -> u.getExtid()).orElse(null);
    }

    private String resolvePatientDiagnosisExtid(Long patientDiagnosisId) {
        if (patientDiagnosisId == null) return null;
        return patientDiagnosisRepository.findById(patientDiagnosisId).map(d -> d.getExtid()).orElse(null);
    }

    PatientPriorTreatment toDomain(RequestPatientPriorTreatmentCreate request) {
        return PatientPriorTreatment.builder()
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .cdk46Status(request.getCdk46Status())
                .endocrineStatus(request.getEndocrineStatus())
                .serdStatus(request.getSerdStatus())
                .chemoStatus(request.getChemoStatus())
                .her2TherapyStatus(request.getHer2TherapyStatus())
                .her2AdcStatus(request.getHer2AdcStatus())
                .trop2AdcStatus(request.getTrop2AdcStatus())
                .parpStatus(request.getParpStatus())
                .pi3kAktMtorStatus(request.getPi3kAktMtorStatus())
                .immunotherapyStatus(request.getImmunotherapyStatus())
                .taxaneStatus(request.getTaxaneStatus())
                .anthracyclineStatus(request.getAnthracyclineStatus())
                .platinumStatus(request.getPlatinumStatus())
                .currentDrugNames(request.getCurrentDrugNames())
                .priorDrugNames(request.getPriorDrugNames())
                .linesOfTherapyMetastatic(request.getLinesOfTherapyMetastatic())
                .hadNeoadjuvant(request.getHadNeoadjuvant())
                .hadAdjuvant(request.getHadAdjuvant())
                .hadRadiation(request.getHadRadiation())
                .hadSurgery(request.getHadSurgery())
                .lastTreatmentEndDate(request.getLastTreatmentEndDate())
                .currentlyOnTreatment(request.getCurrentlyOnTreatment())
                .otherTreatments(request.getOtherTreatments())
                .notes(request.getNotes())
                .build();
    }

    PatientPriorTreatment toDomain(RequestPatientPriorTreatmentUpdate request) {
        return PatientPriorTreatment.builder()
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .cdk46Status(request.getCdk46Status())
                .endocrineStatus(request.getEndocrineStatus())
                .serdStatus(request.getSerdStatus())
                .chemoStatus(request.getChemoStatus())
                .her2TherapyStatus(request.getHer2TherapyStatus())
                .her2AdcStatus(request.getHer2AdcStatus())
                .trop2AdcStatus(request.getTrop2AdcStatus())
                .parpStatus(request.getParpStatus())
                .pi3kAktMtorStatus(request.getPi3kAktMtorStatus())
                .immunotherapyStatus(request.getImmunotherapyStatus())
                .taxaneStatus(request.getTaxaneStatus())
                .anthracyclineStatus(request.getAnthracyclineStatus())
                .platinumStatus(request.getPlatinumStatus())
                .currentDrugNames(request.getCurrentDrugNames())
                .priorDrugNames(request.getPriorDrugNames())
                .linesOfTherapyMetastatic(request.getLinesOfTherapyMetastatic())
                .hadNeoadjuvant(request.getHadNeoadjuvant())
                .hadAdjuvant(request.getHadAdjuvant())
                .hadRadiation(request.getHadRadiation())
                .hadSurgery(request.getHadSurgery())
                .lastTreatmentEndDate(request.getLastTreatmentEndDate())
                .currentlyOnTreatment(request.getCurrentlyOnTreatment())
                .otherTreatments(request.getOtherTreatments())
                .notes(request.getNotes())
                .build();
    }

    ResponsePatientPriorTreatment toResponse(PatientPriorTreatment item) {
        return ResponsePatientPriorTreatment.builder()
                .extid(item.getExtid())
                .patientExtid(resolvePatientExtid(item.getPatientId()))
                .patientDiagnosisExtid(resolvePatientDiagnosisExtid(item.getPatientDiagnosisId()))
                .cdk46Status(item.getCdk46Status())
                .endocrineStatus(item.getEndocrineStatus())
                .serdStatus(item.getSerdStatus())
                .chemoStatus(item.getChemoStatus())
                .her2TherapyStatus(item.getHer2TherapyStatus())
                .her2AdcStatus(item.getHer2AdcStatus())
                .trop2AdcStatus(item.getTrop2AdcStatus())
                .parpStatus(item.getParpStatus())
                .pi3kAktMtorStatus(item.getPi3kAktMtorStatus())
                .immunotherapyStatus(item.getImmunotherapyStatus())
                .taxaneStatus(item.getTaxaneStatus())
                .anthracyclineStatus(item.getAnthracyclineStatus())
                .platinumStatus(item.getPlatinumStatus())
                .currentDrugNames(item.getCurrentDrugNames())
                .priorDrugNames(item.getPriorDrugNames())
                .linesOfTherapyMetastatic(item.getLinesOfTherapyMetastatic())
                .hadNeoadjuvant(item.getHadNeoadjuvant())
                .hadAdjuvant(item.getHadAdjuvant())
                .hadRadiation(item.getHadRadiation())
                .hadSurgery(item.getHadSurgery())
                .lastTreatmentEndDate(item.getLastTreatmentEndDate())
                .currentlyOnTreatment(item.getCurrentlyOnTreatment())
                .otherTreatments(item.getOtherTreatments())
                .notes(item.getNotes())
                .build();
    }

    List<ResponsePatientPriorTreatment> toResponse(List<PatientPriorTreatment> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientPriorTreatmentUpdate request) {
        if (request.getPatientExtid() == null
                && request.getPatientDiagnosisExtid() == null
                && request.getCdk46Status() == null
                && request.getEndocrineStatus() == null
                && request.getSerdStatus() == null
                && request.getChemoStatus() == null
                && request.getHer2TherapyStatus() == null
                && request.getHer2AdcStatus() == null
                && request.getTrop2AdcStatus() == null
                && request.getParpStatus() == null
                && request.getPi3kAktMtorStatus() == null
                && request.getImmunotherapyStatus() == null
                && request.getTaxaneStatus() == null
                && request.getAnthracyclineStatus() == null
                && request.getPlatinumStatus() == null
                && request.getCurrentDrugNames() == null
                && request.getPriorDrugNames() == null
                && request.getLinesOfTherapyMetastatic() == null
                && request.getHadNeoadjuvant() == null
                && request.getHadAdjuvant() == null
                && request.getHadRadiation() == null
                && request.getHadSurgery() == null
                && request.getLastTreatmentEndDate() == null
                && request.getCurrentlyOnTreatment() == null
                && request.getOtherTreatments() == null
                && request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
