package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.EligibilityRuleService;
import com.seibel.cancer.web.request.RequestEligibilityRuleCreate;
import com.seibel.cancer.web.request.RequestEligibilityRuleUpdate;
import com.seibel.cancer.web.response.ResponseEligibilityRule;
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
@RequestMapping("/api/eligibilityrule")
@Validated
@Tag(name = "EligibilityRule", description = "EligibilityRule CRUD endpoints")
@RequiredArgsConstructor
public class EligibilityRuleController {

    private final EligibilityRuleService eligibilityRuleService;
    private final EligibilityRuleConverter converter = new EligibilityRuleConverter();

    @GetMapping
    @Operation(summary = "List eligibilityRules (paginated)")
    public Page<ResponseEligibilityRule> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "sortOrder") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return eligibilityRuleService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get eligibilityRule by extid")
    public ResponseEligibilityRule getByExtid(@PathVariable String extid) {
        return converter.toResponse(eligibilityRuleService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create eligibilityRule")
    public ResponseEntity<ResponseEligibilityRule> create(@Valid @RequestBody RequestEligibilityRuleCreate request) {
        EligibilityRule created = eligibilityRuleService.create(converter.toDomain(request));
        URI location = URI.create("/api/eligibilityrule/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update eligibilityRule (full or partial)")
    public ResponseEligibilityRule update(@PathVariable String extid, @Valid @RequestBody RequestEligibilityRuleUpdate request) {
        converter.validateUpdateRequest(request);
        EligibilityRule updated = eligibilityRuleService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch eligibilityRule (partial update)")
    public ResponseEligibilityRule patch(@PathVariable String extid, @Valid @RequestBody RequestEligibilityRuleUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete eligibilityRule (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = eligibilityRuleService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class EligibilityRuleConverter {

    EligibilityRule toDomain(RequestEligibilityRuleCreate request) {
        return EligibilityRule.builder()
                .trialId(request.getTrialId())
                .parentRuleId(request.getParentRuleId())
                .nodeType(request.getNodeType())
                .operator(request.getOperator())
                .criterionType(request.getCriterionType())
                .criterionId(request.getCriterionId())
                .requirementType(request.getRequirementType())
                .sortOrder(request.getSortOrder())
                .notes(request.getNotes())
                .build();
    }

    EligibilityRule toDomain(RequestEligibilityRuleUpdate request) {
        return EligibilityRule.builder()
                .trialId(request.getTrialId())
                .parentRuleId(request.getParentRuleId())
                .nodeType(request.getNodeType())
                .operator(request.getOperator())
                .criterionType(request.getCriterionType())
                .criterionId(request.getCriterionId())
                .requirementType(request.getRequirementType())
                .sortOrder(request.getSortOrder())
                .notes(request.getNotes())
                .build();
    }

    ResponseEligibilityRule toResponse(EligibilityRule item) {
        return ResponseEligibilityRule.builder()
                .extid(item.getExtid())
                .trialId(item.getTrialId())
                .parentRuleId(item.getParentRuleId())
                .nodeType(item.getNodeType())
                .operator(item.getOperator())
                .criterionType(item.getCriterionType())
                .criterionId(item.getCriterionId())
                .requirementType(item.getRequirementType())
                .sortOrder(item.getSortOrder())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseEligibilityRule> toResponse(List<EligibilityRule> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestEligibilityRuleUpdate request) {
        if (request.getTrialId() == null &&
                request.getParentRuleId() == null &&
                request.getNodeType() == null &&
                request.getOperator() == null &&
                request.getCriterionType() == null &&
                request.getCriterionId() == null &&
                request.getRequirementType() == null &&
                request.getSortOrder() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
