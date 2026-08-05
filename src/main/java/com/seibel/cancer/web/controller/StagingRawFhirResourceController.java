package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.StagingRawFhirResourceService;
import com.seibel.cancer.web.request.RequestStagingRawFhirResourceCreate;
import com.seibel.cancer.web.request.RequestStagingRawFhirResourceUpdate;
import com.seibel.cancer.web.response.ResponseStagingRawFhirResource;
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
@RequestMapping("/api/stagingrawfhirresource")
@Validated
@Tag(name = "StagingRawFhirResource", description = "StagingRawFhirResource CRUD endpoints")
@RequiredArgsConstructor
public class StagingRawFhirResourceController {

    private final StagingRawFhirResourceService stagingRawFhirResourceService;
    private final StagingRawFhirResourceConverter converter = new StagingRawFhirResourceConverter();

    @GetMapping
    @Operation(summary = "List stagingRawFhirResources (paginated)")
    public Page<ResponseStagingRawFhirResource> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "fetchedAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return stagingRawFhirResourceService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get stagingRawFhirResource by extid")
    public ResponseStagingRawFhirResource getByExtid(@PathVariable String extid) {
        return converter.toResponse(stagingRawFhirResourceService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create stagingRawFhirResource")
    public ResponseEntity<ResponseStagingRawFhirResource> create(@Valid @RequestBody RequestStagingRawFhirResourceCreate request) {
        StagingRawFhirResource created = stagingRawFhirResourceService.create(converter.toDomain(request));
        URI location = URI.create("/api/stagingrawfhirresource/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update stagingRawFhirResource (full or partial)")
    public ResponseStagingRawFhirResource update(@PathVariable String extid, @Valid @RequestBody RequestStagingRawFhirResourceUpdate request) {
        converter.validateUpdateRequest(request);
        StagingRawFhirResource updated = stagingRawFhirResourceService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch stagingRawFhirResource (partial update)")
    public ResponseStagingRawFhirResource patch(@PathVariable String extid, @Valid @RequestBody RequestStagingRawFhirResourceUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete stagingRawFhirResource (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = stagingRawFhirResourceService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class StagingRawFhirResourceConverter {

    StagingRawFhirResource toDomain(RequestStagingRawFhirResourceCreate request) {
        return StagingRawFhirResource.builder()
                .resourceType(request.getResourceType())
                .fhirResourceId(request.getFhirResourceId())
                .rawPayload(request.getRawPayload())
                .fetchedAt(request.getFetchedAt())
                .normalizedAt(request.getNormalizedAt())
                .normalizationError(request.getNormalizationError())
                .build();
    }

    StagingRawFhirResource toDomain(RequestStagingRawFhirResourceUpdate request) {
        return StagingRawFhirResource.builder()
                .resourceType(request.getResourceType())
                .fhirResourceId(request.getFhirResourceId())
                .rawPayload(request.getRawPayload())
                .fetchedAt(request.getFetchedAt())
                .normalizedAt(request.getNormalizedAt())
                .normalizationError(request.getNormalizationError())
                .build();
    }

    ResponseStagingRawFhirResource toResponse(StagingRawFhirResource item) {
        return ResponseStagingRawFhirResource.builder()
                .extid(item.getExtid())
                .resourceType(item.getResourceType())
                .fhirResourceId(item.getFhirResourceId())
                .rawPayload(item.getRawPayload())
                .fetchedAt(item.getFetchedAt())
                .normalizedAt(item.getNormalizedAt())
                .normalizationError(item.getNormalizationError())
                .build();
    }

    List<ResponseStagingRawFhirResource> toResponse(List<StagingRawFhirResource> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestStagingRawFhirResourceUpdate request) {
        if (request.getResourceType() == null &&
                request.getFhirResourceId() == null &&
                request.getRawPayload() == null &&
                request.getFetchedAt() == null &&
                request.getNormalizedAt() == null &&
                request.getNormalizationError() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
