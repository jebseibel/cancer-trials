package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.User;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.UserService;
import com.seibel.jobs.web.request.RequestUserCreate;
import com.seibel.jobs.web.request.RequestUserUpdate;
import com.seibel.jobs.web.response.ResponseUser;
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
@RequestMapping("/api/user")
@Validated
@Tag(name = "User", description = "User CRUD endpoints")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserConverter converter = new UserConverter();

    @GetMapping
    @Operation(summary = "List users (paginated)")
    public Page<ResponseUser> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return userService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get user by extid")
    public ResponseUser getByExtid(@PathVariable String extid) {
        return converter.toResponse(userService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<ResponseUser> create(@Valid @RequestBody RequestUserCreate request) {
        User created = userService.create(converter.toDomain(request));
        URI location = URI.create("/api/user/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update user (full or partial)")
    public ResponseUser update(@PathVariable String extid, @Valid @RequestBody RequestUserUpdate request) {
        converter.validateUpdateRequest(request);
        User updated = userService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch user (partial update)")
    public ResponseUser patch(@PathVariable String extid, @Valid @RequestBody RequestUserUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete user (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = userService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class UserConverter {

    User toDomain(RequestUserCreate request) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .role(request.getRole())
                .build();
    }

    User toDomain(RequestUserUpdate request) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .role(request.getRole())
                .build();
    }

    ResponseUser toResponse(User item) {
        return ResponseUser.builder()
                .extid(item.getExtid())
                .username(item.getUsername())
                .email(item.getEmail())
                .role(item.getRole())
                .build();
    }

    List<ResponseUser> toResponse(List<User> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestUserUpdate request) {
        if (request.getUsername() == null &&
                request.getPassword() == null &&
                request.getEmail() == null &&
                request.getRole() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
