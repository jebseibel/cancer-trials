package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.UcHealthOAuthTokenService;
import com.seibel.cancer.web.request.RequestUcHealthOAuthTokenCreate;
import com.seibel.cancer.web.request.RequestUcHealthOAuthTokenUpdate;
import com.seibel.cancer.web.response.ResponseUcHealthOAuthToken;
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
@RequestMapping("/api/uchealthoauthtoken")
@Validated
@Tag(name = "UcHealthOAuthToken", description = "UcHealthOAuthToken CRUD endpoints")
@RequiredArgsConstructor
public class UcHealthOAuthTokenController {

    private final UcHealthOAuthTokenService ucHealthOAuthTokenService;
    private final UcHealthOAuthTokenConverter converter = new UcHealthOAuthTokenConverter();

    @GetMapping
    @Operation(summary = "List ucHealthOAuthTokens (paginated)")
    public Page<ResponseUcHealthOAuthToken> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return ucHealthOAuthTokenService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get ucHealthOAuthToken by extid")
    public ResponseUcHealthOAuthToken getByExtid(@PathVariable String extid) {
        return converter.toResponse(ucHealthOAuthTokenService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create ucHealthOAuthToken")
    public ResponseEntity<ResponseUcHealthOAuthToken> create(@Valid @RequestBody RequestUcHealthOAuthTokenCreate request) {
        UcHealthOAuthToken created = ucHealthOAuthTokenService.create(converter.toDomain(request));
        URI location = URI.create("/api/uchealthoauthtoken/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update ucHealthOAuthToken (full or partial)")
    public ResponseUcHealthOAuthToken update(@PathVariable String extid, @Valid @RequestBody RequestUcHealthOAuthTokenUpdate request) {
        converter.validateUpdateRequest(request);
        UcHealthOAuthToken updated = ucHealthOAuthTokenService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch ucHealthOAuthToken (partial update)")
    public ResponseUcHealthOAuthToken patch(@PathVariable String extid, @Valid @RequestBody RequestUcHealthOAuthTokenUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete ucHealthOAuthToken (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = ucHealthOAuthTokenService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class UcHealthOAuthTokenConverter {

    UcHealthOAuthToken toDomain(RequestUcHealthOAuthTokenCreate request) {
        return UcHealthOAuthToken.builder()
                .accessToken(request.getAccessToken())
                .refreshToken(request.getRefreshToken())
                .expiresAt(request.getExpiresAt())
                .patientFhirId(request.getPatientFhirId())
                .scope(request.getScope())
                .build();
    }

    UcHealthOAuthToken toDomain(RequestUcHealthOAuthTokenUpdate request) {
        return UcHealthOAuthToken.builder()
                .accessToken(request.getAccessToken())
                .refreshToken(request.getRefreshToken())
                .expiresAt(request.getExpiresAt())
                .patientFhirId(request.getPatientFhirId())
                .scope(request.getScope())
                .build();
    }

    ResponseUcHealthOAuthToken toResponse(UcHealthOAuthToken item) {
        return ResponseUcHealthOAuthToken.builder()
                .extid(item.getExtid())
                .accessToken(item.getAccessToken())
                .refreshToken(item.getRefreshToken())
                .expiresAt(item.getExpiresAt())
                .patientFhirId(item.getPatientFhirId())
                .scope(item.getScope())
                .build();
    }

    List<ResponseUcHealthOAuthToken> toResponse(List<UcHealthOAuthToken> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestUcHealthOAuthTokenUpdate request) {
        if (request.getAccessToken() == null &&
                request.getRefreshToken() == null &&
                request.getExpiresAt() == null &&
                request.getPatientFhirId() == null &&
                request.getScope() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
