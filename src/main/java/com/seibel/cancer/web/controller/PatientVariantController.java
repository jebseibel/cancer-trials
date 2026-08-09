package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.AppUserRepository;
import com.seibel.cancer.database.db.repository.PatientDiagnosisRepository;
import com.seibel.cancer.service.PatientVariantService;
import com.seibel.cancer.web.request.RequestPatientVariantCreate;
import com.seibel.cancer.web.request.RequestPatientVariantUpdate;
import com.seibel.cancer.web.response.ResponsePatientVariant;
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
@RequestMapping("/api/patientvariant")
@Validated
@Tag(name = "PatientVariant", description = "PatientVariant CRUD endpoints")
public class PatientVariantController {

    private final PatientVariantService patientVariantService;
    private final PatientVariantConverter converter;

    public PatientVariantController(PatientVariantService patientVariantService,
                                    AppUserRepository appUserRepository,
                                    PatientDiagnosisRepository patientDiagnosisRepository) {
        this.patientVariantService = patientVariantService;
        this.converter = new PatientVariantConverter(appUserRepository, patientDiagnosisRepository);
    }

    @GetMapping
    @Operation(summary = "List patientVariants (paginated)")
    public Page<ResponsePatientVariant> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active) {
        return patientVariantService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get a patientVariant by extid")
    public ResponsePatientVariant getByExtid(@PathVariable String extid) {
        return converter.toResponse(patientVariantService.findByExtid(extid));
    }

    @GetMapping("/by-appuser/{appUserExtid}")
    @Operation(summary = "Get the variant findings for one app user")
    public List<ResponsePatientVariant> getByAppUserExtid(@PathVariable String appUserExtid) {
        Long appUserId = converter.resolveAppUserId(appUserExtid);
        return converter.toResponse(patientVariantService.findByAppUserId(appUserId));
    }

    @GetMapping("/by-diagnosis/{patientDiagnosisExtid}")
    @Operation(summary = "Get the variant findings recorded against one diagnosis")
    public List<ResponsePatientVariant> getByPatientDiagnosisExtid(@PathVariable String patientDiagnosisExtid) {
        Long patientDiagnosisId = converter.resolvePatientDiagnosisId(patientDiagnosisExtid);
        return converter.toResponse(patientVariantService.findByPatientDiagnosisId(patientDiagnosisId));
    }

    @PostMapping
    @Operation(summary = "Create a patientVariant")
    public ResponseEntity<ResponsePatientVariant> create(@Valid @RequestBody RequestPatientVariantCreate request) {
        PatientVariant created = patientVariantService.create(converter.toDomain(request));
        URI location = URI.create("/api/patientvariant/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update a patientVariant")
    public ResponsePatientVariant update(@PathVariable String extid,
                                         @Valid @RequestBody RequestPatientVariantUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(patientVariantService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update a patientVariant")
    public ResponsePatientVariant patch(@PathVariable String extid,
                                        @Valid @RequestBody RequestPatientVariantUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete a patientVariant")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return patientVariantService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class PatientVariantConverter {

    private final AppUserRepository appUserRepository;
    private final PatientDiagnosisRepository patientDiagnosisRepository;

    PatientVariantConverter(AppUserRepository appUserRepository,
                            PatientDiagnosisRepository patientDiagnosisRepository) {
        this.appUserRepository = appUserRepository;
        this.patientDiagnosisRepository = patientDiagnosisRepository;
    }

    Long resolveAppUserId(String appUserExtid) {
        return appUserRepository.findByExtid(appUserExtid)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", appUserExtid))
                .getId();
    }

    Long resolvePatientDiagnosisId(String patientDiagnosisExtid) {
        return patientDiagnosisRepository.findByExtid(patientDiagnosisExtid)
                .orElseThrow(() -> new ResourceNotFoundException("PatientDiagnosis", patientDiagnosisExtid))
                .getId();
    }

    private String resolveAppUserExtid(Long appUserId) {
        if (appUserId == null) return null;
        return appUserRepository.findById(appUserId).map(u -> u.getExtid()).orElse(null);
    }

    private String resolvePatientDiagnosisExtid(Long patientDiagnosisId) {
        if (patientDiagnosisId == null) return null;
        return patientDiagnosisRepository.findById(patientDiagnosisId).map(d -> d.getExtid()).orElse(null);
    }

    PatientVariant toDomain(RequestPatientVariantCreate request) {
        return PatientVariant.builder()
                .appUserId(request.getAppUserExtid() != null ? resolveAppUserId(request.getAppUserExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .pik3caStatus(request.getPik3caStatus())
                .esr1Status(request.getEsr1Status())
                .tp53Status(request.getTp53Status())
                .akt1Status(request.getAkt1Status())
                .ptenStatus(request.getPtenStatus())
                .erbb2SomaticStatus(request.getErbb2SomaticStatus())
                .brca1Status(request.getBrca1Status())
                .brca2Status(request.getBrca2Status())
                .palb2Status(request.getPalb2Status())
                .atmStatus(request.getAtmStatus())
                .chek2Status(request.getChek2Status())
                .hrdStatus(request.getHrdStatus())
                .pdl1Status(request.getPdl1Status())
                .ki67Percent(request.getKi67Percent())
                .germlineTestDone(request.getGermlineTestDone())
                .somaticTestDone(request.getSomaticTestDone())
                .testDate(request.getTestDate())
                .testLab(request.getTestLab())
                .otherVariants(request.getOtherVariants())
                .notes(request.getNotes())
                .build();
    }

    PatientVariant toDomain(RequestPatientVariantUpdate request) {
        return PatientVariant.builder()
                .appUserId(request.getAppUserExtid() != null ? resolveAppUserId(request.getAppUserExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .pik3caStatus(request.getPik3caStatus())
                .esr1Status(request.getEsr1Status())
                .tp53Status(request.getTp53Status())
                .akt1Status(request.getAkt1Status())
                .ptenStatus(request.getPtenStatus())
                .erbb2SomaticStatus(request.getErbb2SomaticStatus())
                .brca1Status(request.getBrca1Status())
                .brca2Status(request.getBrca2Status())
                .palb2Status(request.getPalb2Status())
                .atmStatus(request.getAtmStatus())
                .chek2Status(request.getChek2Status())
                .hrdStatus(request.getHrdStatus())
                .pdl1Status(request.getPdl1Status())
                .ki67Percent(request.getKi67Percent())
                .germlineTestDone(request.getGermlineTestDone())
                .somaticTestDone(request.getSomaticTestDone())
                .testDate(request.getTestDate())
                .testLab(request.getTestLab())
                .otherVariants(request.getOtherVariants())
                .notes(request.getNotes())
                .build();
    }

    ResponsePatientVariant toResponse(PatientVariant item) {
        return ResponsePatientVariant.builder()
                .extid(item.getExtid())
                .appUserExtid(resolveAppUserExtid(item.getAppUserId()))
                .patientDiagnosisExtid(resolvePatientDiagnosisExtid(item.getPatientDiagnosisId()))
                .pik3caStatus(item.getPik3caStatus())
                .esr1Status(item.getEsr1Status())
                .tp53Status(item.getTp53Status())
                .akt1Status(item.getAkt1Status())
                .ptenStatus(item.getPtenStatus())
                .erbb2SomaticStatus(item.getErbb2SomaticStatus())
                .brca1Status(item.getBrca1Status())
                .brca2Status(item.getBrca2Status())
                .palb2Status(item.getPalb2Status())
                .atmStatus(item.getAtmStatus())
                .chek2Status(item.getChek2Status())
                .hrdStatus(item.getHrdStatus())
                .pdl1Status(item.getPdl1Status())
                .ki67Percent(item.getKi67Percent())
                .germlineTestDone(item.getGermlineTestDone())
                .somaticTestDone(item.getSomaticTestDone())
                .testDate(item.getTestDate())
                .testLab(item.getTestLab())
                .otherVariants(item.getOtherVariants())
                .notes(item.getNotes())
                .build();
    }

    List<ResponsePatientVariant> toResponse(List<PatientVariant> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientVariantUpdate request) {
        if (request.getAppUserExtid() == null
                && request.getPatientDiagnosisExtid() == null
                && request.getPik3caStatus() == null
                && request.getEsr1Status() == null
                && request.getTp53Status() == null
                && request.getAkt1Status() == null
                && request.getPtenStatus() == null
                && request.getErbb2SomaticStatus() == null
                && request.getBrca1Status() == null
                && request.getBrca2Status() == null
                && request.getPalb2Status() == null
                && request.getAtmStatus() == null
                && request.getChek2Status() == null
                && request.getHrdStatus() == null
                && request.getPdl1Status() == null
                && request.getKi67Percent() == null
                && request.getGermlineTestDone() == null
                && request.getSomaticTestDone() == null
                && request.getTestDate() == null
                && request.getTestLab() == null
                && request.getOtherVariants() == null
                && request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
