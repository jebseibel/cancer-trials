package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.StagingRawTrialService;
import com.seibel.cancer.web.request.RequestStagingRawTrialCreate;
import com.seibel.cancer.web.request.RequestStagingRawTrialUpdate;
import com.seibel.cancer.web.response.ResponseStagingRawTrial;
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
@RequestMapping("/api/stagingrawtrial")
@Validated
@Tag(name = "StagingRawTrial", description = "StagingRawTrial CRUD endpoints")
@RequiredArgsConstructor
public class StagingRawTrialController {

    private final StagingRawTrialService stagingRawTrialService;
    private final StagingRawTrialConverter converter = new StagingRawTrialConverter();

    @GetMapping
    @Operation(summary = "List stagingRawTrials (paginated)")
    public Page<ResponseStagingRawTrial> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "fetchedAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return stagingRawTrialService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get stagingRawTrial by extid")
    public ResponseStagingRawTrial getByExtid(@PathVariable String extid) {
        return converter.toResponse(stagingRawTrialService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create stagingRawTrial")
    public ResponseEntity<ResponseStagingRawTrial> create(@Valid @RequestBody RequestStagingRawTrialCreate request) {
        StagingRawTrial created = stagingRawTrialService.create(converter.toDomain(request));
        URI location = URI.create("/api/stagingrawtrial/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update stagingRawTrial (full or partial)")
    public ResponseStagingRawTrial update(@PathVariable String extid, @Valid @RequestBody RequestStagingRawTrialUpdate request) {
        converter.validateUpdateRequest(request);
        StagingRawTrial updated = stagingRawTrialService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch stagingRawTrial (partial update)")
    public ResponseStagingRawTrial patch(@PathVariable String extid, @Valid @RequestBody RequestStagingRawTrialUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete stagingRawTrial (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = stagingRawTrialService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class StagingRawTrialConverter {

    StagingRawTrial toDomain(RequestStagingRawTrialCreate request) {
        return StagingRawTrial.builder()
                .trialSourceId(request.getTrialSourceId())
                .sourceTrialId(request.getSourceTrialId())
                .rawPayload(request.getRawPayload())
                .fetchedAt(request.getFetchedAt())
                .normalizedAt(request.getNormalizedAt())
                .normalizationError(request.getNormalizationError())
                .build();
    }

    StagingRawTrial toDomain(RequestStagingRawTrialUpdate request) {
        return StagingRawTrial.builder()
                .trialSourceId(request.getTrialSourceId())
                .sourceTrialId(request.getSourceTrialId())
                .rawPayload(request.getRawPayload())
                .fetchedAt(request.getFetchedAt())
                .normalizedAt(request.getNormalizedAt())
                .normalizationError(request.getNormalizationError())
                .build();
    }

    ResponseStagingRawTrial toResponse(StagingRawTrial item) {
        return ResponseStagingRawTrial.builder()
                .extid(item.getExtid())
                .trialSourceId(item.getTrialSourceId())
                .sourceTrialId(item.getSourceTrialId())
                .rawPayload(item.getRawPayload())
                .fetchedAt(item.getFetchedAt())
                .normalizedAt(item.getNormalizedAt())
                .normalizationError(item.getNormalizationError())
                .build();
    }

    List<ResponseStagingRawTrial> toResponse(List<StagingRawTrial> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestStagingRawTrialUpdate request) {
        if (request.getTrialSourceId() == null &&
                request.getSourceTrialId() == null &&
                request.getRawPayload() == null &&
                request.getFetchedAt() == null &&
                request.getNormalizedAt() == null &&
                request.getNormalizationError() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
