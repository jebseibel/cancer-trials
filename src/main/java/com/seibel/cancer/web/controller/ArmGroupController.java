package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.ArmGroupService;
import com.seibel.cancer.web.request.RequestArmGroupCreate;
import com.seibel.cancer.web.request.RequestArmGroupUpdate;
import com.seibel.cancer.web.response.ResponseArmGroup;
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
@RequestMapping("/api/armgroup")
@Validated
@Tag(name = "ArmGroup", description = "ArmGroup CRUD endpoints")
@RequiredArgsConstructor
public class ArmGroupController {

    private final ArmGroupService armGroupService;
    private final ArmGroupConverter converter = new ArmGroupConverter();

    @GetMapping
    @Operation(summary = "List armGroups (paginated)")
    public Page<ResponseArmGroup> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "label") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return armGroupService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get armGroup by extid")
    public ResponseArmGroup getByExtid(@PathVariable String extid) {
        return converter.toResponse(armGroupService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create armGroup")
    public ResponseEntity<ResponseArmGroup> create(@Valid @RequestBody RequestArmGroupCreate request) {
        ArmGroup created = armGroupService.create(converter.toDomain(request));
        URI location = URI.create("/api/armgroup/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update armGroup (full or partial)")
    public ResponseArmGroup update(@PathVariable String extid, @Valid @RequestBody RequestArmGroupUpdate request) {
        converter.validateUpdateRequest(request);
        ArmGroup updated = armGroupService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch armGroup (partial update)")
    public ResponseArmGroup patch(@PathVariable String extid, @Valid @RequestBody RequestArmGroupUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete armGroup (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = armGroupService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class ArmGroupConverter {

    ArmGroup toDomain(RequestArmGroupCreate request) {
        return ArmGroup.builder()
                .trialId(request.getTrialId())
                .label(request.getLabel())
                .type(request.getType())
                .description(request.getDescription())
                .build();
    }

    ArmGroup toDomain(RequestArmGroupUpdate request) {
        return ArmGroup.builder()
                .trialId(request.getTrialId())
                .label(request.getLabel())
                .type(request.getType())
                .description(request.getDescription())
                .build();
    }

    ResponseArmGroup toResponse(ArmGroup item) {
        return ResponseArmGroup.builder()
                .extid(item.getExtid())
                .trialId(item.getTrialId())
                .label(item.getLabel())
                .type(item.getType())
                .description(item.getDescription())
                .build();
    }

    List<ResponseArmGroup> toResponse(List<ArmGroup> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestArmGroupUpdate request) {
        if (request.getTrialId() == null &&
                request.getLabel() == null &&
                request.getType() == null &&
                request.getDescription() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
