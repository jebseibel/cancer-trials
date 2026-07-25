package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.TrialSourceService;
import com.seibel.cancer.web.request.RequestTrialSourceCreate;
import com.seibel.cancer.web.request.RequestTrialSourceUpdate;
import com.seibel.cancer.web.response.ResponseTrialSource;
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
@RequestMapping("/api/trialsource")
@Validated
@Tag(name = "TrialSource", description = "TrialSource CRUD endpoints")
@RequiredArgsConstructor
public class TrialSourceController {

    private final TrialSourceService trialSourceService;
    private final TrialSourceConverter converter = new TrialSourceConverter();

    @GetMapping
    @Operation(summary = "List trialSources (paginated)")
    public Page<ResponseTrialSource> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "code") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return trialSourceService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get trialSource by extid")
    public ResponseTrialSource getByExtid(@PathVariable String extid) {
        return converter.toResponse(trialSourceService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create trialSource")
    public ResponseEntity<ResponseTrialSource> create(@Valid @RequestBody RequestTrialSourceCreate request) {
        TrialSource created = trialSourceService.create(converter.toDomain(request));
        URI location = URI.create("/api/trialsource/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update trialSource (full or partial)")
    public ResponseTrialSource update(@PathVariable String extid, @Valid @RequestBody RequestTrialSourceUpdate request) {
        converter.validateUpdateRequest(request);
        TrialSource updated = trialSourceService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch trialSource (partial update)")
    public ResponseTrialSource patch(@PathVariable String extid, @Valid @RequestBody RequestTrialSourceUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete trialSource (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = trialSourceService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class TrialSourceConverter {

    TrialSource toDomain(RequestTrialSourceCreate request) {
        return TrialSource.builder()
                .code(request.getCode())
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .build();
    }

    TrialSource toDomain(RequestTrialSourceUpdate request) {
        return TrialSource.builder()
                .code(request.getCode())
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .build();
    }

    ResponseTrialSource toResponse(TrialSource item) {
        return ResponseTrialSource.builder()
                .extid(item.getExtid())
                .code(item.getCode())
                .name(item.getName())
                .baseUrl(item.getBaseUrl())
                .build();
    }

    List<ResponseTrialSource> toResponse(List<TrialSource> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestTrialSourceUpdate request) {
        if (request.getCode() == null &&
                request.getName() == null &&
                request.getBaseUrl() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
