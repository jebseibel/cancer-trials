package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.AppUserRepository;
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

    @GetMapping("/by-appuser/{appUserExtid}")
    @Operation(summary = "List all patientDiagnoses for an app user (unpaginated)")
    public List<ResponsePatientDiagnosis> getByAppUserExtid(@PathVariable String appUserExtid) {
        Long appUserId = converter.resolveAppUserId(appUserExtid);
        return converter.toResponse(patientDiagnosisService.findByAppUserId(appUserId));
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

    private final AppUserRepository appUserRepository;

    Long resolveAppUserId(String appUserExtid) {
        return appUserRepository.findByExtid(appUserExtid)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", appUserExtid))
                .getId();
    }

    private String resolveAppUserExtid(Long appUserId) {
        if (appUserId == null) return null;
        return appUserRepository.findById(appUserId).map(u -> u.getExtid()).orElse(null);
    }

    PatientDiagnosis toDomain(RequestPatientDiagnosisCreate request) {
        return PatientDiagnosis.builder()
                .appUserId(request.getAppUserExtid() != null ? resolveAppUserId(request.getAppUserExtid()) : null)
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
                .dateOfBirth(request.getDateOfBirth())
                .sex(request.getSex())
                .diagnosisDate(request.getDiagnosisDate())
                .notes(request.getNotes())
                .build();
    }

    PatientDiagnosis toDomain(RequestPatientDiagnosisUpdate request) {
        return PatientDiagnosis.builder()
                .appUserId(request.getAppUserExtid() != null ? resolveAppUserId(request.getAppUserExtid()) : null)
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
                .dateOfBirth(request.getDateOfBirth())
                .sex(request.getSex())
                .diagnosisDate(request.getDiagnosisDate())
                .notes(request.getNotes())
                .build();
    }

    ResponsePatientDiagnosis toResponse(PatientDiagnosis item) {
        return ResponsePatientDiagnosis.builder()
                .extid(item.getExtid())
                .appUserExtid(resolveAppUserExtid(item.getAppUserId()))
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
                .dateOfBirth(item.getDateOfBirth())
                .sex(item.getSex())
                .diagnosisDate(item.getDiagnosisDate())
                .notes(item.getNotes())
                .build();
    }

    List<ResponsePatientDiagnosis> toResponse(List<PatientDiagnosis> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientDiagnosisUpdate request) {
        if (request.getAppUserExtid() == null
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
                && request.getDateOfBirth() == null
                && request.getSex() == null
                && request.getDiagnosisDate() == null
                && request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
