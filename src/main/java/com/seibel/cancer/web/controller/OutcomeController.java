package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.TrialRepository;
import com.seibel.cancer.service.OutcomeService;
import com.seibel.cancer.web.request.RequestOutcomeCreate;
import com.seibel.cancer.web.request.RequestOutcomeUpdate;
import com.seibel.cancer.web.response.ResponseOutcome;
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
@RequestMapping("/api/outcome")
@Validated
@Tag(name = "Outcome", description = "Outcome CRUD endpoints")
@RequiredArgsConstructor
public class OutcomeController {

    private final OutcomeService outcomeService;
    private final OutcomeConverter converter;

    @GetMapping
    @Operation(summary = "List outcomes (paginated)")
    public Page<ResponseOutcome> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "measure") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return outcomeService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/by-trial/{trialExtid}")
    @Operation(summary = "List all outcomes for a trial (unpaginated)")
    public List<ResponseOutcome> getByTrialExtid(@PathVariable String trialExtid) {
        Long trialId = converter.resolveTrialId(trialExtid);
        return converter.toResponse(outcomeService.findByTrialId(trialId));
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get outcome by extid")
    public ResponseOutcome getByExtid(@PathVariable String extid) {
        return converter.toResponse(outcomeService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create outcome")
    public ResponseEntity<ResponseOutcome> create(@Valid @RequestBody RequestOutcomeCreate request) {
        Outcome created = outcomeService.create(converter.toDomain(request));
        URI location = URI.create("/api/outcome/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update outcome (full or partial)")
    public ResponseOutcome update(@PathVariable String extid, @Valid @RequestBody RequestOutcomeUpdate request) {
        converter.validateUpdateRequest(request);
        Outcome updated = outcomeService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch outcome (partial update)")
    public ResponseOutcome patch(@PathVariable String extid, @Valid @RequestBody RequestOutcomeUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete outcome (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = outcomeService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

@Component
@RequiredArgsConstructor
class OutcomeConverter {

    private final TrialRepository trialRepository;

    Long resolveTrialId(String trialExtid) {
        return trialRepository.findByExtid(trialExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Trial", trialExtid))
                .getId();
    }

    private String resolveTrialExtid(Long trialId) {
        if (trialId == null) return null;
        return trialRepository.findById(trialId)
                .map(t -> t.getExtid())
                .orElse(null);
    }

    Outcome toDomain(RequestOutcomeCreate request) {
        return Outcome.builder()
                .trialId(resolveTrialId(request.getTrialExtid()))
                .outcomeType(request.getOutcomeType())
                .measure(request.getMeasure())
                .description(request.getDescription())
                .timeFrame(request.getTimeFrame())
                .build();
    }

    Outcome toDomain(RequestOutcomeUpdate request) {
        return Outcome.builder()
                .trialId(request.getTrialExtid() != null ? resolveTrialId(request.getTrialExtid()) : null)
                .outcomeType(request.getOutcomeType())
                .measure(request.getMeasure())
                .description(request.getDescription())
                .timeFrame(request.getTimeFrame())
                .build();
    }

    ResponseOutcome toResponse(Outcome item) {
        return ResponseOutcome.builder()
                .extid(item.getExtid())
                .trialExtid(resolveTrialExtid(item.getTrialId()))
                .outcomeType(item.getOutcomeType())
                .measure(item.getMeasure())
                .description(item.getDescription())
                .timeFrame(item.getTimeFrame())
                .build();
    }

    List<ResponseOutcome> toResponse(List<Outcome> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestOutcomeUpdate request) {
        if (request.getTrialExtid() == null &&
                request.getOutcomeType() == null &&
                request.getMeasure() == null &&
                request.getDescription() == null &&
                request.getTimeFrame() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
