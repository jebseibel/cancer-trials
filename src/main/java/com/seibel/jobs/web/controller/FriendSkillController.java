package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.FriendSkill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.FriendSkillService;
import com.seibel.jobs.web.request.RequestFriendSkillCreate;
import com.seibel.jobs.web.request.RequestFriendSkillUpdate;
import com.seibel.jobs.web.response.ResponseFriendSkill;
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
@RequestMapping("/api/friend-skill")
@Validated
@Tag(name = "FriendSkill", description = "Friend <-> skill link CRUD endpoints")
@RequiredArgsConstructor
public class FriendSkillController {

    private final FriendSkillService friendSkillService;
    private final FriendSkillConverter converter = new FriendSkillConverter();

    @GetMapping
    @Operation(summary = "List friend skill links (paginated)")
    public Page<ResponseFriendSkill> getAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return friendSkillService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get friend skill link by extid")
    public ResponseFriendSkill getByExtid(@PathVariable String extid) {
        return converter.toResponse(friendSkillService.findByExtid(extid));
    }

    @GetMapping("/by-friend/{friendId}")
    @Operation(summary = "List skill links for a friend")
    public List<ResponseFriendSkill> getByFriendId(@PathVariable Long friendId) {
        return converter.toResponse(friendSkillService.findByFriendId(friendId));
    }

    @GetMapping("/by-skill/{skillId}")
    @Operation(summary = "List friend links for a skill")
    public List<ResponseFriendSkill> getBySkillId(@PathVariable Long skillId) {
        return converter.toResponse(friendSkillService.findBySkillId(skillId));
    }

    @PostMapping
    @Operation(summary = "Create friend skill link")
    public ResponseEntity<ResponseFriendSkill> create(@Valid @RequestBody RequestFriendSkillCreate request) {
        FriendSkill created = friendSkillService.create(converter.toDomain(request));
        URI location = URI.create("/api/friend-skill/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update friend skill link (full or partial)")
    public ResponseFriendSkill update(@PathVariable String extid, @Valid @RequestBody RequestFriendSkillUpdate request) {
        converter.validateUpdateRequest(request);
        FriendSkill updated = friendSkillService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch friend skill link (partial update)")
    public ResponseFriendSkill patch(@PathVariable String extid, @Valid @RequestBody RequestFriendSkillUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete friend skill link (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = friendSkillService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class FriendSkillConverter {

    FriendSkill toDomain(RequestFriendSkillCreate request) {
        return FriendSkill.builder()
                .friendId(request.getFriendId())
                .skillId(request.getSkillId())
                .build();
    }

    FriendSkill toDomain(RequestFriendSkillUpdate request) {
        return FriendSkill.builder()
                .friendId(request.getFriendId())
                .skillId(request.getSkillId())
                .build();
    }

    ResponseFriendSkill toResponse(FriendSkill item) {
        return ResponseFriendSkill.builder()
                .extid(item.getExtid())
                .friendId(item.getFriendId())
                .skillId(item.getSkillId())
                .build();
    }

    List<ResponseFriendSkill> toResponse(List<FriendSkill> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestFriendSkillUpdate request) {
        if (request.getFriendId() == null && request.getSkillId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
