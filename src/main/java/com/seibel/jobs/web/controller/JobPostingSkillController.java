package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.JobPostingSkill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.JobPostingSkillService;
import com.seibel.jobs.web.request.RequestJobPostingSkillCreate;
import com.seibel.jobs.web.request.RequestJobPostingSkillUpdate;
import com.seibel.jobs.web.response.ResponseJobPostingSkill;
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
@RequestMapping("/api/job-posting-skill")
@Validated
@Tag(name = "JobPostingSkill", description = "Job posting <-> skill link CRUD endpoints")
@RequiredArgsConstructor
public class JobPostingSkillController {

    private final JobPostingSkillService jobPostingSkillService;
    private final JobPostingSkillConverter converter = new JobPostingSkillConverter();

    @GetMapping
    @Operation(summary = "List job posting skill links (paginated)")
    public Page<ResponseJobPostingSkill> getAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return jobPostingSkillService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get job posting skill link by extid")
    public ResponseJobPostingSkill getByExtid(@PathVariable String extid) {
        return converter.toResponse(jobPostingSkillService.findByExtid(extid));
    }

    @GetMapping("/by-job-posting/{jobPostingId}")
    @Operation(summary = "List skill links for a job posting")
    public List<ResponseJobPostingSkill> getByJobPostingId(@PathVariable Long jobPostingId) {
        return converter.toResponse(jobPostingSkillService.findByJobPostingId(jobPostingId));
    }

    @GetMapping("/by-skill/{skillId}")
    @Operation(summary = "List job posting links for a skill")
    public List<ResponseJobPostingSkill> getBySkillId(@PathVariable Long skillId) {
        return converter.toResponse(jobPostingSkillService.findBySkillId(skillId));
    }

    @PostMapping
    @Operation(summary = "Create job posting skill link")
    public ResponseEntity<ResponseJobPostingSkill> create(@Valid @RequestBody RequestJobPostingSkillCreate request) {
        JobPostingSkill created = jobPostingSkillService.create(converter.toDomain(request));
        URI location = URI.create("/api/job-posting-skill/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update job posting skill link (full or partial)")
    public ResponseJobPostingSkill update(@PathVariable String extid, @Valid @RequestBody RequestJobPostingSkillUpdate request) {
        converter.validateUpdateRequest(request);
        JobPostingSkill updated = jobPostingSkillService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch job posting skill link (partial update)")
    public ResponseJobPostingSkill patch(@PathVariable String extid, @Valid @RequestBody RequestJobPostingSkillUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete job posting skill link (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = jobPostingSkillService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class JobPostingSkillConverter {

    JobPostingSkill toDomain(RequestJobPostingSkillCreate request) {
        return JobPostingSkill.builder()
                .jobPostingId(request.getJobPostingId())
                .skillId(request.getSkillId())
                .build();
    }

    JobPostingSkill toDomain(RequestJobPostingSkillUpdate request) {
        return JobPostingSkill.builder()
                .jobPostingId(request.getJobPostingId())
                .skillId(request.getSkillId())
                .build();
    }

    ResponseJobPostingSkill toResponse(JobPostingSkill item) {
        return ResponseJobPostingSkill.builder()
                .extid(item.getExtid())
                .jobPostingId(item.getJobPostingId())
                .skillId(item.getSkillId())
                .build();
    }

    List<ResponseJobPostingSkill> toResponse(List<JobPostingSkill> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestJobPostingSkillUpdate request) {
        if (request.getJobPostingId() == null && request.getSkillId() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
