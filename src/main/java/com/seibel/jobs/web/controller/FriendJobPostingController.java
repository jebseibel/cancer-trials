package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.FriendJobPosting;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.FriendJobPostingService;
import com.seibel.jobs.web.request.RequestFriendJobPostingCreate;
import com.seibel.jobs.web.request.RequestFriendJobPostingUpdate;
import com.seibel.jobs.web.response.ResponseFriendJobPosting;
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
@RequestMapping("/api/friend-job-posting")
@Validated
@Tag(name = "FriendJobPosting", description = "Friend <-> job posting link CRUD endpoints")
@RequiredArgsConstructor
public class FriendJobPostingController {

    private final FriendJobPostingService friendJobPostingService;
    private final FriendJobPostingConverter converter = new FriendJobPostingConverter();

    @GetMapping
    @Operation(summary = "List friend job posting links (paginated)")
    public Page<ResponseFriendJobPosting> getAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return friendJobPostingService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get friend job posting link by extid")
    public ResponseFriendJobPosting getByExtid(@PathVariable String extid) {
        return converter.toResponse(friendJobPostingService.findByExtid(extid));
    }

    @GetMapping("/by-friend/{friendId}")
    @Operation(summary = "List job posting links for a friend")
    public List<ResponseFriendJobPosting> getByFriendId(@PathVariable Long friendId) {
        return converter.toResponse(friendJobPostingService.findByFriendId(friendId));
    }

    @GetMapping("/by-job-posting/{jobPostingId}")
    @Operation(summary = "List friend links for a job posting")
    public List<ResponseFriendJobPosting> getByJobPostingId(@PathVariable Long jobPostingId) {
        return converter.toResponse(friendJobPostingService.findByJobPostingId(jobPostingId));
    }

    @PostMapping
    @Operation(summary = "Create friend job posting link")
    public ResponseEntity<ResponseFriendJobPosting> create(@Valid @RequestBody RequestFriendJobPostingCreate request) {
        FriendJobPosting created = friendJobPostingService.create(converter.toDomain(request));
        URI location = URI.create("/api/friend-job-posting/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update friend job posting link (full or partial)")
    public ResponseFriendJobPosting update(@PathVariable String extid, @Valid @RequestBody RequestFriendJobPostingUpdate request) {
        converter.validateUpdateRequest(request);
        FriendJobPosting updated = friendJobPostingService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch friend job posting link (partial update)")
    public ResponseFriendJobPosting patch(@PathVariable String extid, @Valid @RequestBody RequestFriendJobPostingUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete friend job posting link (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = friendJobPostingService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class FriendJobPostingConverter {

    FriendJobPosting toDomain(RequestFriendJobPostingCreate request) {
        return FriendJobPosting.builder()
                .friendId(request.getFriendId())
                .jobPostingId(request.getJobPostingId())
                .build();
    }

    FriendJobPosting toDomain(RequestFriendJobPostingUpdate request) {
        return FriendJobPosting.builder()
                .friendId(request.getFriendId())
                .jobPostingId(request.getJobPostingId())
                .build();
    }

    ResponseFriendJobPosting toResponse(FriendJobPosting item) {
        return ResponseFriendJobPosting.builder()
                .extid(item.getExtid())
                .friendId(item.getFriendId())
                .jobPostingId(item.getJobPostingId())
                .build();
    }

    List<ResponseFriendJobPosting> toResponse(List<FriendJobPosting> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestFriendJobPostingUpdate request) {
        if (request.getFriendId() == null && request.getJobPostingId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
