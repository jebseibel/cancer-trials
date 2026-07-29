package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.database.db.repository.TrialRepository;
import com.seibel.cancer.service.LocationService;
import com.seibel.cancer.web.request.RequestLocationCreate;
import com.seibel.cancer.web.request.RequestLocationUpdate;
import com.seibel.cancer.web.response.ResponseLocation;
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
@RequestMapping("/api/location")
@Validated
@Tag(name = "Location", description = "Location CRUD endpoints")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationConverter converter;

    @GetMapping
    @Operation(summary = "List locations (paginated)")
    public Page<ResponseLocation> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "facility") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return locationService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/by-trial/{trialExtid}")
    @Operation(summary = "List all locations for a trial (unpaginated)")
    public List<ResponseLocation> getByTrialExtid(@PathVariable String trialExtid) {
        Long trialId = converter.resolveTrialId(trialExtid);
        return converter.toResponse(locationService.findByTrialId(trialId));
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get location by extid")
    public ResponseLocation getByExtid(@PathVariable String extid) {
        return converter.toResponse(locationService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create location")
    public ResponseEntity<ResponseLocation> create(@Valid @RequestBody RequestLocationCreate request) {
        Location created = locationService.create(converter.toDomain(request));
        URI location = URI.create("/api/location/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update location (full or partial)")
    public ResponseLocation update(@PathVariable String extid, @Valid @RequestBody RequestLocationUpdate request) {
        converter.validateUpdateRequest(request);
        Location updated = locationService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch location (partial update)")
    public ResponseLocation patch(@PathVariable String extid, @Valid @RequestBody RequestLocationUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete location (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = locationService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

@Component
@RequiredArgsConstructor
class LocationConverter {

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

    Location toDomain(RequestLocationCreate request) {
        return Location.builder()
                .trialId(resolveTrialId(request.getTrialExtid()))
                .facility(request.getFacility())
                .city(request.getCity())
                .state(request.getState())
                .zip(request.getZip())
                .country(request.getCountry())
                .status(request.getStatus())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    Location toDomain(RequestLocationUpdate request) {
        return Location.builder()
                .trialId(request.getTrialExtid() != null ? resolveTrialId(request.getTrialExtid()) : null)
                .facility(request.getFacility())
                .city(request.getCity())
                .state(request.getState())
                .zip(request.getZip())
                .country(request.getCountry())
                .status(request.getStatus())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    ResponseLocation toResponse(Location item) {
        return ResponseLocation.builder()
                .extid(item.getExtid())
                .trialExtid(resolveTrialExtid(item.getTrialId()))
                .facility(item.getFacility())
                .city(item.getCity())
                .state(item.getState())
                .zip(item.getZip())
                .country(item.getCountry())
                .status(item.getStatus())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .build();
    }

    List<ResponseLocation> toResponse(List<Location> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestLocationUpdate request) {
        if (request.getTrialExtid() == null &&
                request.getFacility() == null &&
                request.getCity() == null &&
                request.getState() == null &&
                request.getZip() == null &&
                request.getCountry() == null &&
                request.getStatus() == null &&
                request.getLatitude() == null &&
                request.getLongitude() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
