package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.InterventionService;
import com.seibel.cancer.web.request.RequestInterventionCreate;
import com.seibel.cancer.web.request.RequestInterventionUpdate;
import com.seibel.cancer.web.response.ResponseIntervention;
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
@RequestMapping("/api/intervention")
@Validated
@Tag(name = "Intervention", description = "Intervention CRUD endpoints")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;
    private final InterventionConverter converter = new InterventionConverter();

    @GetMapping
    @Operation(summary = "List interventions (paginated)")
    public Page<ResponseIntervention> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return interventionService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get intervention by extid")
    public ResponseIntervention getByExtid(@PathVariable String extid) {
        return converter.toResponse(interventionService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create intervention")
    public ResponseEntity<ResponseIntervention> create(@Valid @RequestBody RequestInterventionCreate request) {
        Intervention created = interventionService.create(converter.toDomain(request));
        URI location = URI.create("/api/intervention/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update intervention (full or partial)")
    public ResponseIntervention update(@PathVariable String extid, @Valid @RequestBody RequestInterventionUpdate request) {
        converter.validateUpdateRequest(request);
        Intervention updated = interventionService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch intervention (partial update)")
    public ResponseIntervention patch(@PathVariable String extid, @Valid @RequestBody RequestInterventionUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete intervention (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = interventionService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class InterventionConverter {

    Intervention toDomain(RequestInterventionCreate request) {
        return Intervention.builder()
                .trialId(request.getTrialId())
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    Intervention toDomain(RequestInterventionUpdate request) {
        return Intervention.builder()
                .trialId(request.getTrialId())
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    ResponseIntervention toResponse(Intervention item) {
        return ResponseIntervention.builder()
                .extid(item.getExtid())
                .trialId(item.getTrialId())
                .type(item.getType())
                .name(item.getName())
                .description(item.getDescription())
                .build();
    }

    List<ResponseIntervention> toResponse(List<Intervention> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestInterventionUpdate request) {
        if (request.getTrialId() == null &&
                request.getType() == null &&
                request.getName() == null &&
                request.getDescription() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
