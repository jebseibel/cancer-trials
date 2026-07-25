package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.SponsorService;
import com.seibel.cancer.web.request.RequestSponsorCreate;
import com.seibel.cancer.web.request.RequestSponsorUpdate;
import com.seibel.cancer.web.response.ResponseSponsor;
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
@RequestMapping("/api/sponsor")
@Validated
@Tag(name = "Sponsor", description = "Sponsor CRUD endpoints")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;
    private final SponsorConverter converter = new SponsorConverter();

    @GetMapping
    @Operation(summary = "List sponsors (paginated)")
    public Page<ResponseSponsor> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return sponsorService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get sponsor by extid")
    public ResponseSponsor getByExtid(@PathVariable String extid) {
        return converter.toResponse(sponsorService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create sponsor")
    public ResponseEntity<ResponseSponsor> create(@Valid @RequestBody RequestSponsorCreate request) {
        Sponsor created = sponsorService.create(converter.toDomain(request));
        URI location = URI.create("/api/sponsor/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update sponsor (full or partial)")
    public ResponseSponsor update(@PathVariable String extid, @Valid @RequestBody RequestSponsorUpdate request) {
        converter.validateUpdateRequest(request);
        Sponsor updated = sponsorService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch sponsor (partial update)")
    public ResponseSponsor patch(@PathVariable String extid, @Valid @RequestBody RequestSponsorUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete sponsor (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = sponsorService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class SponsorConverter {

    Sponsor toDomain(RequestSponsorCreate request) {
        return Sponsor.builder()
                .name(request.getName())
                .orgClass(request.getOrgClass())
                .build();
    }

    Sponsor toDomain(RequestSponsorUpdate request) {
        return Sponsor.builder()
                .name(request.getName())
                .orgClass(request.getOrgClass())
                .build();
    }

    ResponseSponsor toResponse(Sponsor item) {
        return ResponseSponsor.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .orgClass(item.getOrgClass())
                .build();
    }

    List<ResponseSponsor> toResponse(List<Sponsor> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestSponsorUpdate request) {
        if (request.getName() == null && request.getOrgClass() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
