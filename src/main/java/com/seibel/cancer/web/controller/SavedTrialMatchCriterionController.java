package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.SavedTrialMatchRepository;
import com.seibel.cancer.service.SavedTrialMatchCriterionService;
import com.seibel.cancer.web.request.RequestSavedTrialMatchCriterionCreate;
import com.seibel.cancer.web.request.RequestSavedTrialMatchCriterionUpdate;
import com.seibel.cancer.web.response.ResponseSavedTrialMatchCriterion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/trialmatchcriterion")
@Validated
@Tag(name = "SavedTrialMatchCriterion", description = "SavedTrialMatchCriterion CRUD endpoints")
public class SavedTrialMatchCriterionController {

    private final SavedTrialMatchCriterionService trialMatchCriterionService;
    private final SavedTrialMatchCriterionConverter converter;

    public SavedTrialMatchCriterionController(SavedTrialMatchCriterionService trialMatchCriterionService,
                                         SavedTrialMatchRepository trialMatchRepository) {
        this.trialMatchCriterionService = trialMatchCriterionService;
        this.converter = new SavedTrialMatchCriterionConverter(trialMatchRepository);
    }

    @GetMapping
    @Operation(summary = "List trialMatchCriteria (paginated)")
    public Page<ResponseSavedTrialMatchCriterion> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "score") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active) {
        return trialMatchCriterionService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get trialMatchCriterion details")
    public ResponseSavedTrialMatchCriterion getByExtid(@PathVariable String extid) {
        return converter.toResponse(trialMatchCriterionService.findByExtid(extid));
    }

    @GetMapping("/by-trialmatch/{trialMatchExtid}")
    @Operation(summary = "The evidence behind one match, best-scoring first (unpaginated)")
    public List<ResponseSavedTrialMatchCriterion> getByTrialMatchExtid(@PathVariable String trialMatchExtid) {
        Long trialMatchId = converter.resolveTrialMatchId(trialMatchExtid);
        return converter.toResponse(trialMatchCriterionService.findByTrialMatchId(trialMatchId));
    }

    @PostMapping
    @Operation(summary = "Create trialMatchCriterion")
    public ResponseEntity<ResponseSavedTrialMatchCriterion> create(
            @Valid @RequestBody RequestSavedTrialMatchCriterionCreate request) {
        SavedTrialMatchCriterion created = trialMatchCriterionService.create(converter.toDomain(request));
        URI location = URI.create("/api/trialmatchcriterion/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update trialMatchCriterion")
    public ResponseSavedTrialMatchCriterion update(@PathVariable String extid,
                                              @Valid @RequestBody RequestSavedTrialMatchCriterionUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(trialMatchCriterionService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update trialMatchCriterion")
    public ResponseSavedTrialMatchCriterion patch(@PathVariable String extid,
                                             @Valid @RequestBody RequestSavedTrialMatchCriterionUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete trialMatchCriterion")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return trialMatchCriterionService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class SavedTrialMatchCriterionConverter {

    private final SavedTrialMatchRepository trialMatchRepository;

    SavedTrialMatchCriterionConverter(SavedTrialMatchRepository trialMatchRepository) {
        this.trialMatchRepository = trialMatchRepository;
    }

    Long resolveTrialMatchId(String trialMatchExtid) {
        return trialMatchRepository.findByExtid(trialMatchExtid)
                .orElseThrow(() -> new ResourceNotFoundException("SavedTrialMatch", trialMatchExtid))
                .getId();
    }

    private String resolveTrialMatchExtid(Long trialMatchId) {
        if (trialMatchId == null) return null;
        return trialMatchRepository.findById(trialMatchId).map(m -> m.getExtid()).orElse(null);
    }

    SavedTrialMatchCriterion toDomain(RequestSavedTrialMatchCriterionCreate request) {
        return SavedTrialMatchCriterion.builder()
                .trialMatchId(request.getTrialMatchExtid() != null
                        ? resolveTrialMatchId(request.getTrialMatchExtid()) : null)
                .chunkText(request.getChunkText())
                .score(request.getScore())
                .isExclusion(request.getIsExclusion())
                .source(request.getSource())
                .ordinal(request.getOrdinal())
                .build();
    }

    SavedTrialMatchCriterion toDomain(RequestSavedTrialMatchCriterionUpdate request) {
        return SavedTrialMatchCriterion.builder()
                .trialMatchId(request.getTrialMatchExtid() != null
                        ? resolveTrialMatchId(request.getTrialMatchExtid()) : null)
                .chunkText(request.getChunkText())
                .score(request.getScore())
                .isExclusion(request.getIsExclusion())
                .source(request.getSource())
                .ordinal(request.getOrdinal())
                .build();
    }

    ResponseSavedTrialMatchCriterion toResponse(SavedTrialMatchCriterion item) {
        return ResponseSavedTrialMatchCriterion.builder()
                .extid(item.getExtid())
                .trialMatchExtid(resolveTrialMatchExtid(item.getTrialMatchId()))
                .chunkText(item.getChunkText())
                .score(item.getScore())
                .isExclusion(item.getIsExclusion())
                .source(item.getSource())
                .ordinal(item.getOrdinal())
                .build();
    }

    List<ResponseSavedTrialMatchCriterion> toResponse(List<SavedTrialMatchCriterion> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestSavedTrialMatchCriterionUpdate request) {
        if (request.getTrialMatchExtid() == null
                && request.getChunkText() == null
                && request.getScore() == null
                && request.getIsExclusion() == null
                && request.getSource() == null
                && request.getOrdinal() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
