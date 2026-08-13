package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.service.CurrentUserService;
import com.seibel.cancer.database.db.repository.PatientDiagnosisRepository;
import com.seibel.cancer.database.db.repository.TrialRepository;
import com.seibel.cancer.service.SavedTrialMatchService;
import com.seibel.cancer.web.request.RequestSavedTrialMatchCreate;
import com.seibel.cancer.web.request.RequestSavedTrialMatchUpdate;
import com.seibel.cancer.web.response.ResponseSavedTrialMatch;
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
@RequestMapping("/api/trialmatch")
@Validated
@Tag(name = "SavedTrialMatch", description = "SavedTrialMatch CRUD endpoints")
public class SavedTrialMatchController {

    private final SavedTrialMatchService trialMatchService;
    private final CurrentUserService currentUserService;
    private final SavedTrialMatchConverter converter;

    public SavedTrialMatchController(SavedTrialMatchService trialMatchService,
                                           CurrentUserService currentUserService,
                                TrialRepository trialRepository,
                                PatientRepository patientRepository,
                                PatientDiagnosisRepository patientDiagnosisRepository) {
        this.trialMatchService = trialMatchService;
        this.currentUserService = currentUserService;
        this.converter = new SavedTrialMatchConverter(trialRepository, patientRepository, patientDiagnosisRepository);
    }

    @GetMapping
    @Operation(summary = "List trialMatches (paginated)")
    public Page<ResponseSavedTrialMatch> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "topScore") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active) {
        return trialMatchService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get trialMatch details")
    public ResponseSavedTrialMatch getByExtid(@PathVariable String extid) {
        return converter.toResponse(trialMatchService.findByExtid(extid));
    }

    @GetMapping("/by-searchrun/{searchRunId}")
    @Operation(summary = "All matches from one search run, best-ranked first (unpaginated)")
    public List<ResponseSavedTrialMatch> getBySearchRunId(@PathVariable String searchRunId) {
        return converter.toResponse(trialMatchService.findBySearchRunId(searchRunId));
    }

    @GetMapping("/by-patient/{patientExtid}")
    @Operation(summary = "Every match ever recorded for an app user, newest first (unpaginated)")
    public List<ResponseSavedTrialMatch> getByPatientExtid(@PathVariable String patientExtid) {
        Long patientId = currentUserService.requireAccessId(patientExtid, AccessLevel.VIEW_TRIALS);
        return converter.toResponse(trialMatchService.findByPatientId(patientId));
    }

    @GetMapping("/by-trial/{trialExtid}")
    @Operation(summary = "Every run a trial has appeared in, newest first (unpaginated)")
    public List<ResponseSavedTrialMatch> getByTrialExtid(@PathVariable String trialExtid) {
        Long trialId = converter.resolveTrialId(trialExtid);
        return converter.toResponse(trialMatchService.findByTrialId(trialId));
    }

    @PostMapping
    @Operation(summary = "Create trialMatch")
    public ResponseEntity<ResponseSavedTrialMatch> create(@Valid @RequestBody RequestSavedTrialMatchCreate request) {
        SavedTrialMatch created = trialMatchService.create(converter.toDomain(request));
        URI location = URI.create("/api/trialmatch/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update trialMatch")
    public ResponseSavedTrialMatch update(@PathVariable String extid,
                                     @Valid @RequestBody RequestSavedTrialMatchUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(trialMatchService.update(extid, converter.toDomain(request)));
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Partially update trialMatch")
    public ResponseSavedTrialMatch patch(@PathVariable String extid,
                                    @Valid @RequestBody RequestSavedTrialMatchUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Soft delete trialMatch")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        return trialMatchService.delete(extid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

class SavedTrialMatchConverter {

    private final TrialRepository trialRepository;
    private final PatientRepository patientRepository;
    private final PatientDiagnosisRepository patientDiagnosisRepository;

    SavedTrialMatchConverter(TrialRepository trialRepository,
                        PatientRepository patientRepository,
                        PatientDiagnosisRepository patientDiagnosisRepository) {
        this.trialRepository = trialRepository;
        this.patientRepository = patientRepository;
        this.patientDiagnosisRepository = patientDiagnosisRepository;
    }

    Long resolveTrialId(String trialExtid) {
        return trialRepository.findByExtid(trialExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Trial", trialExtid))
                .getId();
    }

    Long resolvePatientId(String patientExtid) {
        return patientRepository.findByExtid(patientExtid)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientExtid))
                .getId();
    }

    Long resolvePatientDiagnosisId(String patientDiagnosisExtid) {
        return patientDiagnosisRepository.findByExtid(patientDiagnosisExtid)
                .orElseThrow(() -> new ResourceNotFoundException("PatientDiagnosis", patientDiagnosisExtid))
                .getId();
    }

    private String resolveTrialExtid(Long trialId) {
        if (trialId == null) return null;
        return trialRepository.findById(trialId).map(t -> t.getExtid()).orElse(null);
    }

    private String resolvePatientExtid(Long patientId) {
        if (patientId == null) return null;
        return patientRepository.findById(patientId).map(u -> u.getExtid()).orElse(null);
    }

    private String resolvePatientDiagnosisExtid(Long patientDiagnosisId) {
        if (patientDiagnosisId == null) return null;
        return patientDiagnosisRepository.findById(patientDiagnosisId).map(d -> d.getExtid()).orElse(null);
    }

    SavedTrialMatch toDomain(RequestSavedTrialMatchCreate request) {
        return SavedTrialMatch.builder()
                .trialId(request.getTrialExtid() != null ? resolveTrialId(request.getTrialExtid()) : null)
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .searchRunId(request.getSearchRunId())
                .queryText(request.getQueryText())
                .topScore(request.getTopScore())
                .matchRank(request.getMatchRank())
                .snapshotErStatus(request.getSnapshotErStatus())
                .snapshotPrStatus(request.getSnapshotPrStatus())
                .snapshotHer2Status(request.getSnapshotHer2Status())
                .snapshotStage(request.getSnapshotStage())
                .snapshotBiomarkers(request.getSnapshotBiomarkers())
                .matchedAt(request.getMatchedAt())
                .build();
    }

    SavedTrialMatch toDomain(RequestSavedTrialMatchUpdate request) {
        return SavedTrialMatch.builder()
                .trialId(request.getTrialExtid() != null ? resolveTrialId(request.getTrialExtid()) : null)
                .patientId(request.getPatientExtid() != null ? resolvePatientId(request.getPatientExtid()) : null)
                .patientDiagnosisId(request.getPatientDiagnosisExtid() != null
                        ? resolvePatientDiagnosisId(request.getPatientDiagnosisExtid()) : null)
                .searchRunId(request.getSearchRunId())
                .queryText(request.getQueryText())
                .topScore(request.getTopScore())
                .matchRank(request.getMatchRank())
                .snapshotErStatus(request.getSnapshotErStatus())
                .snapshotPrStatus(request.getSnapshotPrStatus())
                .snapshotHer2Status(request.getSnapshotHer2Status())
                .snapshotStage(request.getSnapshotStage())
                .snapshotBiomarkers(request.getSnapshotBiomarkers())
                .matchedAt(request.getMatchedAt())
                .build();
    }

    ResponseSavedTrialMatch toResponse(SavedTrialMatch item) {
        return ResponseSavedTrialMatch.builder()
                .extid(item.getExtid())
                .trialExtid(resolveTrialExtid(item.getTrialId()))
                .patientExtid(resolvePatientExtid(item.getPatientId()))
                .patientDiagnosisExtid(resolvePatientDiagnosisExtid(item.getPatientDiagnosisId()))
                .searchRunId(item.getSearchRunId())
                .queryText(item.getQueryText())
                .topScore(item.getTopScore())
                .matchRank(item.getMatchRank())
                .snapshotErStatus(item.getSnapshotErStatus())
                .snapshotPrStatus(item.getSnapshotPrStatus())
                .snapshotHer2Status(item.getSnapshotHer2Status())
                .snapshotStage(item.getSnapshotStage())
                .snapshotBiomarkers(item.getSnapshotBiomarkers())
                .matchedAt(item.getMatchedAt())
                .build();
    }

    List<ResponseSavedTrialMatch> toResponse(List<SavedTrialMatch> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestSavedTrialMatchUpdate request) {
        if (request.getTrialExtid() == null
                && request.getPatientExtid() == null
                && request.getPatientDiagnosisExtid() == null
                && request.getSearchRunId() == null
                && request.getQueryText() == null
                && request.getTopScore() == null
                && request.getMatchRank() == null
                && request.getSnapshotErStatus() == null
                && request.getSnapshotPrStatus() == null
                && request.getSnapshotHer2Status() == null
                && request.getSnapshotStage() == null
                && request.getSnapshotBiomarkers() == null
                && request.getMatchedAt() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
