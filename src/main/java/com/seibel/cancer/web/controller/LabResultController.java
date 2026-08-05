package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.LabResultService;
import com.seibel.cancer.web.request.RequestLabResultCreate;
import com.seibel.cancer.web.request.RequestLabResultUpdate;
import com.seibel.cancer.web.response.ResponseLabResult;
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
@RequestMapping("/api/labresult")
@Validated
@Tag(name = "LabResult", description = "LabResult CRUD endpoints")
@RequiredArgsConstructor
public class LabResultController {

    private final LabResultService labResultService;
    private final LabResultConverter converter = new LabResultConverter();

    @GetMapping
    @Operation(summary = "List labResults (paginated)")
    public Page<ResponseLabResult> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "effectiveAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return labResultService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get labResult by extid")
    public ResponseLabResult getByExtid(@PathVariable String extid) {
        return converter.toResponse(labResultService.findByExtid(extid));
    }

    @GetMapping("/by-loinc/{loincCode}")
    @Operation(summary = "List all results for one LOINC code, newest first")
    public List<ResponseLabResult> getByLoincCode(@PathVariable String loincCode) {
        return converter.toResponse(labResultService.findByLoincCode(loincCode));
    }

    @PostMapping
    @Operation(summary = "Create labResult")
    public ResponseEntity<ResponseLabResult> create(@Valid @RequestBody RequestLabResultCreate request) {
        LabResult created = labResultService.create(converter.toDomain(request));
        URI location = URI.create("/api/labresult/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update labResult (full or partial)")
    public ResponseLabResult update(@PathVariable String extid, @Valid @RequestBody RequestLabResultUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(labResultService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update labResult")
    public ResponseLabResult patch(@PathVariable String extid, @Valid @RequestBody RequestLabResultUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete labResult")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return labResultService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class LabResultConverter {

    LabResult toDomain(RequestLabResultCreate request) {
        return LabResult.builder()
                .fhirResourceId(request.getFhirResourceId())
                .testName(request.getTestName())
                .loincCode(request.getLoincCode())
                .status(request.getStatus())
                .category(request.getCategory())
                .effectiveAt(request.getEffectiveAt())
                .issuedAt(request.getIssuedAt())
                .valueQuantity(request.getValueQuantity())
                .valueUnit(request.getValueUnit())
                .valueString(request.getValueString())
                .interpretation(request.getInterpretation())
                .referenceRangeLow(request.getReferenceRangeLow())
                .referenceRangeHigh(request.getReferenceRangeHigh())
                .referenceRangeText(request.getReferenceRangeText())
                .isPanel(request.getIsPanel())
                .displayText(request.getDisplayText())
                .build();
    }

    LabResult toDomain(RequestLabResultUpdate request) {
        return LabResult.builder()
                .fhirResourceId(request.getFhirResourceId())
                .testName(request.getTestName())
                .loincCode(request.getLoincCode())
                .status(request.getStatus())
                .category(request.getCategory())
                .effectiveAt(request.getEffectiveAt())
                .issuedAt(request.getIssuedAt())
                .valueQuantity(request.getValueQuantity())
                .valueUnit(request.getValueUnit())
                .valueString(request.getValueString())
                .interpretation(request.getInterpretation())
                .referenceRangeLow(request.getReferenceRangeLow())
                .referenceRangeHigh(request.getReferenceRangeHigh())
                .referenceRangeText(request.getReferenceRangeText())
                .isPanel(request.getIsPanel())
                .displayText(request.getDisplayText())
                .build();
    }

    ResponseLabResult toResponse(LabResult item) {
        return ResponseLabResult.builder()
                .extid(item.getExtid())
                .fhirResourceId(item.getFhirResourceId())
                .testName(item.getTestName())
                .loincCode(item.getLoincCode())
                .status(item.getStatus())
                .category(item.getCategory())
                .effectiveAt(item.getEffectiveAt())
                .issuedAt(item.getIssuedAt())
                .valueQuantity(item.getValueQuantity())
                .valueUnit(item.getValueUnit())
                .valueString(item.getValueString())
                .interpretation(item.getInterpretation())
                .referenceRangeLow(item.getReferenceRangeLow())
                .referenceRangeHigh(item.getReferenceRangeHigh())
                .referenceRangeText(item.getReferenceRangeText())
                .isPanel(item.getIsPanel())
                .displayText(item.getDisplayText())
                .build();
    }

    List<ResponseLabResult> toResponse(List<LabResult> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestLabResultUpdate request) {
        if (request.getFhirResourceId() == null
                && request.getTestName() == null
                && request.getLoincCode() == null
                && request.getStatus() == null
                && request.getCategory() == null
                && request.getEffectiveAt() == null
                && request.getIssuedAt() == null
                && request.getValueQuantity() == null
                && request.getValueUnit() == null
                && request.getValueString() == null
                && request.getInterpretation() == null
                && request.getReferenceRangeLow() == null
                && request.getReferenceRangeHigh() == null
                && request.getReferenceRangeText() == null
                && request.getIsPanel() == null
                && request.getDisplayText() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
