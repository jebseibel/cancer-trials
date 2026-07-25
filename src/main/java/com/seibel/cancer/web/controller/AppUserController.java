package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.AppUserService;
import com.seibel.cancer.web.request.RequestAppUserCreate;
import com.seibel.cancer.web.request.RequestAppUserUpdate;
import com.seibel.cancer.web.response.ResponseAppUser;
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
@RequestMapping("/api/appuser")
@Validated
@Tag(name = "AppUser", description = "AppUser CRUD endpoints")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;
    private final AppUserConverter converter = new AppUserConverter();

    @GetMapping
    @Operation(summary = "List app users (paginated)")
    public Page<ResponseAppUser> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return appUserService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get app user by extid")
    public ResponseAppUser getByExtid(@PathVariable String extid) {
        return converter.toResponse(appUserService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create app user")
    public ResponseEntity<ResponseAppUser> create(@Valid @RequestBody RequestAppUserCreate request) {
        AppUser created = appUserService.create(converter.toDomain(request));
        URI location = URI.create("/api/appuser/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update app user (full or partial)")
    public ResponseAppUser update(@PathVariable String extid, @Valid @RequestBody RequestAppUserUpdate request) {
        converter.validateUpdateRequest(request);
        AppUser updated = appUserService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch app user (partial update)")
    public ResponseAppUser patch(@PathVariable String extid, @Valid @RequestBody RequestAppUserUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete app user (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = appUserService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class AppUserConverter {

    AppUser toDomain(RequestAppUserCreate request) {
        return AppUser.builder()
                .username(request.getUsername())
                .passwordHash(request.getPasswordHash())
                .displayName(request.getDisplayName())
                .build();
    }

    AppUser toDomain(RequestAppUserUpdate request) {
        return AppUser.builder()
                .username(request.getUsername())
                .passwordHash(request.getPasswordHash())
                .displayName(request.getDisplayName())
                .build();
    }

    ResponseAppUser toResponse(AppUser item) {
        return ResponseAppUser.builder()
                .extid(item.getExtid())
                .username(item.getUsername())
                .displayName(item.getDisplayName())
                .build();
    }

    List<ResponseAppUser> toResponse(List<AppUser> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestAppUserUpdate request) {
        if (request.getUsername() == null &&
                request.getPasswordHash() == null &&
                request.getDisplayName() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
