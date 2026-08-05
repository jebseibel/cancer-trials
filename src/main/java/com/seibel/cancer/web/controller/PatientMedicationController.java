package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.PatientMedicationService;
import com.seibel.cancer.web.request.RequestPatientMedicationCreate;
import com.seibel.cancer.web.request.RequestPatientMedicationUpdate;
import com.seibel.cancer.web.response.ResponsePatientMedication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patientmedication")
@Validated
@Tag(name = "PatientMedication", description = "PatientMedication CRUD endpoints")
@RequiredArgsConstructor
public class PatientMedicationController {

    private final PatientMedicationService patientMedicationService;
    private final PatientMedicationConverter converter = new PatientMedicationConverter();

    @GetMapping
    @Operation(summary = "List patientMedications (paginated)")
    public Page<ResponsePatientMedication> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "medicationName") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return patientMedicationService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get patientMedication by extid")
    public ResponsePatientMedication getByExtid(@PathVariable String extid) {
        return converter.toResponse(patientMedicationService.findByExtid(extid));
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "List patientMedications by FHIR status (active, completed, stopped, ...)")
    public List<ResponsePatientMedication> getByStatus(@PathVariable String status) {
        return converter.toResponse(patientMedicationService.findByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create patientMedication")
    public ResponseEntity<ResponsePatientMedication> create(@Valid @RequestBody RequestPatientMedicationCreate request) {
        PatientMedication created = patientMedicationService.create(converter.toDomain(request));
        URI location = URI.create("/api/patientmedication/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update patientMedication (full or partial)")
    public ResponsePatientMedication update(@PathVariable String extid, @Valid @RequestBody RequestPatientMedicationUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(patientMedicationService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update patientMedication")
    public ResponsePatientMedication patch(@PathVariable String extid, @Valid @RequestBody RequestPatientMedicationUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete patientMedication")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return patientMedicationService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class PatientMedicationConverter {

    PatientMedication toDomain(RequestPatientMedicationCreate request) {
        return PatientMedication.builder()
                .fhirResourceId(request.getFhirResourceId())
                .medicationName(request.getMedicationName())
                .rxnormCode(request.getRxnormCode())
                .status(request.getStatus())
                .intent(request.getIntent())
                .authoredOn(request.getAuthoredOn())
                .dosageText(request.getDosageText())
                .doseQuantity(request.getDoseQuantity())
                .doseUnit(request.getDoseUnit())
                .route(request.getRoute())
                .frequencyText(request.getFrequencyText())
                .prescriberName(request.getPrescriberName())
                .reasonText(request.getReasonText())
                .validityStart(request.getValidityStart())
                .validityEnd(request.getValidityEnd())
                .refillsAllowed(request.getRefillsAllowed())
                .displayText(request.getDisplayText())
                .build();
    }

    PatientMedication toDomain(RequestPatientMedicationUpdate request) {
        return PatientMedication.builder()
                .fhirResourceId(request.getFhirResourceId())
                .medicationName(request.getMedicationName())
                .rxnormCode(request.getRxnormCode())
                .status(request.getStatus())
                .intent(request.getIntent())
                .authoredOn(request.getAuthoredOn())
                .dosageText(request.getDosageText())
                .doseQuantity(request.getDoseQuantity())
                .doseUnit(request.getDoseUnit())
                .route(request.getRoute())
                .frequencyText(request.getFrequencyText())
                .prescriberName(request.getPrescriberName())
                .reasonText(request.getReasonText())
                .validityStart(request.getValidityStart())
                .validityEnd(request.getValidityEnd())
                .refillsAllowed(request.getRefillsAllowed())
                .displayText(request.getDisplayText())
                .build();
    }

    ResponsePatientMedication toResponse(PatientMedication item) {
        return ResponsePatientMedication.builder()
                .extid(item.getExtid())
                .fhirResourceId(item.getFhirResourceId())
                .medicationName(item.getMedicationName())
                .rxnormCode(item.getRxnormCode())
                .status(item.getStatus())
                .intent(item.getIntent())
                .authoredOn(item.getAuthoredOn())
                .dosageText(item.getDosageText())
                .doseQuantity(item.getDoseQuantity())
                .doseUnit(item.getDoseUnit())
                .route(item.getRoute())
                .frequencyText(item.getFrequencyText())
                .prescriberName(item.getPrescriberName())
                .reasonText(item.getReasonText())
                .validityStart(item.getValidityStart())
                .validityEnd(item.getValidityEnd())
                .refillsAllowed(item.getRefillsAllowed())
                .displayText(item.getDisplayText())
                .build();
    }

    List<ResponsePatientMedication> toResponse(List<PatientMedication> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPatientMedicationUpdate request) {
        if (request.getFhirResourceId() == null
                && request.getMedicationName() == null
                && request.getRxnormCode() == null
                && request.getStatus() == null
                && request.getIntent() == null
                && request.getAuthoredOn() == null
                && request.getDosageText() == null
                && request.getDoseQuantity() == null
                && request.getDoseUnit() == null
                && request.getRoute() == null
                && request.getFrequencyText() == null
                && request.getPrescriberName() == null
                && request.getReasonText() == null
                && request.getValidityStart() == null
                && request.getValidityEnd() == null
                && request.getRefillsAllowed() == null
                && request.getDisplayText() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
