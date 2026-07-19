package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.Contact;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.ContactService;
import com.seibel.jobs.web.request.RequestContactCreate;
import com.seibel.jobs.web.request.RequestContactUpdate;
import com.seibel.jobs.web.response.ResponseContact;
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
@RequestMapping("/api/contact")
@Validated
@Tag(name = "Contact", description = "Contact CRUD endpoints")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final ContactConverter converter = new ContactConverter();

    @GetMapping
    @Operation(summary = "List contacts (paginated)")
    public Page<ResponseContact> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return contactService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get contact by extid")
    public ResponseContact getByExtid(@PathVariable String extid) {
        return converter.toResponse(contactService.findByExtid(extid));
    }

    @GetMapping("/by-company/{companyId}")
    @Operation(summary = "List contacts for a company")
    public List<ResponseContact> getByCompanyId(@PathVariable Long companyId) {
        return converter.toResponse(contactService.findByCompanyId(companyId));
    }

    @GetMapping("/by-job-posting/{jobPostingId}")
    @Operation(summary = "List contacts for a job posting")
    public List<ResponseContact> getByJobPostingId(@PathVariable Long jobPostingId) {
        return converter.toResponse(contactService.findByJobPostingId(jobPostingId));
    }

    @PostMapping
    @Operation(summary = "Create contact")
    public ResponseEntity<ResponseContact> create(@Valid @RequestBody RequestContactCreate request) {
        Contact created = contactService.create(converter.toDomain(request));
        URI location = URI.create("/api/contact/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update contact (full or partial)")
    public ResponseContact update(@PathVariable String extid, @Valid @RequestBody RequestContactUpdate request) {
        converter.validateUpdateRequest(request);
        Contact updated = contactService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch contact (partial update)")
    public ResponseContact patch(@PathVariable String extid, @Valid @RequestBody RequestContactUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete contact (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = contactService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class ContactConverter {

    Contact toDomain(RequestContactCreate request) {
        return Contact.builder()
                .companyId(request.getCompanyId())
                .jobPostingId(request.getJobPostingId())
                .name(request.getName())
                .role(request.getRole())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .build();
    }

    Contact toDomain(RequestContactUpdate request) {
        return Contact.builder()
                .companyId(request.getCompanyId())
                .jobPostingId(request.getJobPostingId())
                .name(request.getName())
                .role(request.getRole())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .build();
    }

    ResponseContact toResponse(Contact item) {
        return ResponseContact.builder()
                .extid(item.getExtid())
                .companyId(item.getCompanyId())
                .jobPostingId(item.getJobPostingId())
                .name(item.getName())
                .role(item.getRole())
                .email(item.getEmail())
                .phone(item.getPhone())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseContact> toResponse(List<Contact> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestContactUpdate request) {
        if (request.getCompanyId() == null &&
                request.getJobPostingId() == null &&
                request.getName() == null &&
                request.getRole() == null &&
                request.getEmail() == null &&
                request.getPhone() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
