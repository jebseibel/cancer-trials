package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.LabResultRepository;
import com.seibel.cancer.service.LabResultComponentService;
import com.seibel.cancer.web.request.RequestLabResultComponentCreate;
import com.seibel.cancer.web.request.RequestLabResultComponentUpdate;
import com.seibel.cancer.web.response.ResponseLabResultComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/labresultcomponent")
@Validated
@Tag(name = "LabResultComponent", description = "LabResultComponent CRUD endpoints")
public class LabResultComponentController {

    private final LabResultComponentService labResultComponentService;
    private final LabResultComponentConverter converter;

    public LabResultComponentController(LabResultComponentService labResultComponentService,
                                        LabResultRepository labResultRepository) {
        this.labResultComponentService = labResultComponentService;
        this.converter = new LabResultComponentConverter(labResultRepository);
    }

    @GetMapping
    @Operation(summary = "List labResultComponents (paginated)")
    public Page<ResponseLabResultComponent> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "componentName") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return labResultComponentService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/by-labresult/{labResultExtid}")
    @Operation(summary = "List all components for one lab result (unpaginated)")
    public List<ResponseLabResultComponent> getByLabResultExtid(@PathVariable String labResultExtid) {
        Long labResultId = converter.resolveLabResultId(labResultExtid);
        return converter.toResponse(labResultComponentService.findByLabResultId(labResultId));
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get labResultComponent by extid")
    public ResponseLabResultComponent getByExtid(@PathVariable String extid) {
        return converter.toResponse(labResultComponentService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create labResultComponent")
    public ResponseEntity<ResponseLabResultComponent> create(@Valid @RequestBody RequestLabResultComponentCreate request) {
        LabResultComponent created = labResultComponentService.create(converter.toDomain(request));
        URI location = URI.create("/api/labresultcomponent/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update labResultComponent (full or partial)")
    public ResponseLabResultComponent update(@PathVariable String extid, @Valid @RequestBody RequestLabResultComponentUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(labResultComponentService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update labResultComponent")
    public ResponseLabResultComponent patch(@PathVariable String extid, @Valid @RequestBody RequestLabResultComponentUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete labResultComponent")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return labResultComponentService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class LabResultComponentConverter {

    private final LabResultRepository labResultRepository;

    LabResultComponentConverter(LabResultRepository labResultRepository) {
        this.labResultRepository = labResultRepository;
    }

    Long resolveLabResultId(String labResultExtid) {
        return labResultRepository.findByExtid(labResultExtid)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", labResultExtid))
                .getId();
    }

    private String resolveLabResultExtid(Long labResultId) {
        if (labResultId == null) return null;
        return labResultRepository.findById(labResultId)
                .map(l -> l.getExtid())
                .orElse(null);
    }

    LabResultComponent toDomain(RequestLabResultComponentCreate request) {
        return LabResultComponent.builder()
                .labResultId(resolveLabResultId(request.getLabResultExtid()))
                .componentName(request.getComponentName())
                .loincCode(request.getLoincCode())
                .valueQuantity(request.getValueQuantity())
                .valueUnit(request.getValueUnit())
                .valueString(request.getValueString())
                .interpretation(request.getInterpretation())
                .referenceRangeLow(request.getReferenceRangeLow())
                .referenceRangeHigh(request.getReferenceRangeHigh())
                .referenceRangeText(request.getReferenceRangeText())
                .displayText(request.getDisplayText())
                .build();
    }

    LabResultComponent toDomain(RequestLabResultComponentUpdate request) {
        return LabResultComponent.builder()
                .labResultId(request.getLabResultExtid() != null ? resolveLabResultId(request.getLabResultExtid()) : null)
                .componentName(request.getComponentName())
                .loincCode(request.getLoincCode())
                .valueQuantity(request.getValueQuantity())
                .valueUnit(request.getValueUnit())
                .valueString(request.getValueString())
                .interpretation(request.getInterpretation())
                .referenceRangeLow(request.getReferenceRangeLow())
                .referenceRangeHigh(request.getReferenceRangeHigh())
                .referenceRangeText(request.getReferenceRangeText())
                .displayText(request.getDisplayText())
                .build();
    }

    ResponseLabResultComponent toResponse(LabResultComponent item) {
        return ResponseLabResultComponent.builder()
                .extid(item.getExtid())
                .labResultExtid(resolveLabResultExtid(item.getLabResultId()))
                .componentName(item.getComponentName())
                .loincCode(item.getLoincCode())
                .valueQuantity(item.getValueQuantity())
                .valueUnit(item.getValueUnit())
                .valueString(item.getValueString())
                .interpretation(item.getInterpretation())
                .referenceRangeLow(item.getReferenceRangeLow())
                .referenceRangeHigh(item.getReferenceRangeHigh())
                .referenceRangeText(item.getReferenceRangeText())
                .displayText(item.getDisplayText())
                .build();
    }

    List<ResponseLabResultComponent> toResponse(List<LabResultComponent> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestLabResultComponentUpdate request) {
        if (request.getLabResultExtid() == null
                && request.getComponentName() == null
                && request.getLoincCode() == null
                && request.getValueQuantity() == null
                && request.getValueUnit() == null
                && request.getValueString() == null
                && request.getInterpretation() == null
                && request.getReferenceRangeLow() == null
                && request.getReferenceRangeHigh() == null
                && request.getReferenceRangeText() == null
                && request.getDisplayText() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
