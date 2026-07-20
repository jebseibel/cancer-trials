package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.Skill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.SkillService;
import com.seibel.jobhunting.web.request.RequestSkillCreate;
import com.seibel.jobhunting.web.request.RequestSkillUpdate;
import com.seibel.jobhunting.web.response.ResponseSkill;
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
@RequestMapping("/api/skill")
@Validated
@Tag(name = "Skill", description = "Skill CRUD endpoints")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final SkillConverter converter = new SkillConverter();

    @GetMapping
    @Operation(summary = "List skills (paginated)")
    public Page<ResponseSkill> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return skillService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get skill by extid")
    public ResponseSkill getByExtid(@PathVariable String extid) {
        return converter.toResponse(skillService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create skill")
    public ResponseEntity<ResponseSkill> create(@Valid @RequestBody RequestSkillCreate request) {
        Skill created = skillService.create(converter.toDomain(request));
        URI location = URI.create("/api/skill/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update skill (full or partial)")
    public ResponseSkill update(@PathVariable String extid, @Valid @RequestBody RequestSkillUpdate request) {
        converter.validateUpdateRequest(request);
        Skill updated = skillService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch skill (partial update)")
    public ResponseSkill patch(@PathVariable String extid, @Valid @RequestBody RequestSkillUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete skill (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = skillService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class SkillConverter {

    Skill toDomain(RequestSkillCreate request) {
        return Skill.builder()
                .name(request.getName())
                .build();
    }

    Skill toDomain(RequestSkillUpdate request) {
        return Skill.builder()
                .name(request.getName())
                .build();
    }

    ResponseSkill toResponse(Skill item) {
        return ResponseSkill.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .build();
    }

    List<ResponseSkill> toResponse(List<Skill> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestSkillUpdate request) {
        if (request.getName() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
