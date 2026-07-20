package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.Friend;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.FriendService;
import com.seibel.jobhunting.web.request.RequestFriendCreate;
import com.seibel.jobhunting.web.request.RequestFriendUpdate;
import com.seibel.jobhunting.web.response.ResponseFriend;
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
@RequestMapping("/api/friend")
@Validated
@Tag(name = "Friend", description = "Friend CRUD endpoints")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final FriendConverter converter = new FriendConverter();

    @GetMapping
    @Operation(summary = "List friends (paginated)")
    public Page<ResponseFriend> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return friendService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get friend by extid")
    public ResponseFriend getByExtid(@PathVariable String extid) {
        return converter.toResponse(friendService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create friend")
    public ResponseEntity<ResponseFriend> create(@Valid @RequestBody RequestFriendCreate request) {
        Friend created = friendService.create(converter.toDomain(request));
        URI location = URI.create("/api/friend/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update friend (full or partial)")
    public ResponseFriend update(@PathVariable String extid, @Valid @RequestBody RequestFriendUpdate request) {
        converter.validateUpdateRequest(request);
        Friend updated = friendService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch friend (partial update)")
    public ResponseFriend patch(@PathVariable String extid, @Valid @RequestBody RequestFriendUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete friend (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = friendService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class FriendConverter {

    Friend toDomain(RequestFriendCreate request) {
        return Friend.builder()
                .name(request.getName())
                .relationship(request.getRelationship())
                .email(request.getEmail())
                .phone(request.getPhone())
                .linkedinUrl(request.getLinkedinUrl())
                .lastContactedAt(request.getLastContactedAt())
                .notes(request.getNotes())
                .build();
    }

    Friend toDomain(RequestFriendUpdate request) {
        return Friend.builder()
                .name(request.getName())
                .relationship(request.getRelationship())
                .email(request.getEmail())
                .phone(request.getPhone())
                .linkedinUrl(request.getLinkedinUrl())
                .lastContactedAt(request.getLastContactedAt())
                .notes(request.getNotes())
                .build();
    }

    ResponseFriend toResponse(Friend item) {
        return ResponseFriend.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .relationship(item.getRelationship())
                .email(item.getEmail())
                .phone(item.getPhone())
                .linkedinUrl(item.getLinkedinUrl())
                .lastContactedAt(item.getLastContactedAt())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseFriend> toResponse(List<Friend> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestFriendUpdate request) {
        if (request.getName() == null &&
                request.getRelationship() == null &&
                request.getEmail() == null &&
                request.getPhone() == null &&
                request.getLinkedinUrl() == null &&
                request.getLastContactedAt() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
