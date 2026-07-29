package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.AppUserRepository;
import com.seibel.cancer.database.db.repository.TrialRepository;
import com.seibel.cancer.service.TrialStatusService;
import com.seibel.cancer.web.request.RequestTrialStatusCreate;
import com.seibel.cancer.web.request.RequestTrialStatusUpdate;
import com.seibel.cancer.web.response.ResponseTrialStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/trialstatus")
@Validated
@Tag(name = "TrialStatus", description = "TrialStatus CRUD endpoints")
@RequiredArgsConstructor
public class TrialStatusController {

    private final TrialStatusService trialStatusService;
    private final TrialStatusConverter converter;

    @GetMapping
    @Operation(summary = "List trial statuses (paginated)")
    public Page<ResponseTrialStatus> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "statusChangedAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return trialStatusService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/by-appuser/{appUserExtid}")
    @Operation(summary = "List all trial statuses for an app user (unpaginated)")
    public List<ResponseTrialStatus> getByAppUserExtid(@PathVariable String appUserExtid) {
        Long appUserId = converter.resolveAppUserId(appUserExtid);
        return converter.toResponse(trialStatusService.findByAppUserId(appUserId));
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get trial status by extid")
    public ResponseTrialStatus getByExtid(@PathVariable String extid) {
        return converter.toResponse(trialStatusService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create trial status")
    public ResponseEntity<ResponseTrialStatus> create(@Valid @RequestBody RequestTrialStatusCreate request) {
        TrialStatus created = trialStatusService.create(converter.toDomain(request));
        URI location = URI.create("/api/trialstatus/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update trial status (full or partial)")
    public ResponseTrialStatus update(@PathVariable String extid, @Valid @RequestBody RequestTrialStatusUpdate request) {
        converter.validateUpdateRequest(request);
        TrialStatus updated = trialStatusService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch trial status (partial update)")
    public ResponseTrialStatus patch(@PathVariable String extid, @Valid @RequestBody RequestTrialStatusUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete trial status (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = trialStatusService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

@Component
@RequiredArgsConstructor
class TrialStatusConverter {

    private final TrialRepository trialRepository;
    private final AppUserRepository appUserRepository;

    Long resolveTrialId(String trialExtid) {
        return trialRepository.findByExtid(trialExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Trial", trialExtid))
                .getId();
    }

    Long resolveAppUserId(String appUserExtid) {
        return appUserRepository.findByExtid(appUserExtid)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", appUserExtid))
                .getId();
    }

    private String resolveTrialExtid(Long trialId) {
        if (trialId == null) return null;
        return trialRepository.findById(trialId).map(t -> t.getExtid()).orElse(null);
    }

    private String resolveAppUserExtid(Long appUserId) {
        if (appUserId == null) return null;
        return appUserRepository.findById(appUserId).map(u -> u.getExtid()).orElse(null);
    }

    TrialStatus toDomain(RequestTrialStatusCreate request) {
        return TrialStatus.builder()
                .trialId(resolveTrialId(request.getTrialExtid()))
                .appUserId(resolveAppUserId(request.getAppUserExtid()))
                .status(request.getStatus())
                .notes(request.getNotes())
                .statusChangedAt(request.getStatusChangedAt())
                .build();
    }

    TrialStatus toDomain(RequestTrialStatusUpdate request) {
        return TrialStatus.builder()
                .trialId(request.getTrialExtid() != null ? resolveTrialId(request.getTrialExtid()) : null)
                .appUserId(request.getAppUserExtid() != null ? resolveAppUserId(request.getAppUserExtid()) : null)
                .status(request.getStatus())
                .notes(request.getNotes())
                .statusChangedAt(request.getStatusChangedAt())
                .build();
    }

    ResponseTrialStatus toResponse(TrialStatus item) {
        return ResponseTrialStatus.builder()
                .extid(item.getExtid())
                .trialExtid(resolveTrialExtid(item.getTrialId()))
                .appUserExtid(resolveAppUserExtid(item.getAppUserId()))
                .status(item.getStatus())
                .notes(item.getNotes())
                .statusChangedAt(item.getStatusChangedAt())
                .build();
    }

    List<ResponseTrialStatus> toResponse(List<TrialStatus> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestTrialStatusUpdate request) {
        if (request.getTrialExtid() == null &&
                request.getAppUserExtid() == null &&
                request.getStatus() == null &&
                request.getNotes() == null &&
                request.getStatusChangedAt() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
