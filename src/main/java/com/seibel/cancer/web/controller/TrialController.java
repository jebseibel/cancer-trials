package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.TrialService;
import com.seibel.cancer.web.request.RequestTrialCreate;
import com.seibel.cancer.web.request.RequestTrialUpdate;
import com.seibel.cancer.web.response.ResponseTrial;
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
@RequestMapping("/api/trial")
@Validated
@Tag(name = "Trial", description = "Trial CRUD endpoints")
@RequiredArgsConstructor
public class TrialController {

    private final TrialService trialService;
    private final TrialConverter converter = new TrialConverter();

    @GetMapping
    @Operation(summary = "List trials (paginated)")
    public Page<ResponseTrial> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "briefTitle") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return trialService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get trial by extid")
    public ResponseTrial getByExtid(@PathVariable String extid) {
        return converter.toResponse(trialService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create trial")
    public ResponseEntity<ResponseTrial> create(@Valid @RequestBody RequestTrialCreate request) {
        Trial created = trialService.create(converter.toDomain(request));
        URI location = URI.create("/api/trial/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update trial (full or partial)")
    public ResponseTrial update(@PathVariable String extid, @Valid @RequestBody RequestTrialUpdate request) {
        converter.validateUpdateRequest(request);
        Trial updated = trialService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch trial (partial update)")
    public ResponseTrial patch(@PathVariable String extid, @Valid @RequestBody RequestTrialUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete trial (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = trialService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class TrialConverter {

    Trial toDomain(RequestTrialCreate request) {
        return Trial.builder()
                .nctId(request.getNctId())
                .briefTitle(request.getBriefTitle())
                .officialTitle(request.getOfficialTitle())
                .overallStatus(request.getOverallStatus())
                .studyType(request.getStudyType())
                .briefSummary(request.getBriefSummary())
                .detailedDescription(request.getDetailedDescription())
                .startDate(request.getStartDate())
                .primaryCompletionDate(request.getPrimaryCompletionDate())
                .completionDate(request.getCompletionDate())
                .lastUpdatePostedDate(request.getLastUpdatePostedDate())
                .enrollmentCount(request.getEnrollmentCount())
                .enrollmentType(request.getEnrollmentType())
                .healthyVolunteers(request.getHealthyVolunteers())
                .sex(request.getSex())
                .minimumAge(request.getMinimumAge())
                .maximumAge(request.getMaximumAge())
                .eligibilityCriteria(request.getEligibilityCriteria())
                .isPaidStudy(request.getIsPaidStudy())
                .paidAmount(request.getPaidAmount())
                .primaryTrialSourceId(request.getPrimaryTrialSourceId())
                .build();
    }

    Trial toDomain(RequestTrialUpdate request) {
        return Trial.builder()
                .nctId(request.getNctId())
                .briefTitle(request.getBriefTitle())
                .officialTitle(request.getOfficialTitle())
                .overallStatus(request.getOverallStatus())
                .studyType(request.getStudyType())
                .briefSummary(request.getBriefSummary())
                .detailedDescription(request.getDetailedDescription())
                .startDate(request.getStartDate())
                .primaryCompletionDate(request.getPrimaryCompletionDate())
                .completionDate(request.getCompletionDate())
                .lastUpdatePostedDate(request.getLastUpdatePostedDate())
                .enrollmentCount(request.getEnrollmentCount())
                .enrollmentType(request.getEnrollmentType())
                .healthyVolunteers(request.getHealthyVolunteers())
                .sex(request.getSex())
                .minimumAge(request.getMinimumAge())
                .maximumAge(request.getMaximumAge())
                .eligibilityCriteria(request.getEligibilityCriteria())
                .isPaidStudy(request.getIsPaidStudy())
                .paidAmount(request.getPaidAmount())
                .primaryTrialSourceId(request.getPrimaryTrialSourceId())
                .build();
    }

    ResponseTrial toResponse(Trial item) {
        return ResponseTrial.builder()
                .extid(item.getExtid())
                .nctId(item.getNctId())
                .briefTitle(item.getBriefTitle())
                .officialTitle(item.getOfficialTitle())
                .overallStatus(item.getOverallStatus())
                .studyType(item.getStudyType())
                .briefSummary(item.getBriefSummary())
                .detailedDescription(item.getDetailedDescription())
                .startDate(item.getStartDate())
                .primaryCompletionDate(item.getPrimaryCompletionDate())
                .completionDate(item.getCompletionDate())
                .lastUpdatePostedDate(item.getLastUpdatePostedDate())
                .enrollmentCount(item.getEnrollmentCount())
                .enrollmentType(item.getEnrollmentType())
                .healthyVolunteers(item.getHealthyVolunteers())
                .sex(item.getSex())
                .minimumAge(item.getMinimumAge())
                .maximumAge(item.getMaximumAge())
                .eligibilityCriteria(item.getEligibilityCriteria())
                .isPaidStudy(item.getIsPaidStudy())
                .paidAmount(item.getPaidAmount())
                .primaryTrialSourceId(item.getPrimaryTrialSourceId())
                .build();
    }

    List<ResponseTrial> toResponse(List<Trial> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestTrialUpdate request) {
        if (request.getNctId() == null &&
                request.getBriefTitle() == null &&
                request.getOfficialTitle() == null &&
                request.getOverallStatus() == null &&
                request.getStudyType() == null &&
                request.getBriefSummary() == null &&
                request.getDetailedDescription() == null &&
                request.getStartDate() == null &&
                request.getPrimaryCompletionDate() == null &&
                request.getCompletionDate() == null &&
                request.getLastUpdatePostedDate() == null &&
                request.getEnrollmentCount() == null &&
                request.getEnrollmentType() == null &&
                request.getHealthyVolunteers() == null &&
                request.getSex() == null &&
                request.getMinimumAge() == null &&
                request.getMaximumAge() == null &&
                request.getEligibilityCriteria() == null &&
                request.getIsPaidStudy() == null &&
                request.getPaidAmount() == null &&
                request.getPrimaryTrialSourceId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
