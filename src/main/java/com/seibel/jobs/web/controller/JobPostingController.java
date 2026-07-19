package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.JobPosting;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.JobPostingService;
import com.seibel.jobs.web.request.RequestJobPostingCreate;
import com.seibel.jobs.web.request.RequestJobPostingUpdate;
import com.seibel.jobs.web.response.ResponseJobPosting;
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
@RequestMapping("/api/job-posting")
@Validated
@Tag(name = "JobPosting", description = "Job posting CRUD endpoints")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final JobPostingConverter converter = new JobPostingConverter();

    @GetMapping
    @Operation(summary = "List job postings (paginated)")
    public Page<ResponseJobPosting> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "postedAt") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return jobPostingService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get job posting by extid")
    public ResponseJobPosting getByExtid(@PathVariable String extid) {
        return converter.toResponse(jobPostingService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create job posting")
    public ResponseEntity<ResponseJobPosting> create(@Valid @RequestBody RequestJobPostingCreate request) {
        JobPosting created = jobPostingService.create(converter.toDomain(request));
        URI location = URI.create("/api/job-posting/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update job posting (full or partial)")
    public ResponseJobPosting update(@PathVariable String extid, @Valid @RequestBody RequestJobPostingUpdate request) {
        converter.validateUpdateRequest(request);
        JobPosting updated = jobPostingService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch job posting (partial update)")
    public ResponseJobPosting patch(@PathVariable String extid, @Valid @RequestBody RequestJobPostingUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete job posting (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = jobPostingService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class JobPostingConverter {

    JobPosting toDomain(RequestJobPostingCreate request) {
        return JobPosting.builder()
                .title(request.getTitle())
                .companyId(request.getCompanyId())
                .description(request.getDescription())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .workMode(request.getWorkMode())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .source(request.getSource())
                .sourceUrl(request.getSourceUrl())
                .postedAt(request.getPostedAt())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();
    }

    JobPosting toDomain(RequestJobPostingUpdate request) {
        return JobPosting.builder()
                .title(request.getTitle())
                .companyId(request.getCompanyId())
                .description(request.getDescription())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .workMode(request.getWorkMode())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .source(request.getSource())
                .sourceUrl(request.getSourceUrl())
                .postedAt(request.getPostedAt())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();
    }

    ResponseJobPosting toResponse(JobPosting item) {
        return ResponseJobPosting.builder()
                .extid(item.getExtid())
                .title(item.getTitle())
                .companyId(item.getCompanyId())
                .description(item.getDescription())
                .city(item.getCity())
                .state(item.getState())
                .country(item.getCountry())
                .workMode(item.getWorkMode())
                .salaryMin(item.getSalaryMin())
                .salaryMax(item.getSalaryMax())
                .salaryCurrency(item.getSalaryCurrency())
                .source(item.getSource())
                .sourceUrl(item.getSourceUrl())
                .postedAt(item.getPostedAt())
                .status(item.getStatus())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseJobPosting> toResponse(List<JobPosting> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestJobPostingUpdate request) {
        if (request.getTitle() == null &&
                request.getCompanyId() == null &&
                request.getDescription() == null &&
                request.getCity() == null &&
                request.getState() == null &&
                request.getCountry() == null &&
                request.getWorkMode() == null &&
                request.getSalaryMin() == null &&
                request.getSalaryMax() == null &&
                request.getSalaryCurrency() == null &&
                request.getSource() == null &&
                request.getSourceUrl() == null &&
                request.getPostedAt() == null &&
                request.getStatus() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
