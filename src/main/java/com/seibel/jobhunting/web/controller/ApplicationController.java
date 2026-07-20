package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.Application;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.ApplicationService;
import com.seibel.jobhunting.web.request.RequestApplicationCreate;
import com.seibel.jobhunting.web.request.RequestApplicationUpdate;
import com.seibel.jobhunting.web.response.ResponseApplication;
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
@RequestMapping("/api/application")
@Validated
@Tag(name = "Application", description = "Application CRUD endpoints")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationConverter converter = new ApplicationConverter();

    @GetMapping
    @Operation(summary = "List applications (paginated)")
    public Page<ResponseApplication> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "dateApplied") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return applicationService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get application by extid")
    public ResponseApplication getByExtid(@PathVariable String extid) {
        return converter.toResponse(applicationService.findByExtid(extid));
    }

    @GetMapping("/by-job-posting/{jobPostingId}")
    @Operation(summary = "List applications for a job posting")
    public List<ResponseApplication> getByJobPostingId(@PathVariable Long jobPostingId) {
        return converter.toResponse(applicationService.findByJobPostingId(jobPostingId));
    }

    @PostMapping
    @Operation(summary = "Create application")
    public ResponseEntity<ResponseApplication> create(@Valid @RequestBody RequestApplicationCreate request) {
        Application created = applicationService.create(converter.toDomain(request));
        URI location = URI.create("/api/application/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update application (full or partial)")
    public ResponseApplication update(@PathVariable String extid, @Valid @RequestBody RequestApplicationUpdate request) {
        converter.validateUpdateRequest(request);
        Application updated = applicationService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch application (partial update)")
    public ResponseApplication patch(@PathVariable String extid, @Valid @RequestBody RequestApplicationUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete application (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = applicationService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class ApplicationConverter {

    Application toDomain(RequestApplicationCreate request) {
        return Application.builder()
                .jobPostingId(request.getJobPostingId())
                .dateApplied(request.getDateApplied())
                .resumeVersion(request.getResumeVersion())
                .applicationStatus(request.getApplicationStatus())
                .notes(request.getNotes())
                .build();
    }

    Application toDomain(RequestApplicationUpdate request) {
        return Application.builder()
                .jobPostingId(request.getJobPostingId())
                .dateApplied(request.getDateApplied())
                .resumeVersion(request.getResumeVersion())
                .applicationStatus(request.getApplicationStatus())
                .notes(request.getNotes())
                .build();
    }

    ResponseApplication toResponse(Application item) {
        return ResponseApplication.builder()
                .extid(item.getExtid())
                .jobPostingId(item.getJobPostingId())
                .dateApplied(item.getDateApplied())
                .resumeVersion(item.getResumeVersion())
                .applicationStatus(item.getApplicationStatus())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseApplication> toResponse(List<Application> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestApplicationUpdate request) {
        if (request.getJobPostingId() == null &&
                request.getDateApplied() == null &&
                request.getResumeVersion() == null &&
                request.getApplicationStatus() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
