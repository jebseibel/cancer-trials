package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.ConditionService;
import com.seibel.cancer.web.request.RequestConditionCreate;
import com.seibel.cancer.web.request.RequestConditionUpdate;
import com.seibel.cancer.web.response.ResponseCondition;
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
@RequestMapping("/api/condition")
@Validated
@Tag(name = "Condition", description = "Condition CRUD endpoints")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionService conditionService;
    private final ConditionConverter converter = new ConditionConverter();

    @GetMapping
    @Operation(summary = "List conditions (paginated)")
    public Page<ResponseCondition> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return conditionService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get condition by extid")
    public ResponseCondition getByExtid(@PathVariable String extid) {
        return converter.toResponse(conditionService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create condition")
    public ResponseEntity<ResponseCondition> create(@Valid @RequestBody RequestConditionCreate request) {
        Condition created = conditionService.create(converter.toDomain(request));
        URI location = URI.create("/api/condition/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update condition (full or partial)")
    public ResponseCondition update(@PathVariable String extid, @Valid @RequestBody RequestConditionUpdate request) {
        converter.validateUpdateRequest(request);
        Condition updated = conditionService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch condition (partial update)")
    public ResponseCondition patch(@PathVariable String extid, @Valid @RequestBody RequestConditionUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete condition (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = conditionService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class ConditionConverter {

    Condition toDomain(RequestConditionCreate request) {
        return Condition.builder()
                .name(request.getName())
                .build();
    }

    Condition toDomain(RequestConditionUpdate request) {
        return Condition.builder()
                .name(request.getName())
                .build();
    }

    ResponseCondition toResponse(Condition item) {
        return ResponseCondition.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .build();
    }

    List<ResponseCondition> toResponse(List<Condition> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestConditionUpdate request) {
        if (request.getName() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
