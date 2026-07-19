package com.seibel.jobs.web.controller;

import com.seibel.jobs.common.domain.Company;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ValidationException;
import com.seibel.jobs.service.CompanyService;
import com.seibel.jobs.web.request.RequestCompanyCreate;
import com.seibel.jobs.web.request.RequestCompanyUpdate;
import com.seibel.jobs.web.response.ResponseCompany;
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
@RequestMapping("/api/company")
@Validated
@Tag(name = "Company", description = "Company CRUD endpoints")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyConverter converter = new CompanyConverter();

    @GetMapping
    @Operation(summary = "List companies (paginated)")
    public Page<ResponseCompany> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return companyService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get company by extid")
    public ResponseCompany getByExtid(@PathVariable String extid) {
        return converter.toResponse(companyService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create company")
    public ResponseEntity<ResponseCompany> create(@Valid @RequestBody RequestCompanyCreate request) {
        Company created = companyService.create(converter.toDomain(request));
        URI location = URI.create("/api/company/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update company (full or partial)")
    public ResponseCompany update(@PathVariable String extid, @Valid @RequestBody RequestCompanyUpdate request) {
        converter.validateUpdateRequest(request);
        Company updated = companyService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch company (partial update)")
    public ResponseCompany patch(@PathVariable String extid, @Valid @RequestBody RequestCompanyUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete company (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = companyService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class CompanyConverter {

    Company toDomain(RequestCompanyCreate request) {
        return Company.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .notes(request.getNotes())
                .build();
    }

    Company toDomain(RequestCompanyUpdate request) {
        return Company.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .notes(request.getNotes())
                .build();
    }

    ResponseCompany toResponse(Company item) {
        return ResponseCompany.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .website(item.getWebsite())
                .industry(item.getIndustry())
                .notes(item.getNotes())
                .build();
    }

    List<ResponseCompany> toResponse(List<Company> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestCompanyUpdate request) {
        if (request.getName() == null &&
                request.getWebsite() == null &&
                request.getIndustry() == null &&
                request.getNotes() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
