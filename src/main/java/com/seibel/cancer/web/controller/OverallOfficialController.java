package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.OverallOfficialService;
import com.seibel.cancer.web.request.RequestOverallOfficialCreate;
import com.seibel.cancer.web.request.RequestOverallOfficialUpdate;
import com.seibel.cancer.web.response.ResponseOverallOfficial;
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
@RequestMapping("/api/overallofficial")
@Validated
@Tag(name = "OverallOfficial", description = "OverallOfficial CRUD endpoints")
@RequiredArgsConstructor
public class OverallOfficialController {

    private final OverallOfficialService overallOfficialService;
    private final OverallOfficialConverter converter = new OverallOfficialConverter();

    @GetMapping
    @Operation(summary = "List overallOfficials (paginated)")
    public Page<ResponseOverallOfficial> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return overallOfficialService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get overallOfficial by extid")
    public ResponseOverallOfficial getByExtid(@PathVariable String extid) {
        return converter.toResponse(overallOfficialService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create overallOfficial")
    public ResponseEntity<ResponseOverallOfficial> create(@Valid @RequestBody RequestOverallOfficialCreate request) {
        OverallOfficial created = overallOfficialService.create(converter.toDomain(request));
        URI location = URI.create("/api/overallofficial/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update overallOfficial (full or partial)")
    public ResponseOverallOfficial update(@PathVariable String extid, @Valid @RequestBody RequestOverallOfficialUpdate request) {
        converter.validateUpdateRequest(request);
        OverallOfficial updated = overallOfficialService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch overallOfficial (partial update)")
    public ResponseOverallOfficial patch(@PathVariable String extid, @Valid @RequestBody RequestOverallOfficialUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete overallOfficial (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = overallOfficialService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class OverallOfficialConverter {

    OverallOfficial toDomain(RequestOverallOfficialCreate request) {
        return OverallOfficial.builder()
                .trialId(request.getTrialId())
                .name(request.getName())
                .affiliation(request.getAffiliation())
                .role(request.getRole())
                .build();
    }

    OverallOfficial toDomain(RequestOverallOfficialUpdate request) {
        return OverallOfficial.builder()
                .trialId(request.getTrialId())
                .name(request.getName())
                .affiliation(request.getAffiliation())
                .role(request.getRole())
                .build();
    }

    ResponseOverallOfficial toResponse(OverallOfficial item) {
        return ResponseOverallOfficial.builder()
                .extid(item.getExtid())
                .trialId(item.getTrialId())
                .name(item.getName())
                .affiliation(item.getAffiliation())
                .role(item.getRole())
                .build();
    }

    List<ResponseOverallOfficial> toResponse(List<OverallOfficial> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestOverallOfficialUpdate request) {
        if (request.getTrialId() == null &&
                request.getName() == null &&
                request.getAffiliation() == null &&
                request.getRole() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
