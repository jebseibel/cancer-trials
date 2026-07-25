package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.MedicationService;
import com.seibel.cancer.web.request.RequestMedicationCreate;
import com.seibel.cancer.web.request.RequestMedicationUpdate;
import com.seibel.cancer.web.response.ResponseMedication;
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
@RequestMapping("/api/medication")
@Validated
@Tag(name = "Medication", description = "Medication CRUD endpoints")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;
    private final MedicationConverter converter = new MedicationConverter();

    @GetMapping
    @Operation(summary = "List medications (paginated)")
    public Page<ResponseMedication> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return medicationService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get medication by extid")
    public ResponseMedication getByExtid(@PathVariable String extid) {
        return converter.toResponse(medicationService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create medication")
    public ResponseEntity<ResponseMedication> create(@Valid @RequestBody RequestMedicationCreate request) {
        Medication created = medicationService.create(converter.toDomain(request));
        URI location = URI.create("/api/medication/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update medication (full or partial)")
    public ResponseMedication update(@PathVariable String extid, @Valid @RequestBody RequestMedicationUpdate request) {
        converter.validateUpdateRequest(request);
        Medication updated = medicationService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch medication (partial update)")
    public ResponseMedication patch(@PathVariable String extid, @Valid @RequestBody RequestMedicationUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete medication (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = medicationService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class MedicationConverter {

    Medication toDomain(RequestMedicationCreate request) {
        return Medication.builder()
                .name(request.getName())
                .build();
    }

    Medication toDomain(RequestMedicationUpdate request) {
        return Medication.builder()
                .name(request.getName())
                .build();
    }

    ResponseMedication toResponse(Medication item) {
        return ResponseMedication.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .build();
    }

    List<ResponseMedication> toResponse(List<Medication> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestMedicationUpdate request) {
        if (request.getName() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
