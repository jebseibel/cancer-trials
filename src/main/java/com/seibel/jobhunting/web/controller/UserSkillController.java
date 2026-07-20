package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.UserSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.UserSkillService;
import com.seibel.jobhunting.web.request.RequestUserSkillCreate;
import com.seibel.jobhunting.web.request.RequestUserSkillUpdate;
import com.seibel.jobhunting.web.response.ResponseUserSkill;
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
@RequestMapping("/api/user-skill")
@Validated
@Tag(name = "UserSkill", description = "User <-> skill link CRUD endpoints")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final UserSkillConverter converter = new UserSkillConverter();

    @GetMapping
    @Operation(summary = "List user skill links (paginated)")
    public Page<ResponseUserSkill> getAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return userSkillService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get user skill link by extid")
    public ResponseUserSkill getByExtid(@PathVariable String extid) {
        return converter.toResponse(userSkillService.findByExtid(extid));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "List skill links for a user")
    public List<ResponseUserSkill> getByUserId(@PathVariable Long userId) {
        return converter.toResponse(userSkillService.findByUserId(userId));
    }

    @GetMapping("/by-skill/{skillId}")
    @Operation(summary = "List user links for a skill")
    public List<ResponseUserSkill> getBySkillId(@PathVariable Long skillId) {
        return converter.toResponse(userSkillService.findBySkillId(skillId));
    }

    @PostMapping
    @Operation(summary = "Create user skill link")
    public ResponseEntity<ResponseUserSkill> create(@Valid @RequestBody RequestUserSkillCreate request) {
        UserSkill created = userSkillService.create(converter.toDomain(request));
        URI location = URI.create("/api/user-skill/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update user skill link (full or partial)")
    public ResponseUserSkill update(@PathVariable String extid, @Valid @RequestBody RequestUserSkillUpdate request) {
        converter.validateUpdateRequest(request);
        UserSkill updated = userSkillService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch user skill link (partial update)")
    public ResponseUserSkill patch(@PathVariable String extid, @Valid @RequestBody RequestUserSkillUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete user skill link (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = userSkillService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class UserSkillConverter {

    UserSkill toDomain(RequestUserSkillCreate request) {
        return UserSkill.builder()
                .userId(request.getUserId())
                .skillId(request.getSkillId())
                .build();
    }

    UserSkill toDomain(RequestUserSkillUpdate request) {
        return UserSkill.builder()
                .userId(request.getUserId())
                .skillId(request.getSkillId())
                .build();
    }

    ResponseUserSkill toResponse(UserSkill item) {
        return ResponseUserSkill.builder()
                .extid(item.getExtid())
                .userId(item.getUserId())
                .skillId(item.getSkillId())
                .build();
    }

    List<ResponseUserSkill> toResponse(List<UserSkill> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestUserSkillUpdate request) {
        if (request.getUserId() == null && request.getSkillId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
