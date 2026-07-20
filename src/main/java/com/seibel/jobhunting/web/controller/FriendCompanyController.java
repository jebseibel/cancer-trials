package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.FriendCompany;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.FriendCompanyService;
import com.seibel.jobhunting.web.request.RequestFriendCompanyCreate;
import com.seibel.jobhunting.web.request.RequestFriendCompanyUpdate;
import com.seibel.jobhunting.web.response.ResponseFriendCompany;
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
@RequestMapping("/api/friend-company")
@Validated
@Tag(name = "FriendCompany", description = "Friend <-> company link CRUD endpoints")
@RequiredArgsConstructor
public class FriendCompanyController {

    private final FriendCompanyService friendCompanyService;
    private final FriendCompanyConverter converter = new FriendCompanyConverter();

    @GetMapping
    @Operation(summary = "List friend company links (paginated)")
    public Page<ResponseFriendCompany> getAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return friendCompanyService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get friend company link by extid")
    public ResponseFriendCompany getByExtid(@PathVariable String extid) {
        return converter.toResponse(friendCompanyService.findByExtid(extid));
    }

    @GetMapping("/by-friend/{friendId}")
    @Operation(summary = "List company links for a friend")
    public List<ResponseFriendCompany> getByFriendId(@PathVariable Long friendId) {
        return converter.toResponse(friendCompanyService.findByFriendId(friendId));
    }

    @GetMapping("/by-company/{companyId}")
    @Operation(summary = "List friend links for a company")
    public List<ResponseFriendCompany> getByCompanyId(@PathVariable Long companyId) {
        return converter.toResponse(friendCompanyService.findByCompanyId(companyId));
    }

    @PostMapping
    @Operation(summary = "Create friend company link")
    public ResponseEntity<ResponseFriendCompany> create(@Valid @RequestBody RequestFriendCompanyCreate request) {
        FriendCompany created = friendCompanyService.create(converter.toDomain(request));
        URI location = URI.create("/api/friend-company/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update friend company link (full or partial)")
    public ResponseFriendCompany update(@PathVariable String extid, @Valid @RequestBody RequestFriendCompanyUpdate request) {
        converter.validateUpdateRequest(request);
        FriendCompany updated = friendCompanyService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch friend company link (partial update)")
    public ResponseFriendCompany patch(@PathVariable String extid, @Valid @RequestBody RequestFriendCompanyUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete friend company link (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = friendCompanyService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class FriendCompanyConverter {

    FriendCompany toDomain(RequestFriendCompanyCreate request) {
        return FriendCompany.builder()
                .friendId(request.getFriendId())
                .companyId(request.getCompanyId())
                .build();
    }

    FriendCompany toDomain(RequestFriendCompanyUpdate request) {
        return FriendCompany.builder()
                .friendId(request.getFriendId())
                .companyId(request.getCompanyId())
                .build();
    }

    ResponseFriendCompany toResponse(FriendCompany item) {
        return ResponseFriendCompany.builder()
                .extid(item.getExtid())
                .friendId(item.getFriendId())
                .companyId(item.getCompanyId())
                .build();
    }

    List<ResponseFriendCompany> toResponse(List<FriendCompany> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestFriendCompanyUpdate request) {
        if (request.getFriendId() == null && request.getCompanyId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
