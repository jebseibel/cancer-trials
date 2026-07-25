package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ValidationException;
import com.seibel.cancer.service.KeywordService;
import com.seibel.cancer.web.request.RequestKeywordCreate;
import com.seibel.cancer.web.request.RequestKeywordUpdate;
import com.seibel.cancer.web.response.ResponseKeyword;
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
@RequestMapping("/api/keyword")
@Validated
@Tag(name = "Keyword", description = "Keyword CRUD endpoints")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;
    private final KeywordConverter converter = new KeywordConverter();

    @GetMapping
    @Operation(summary = "List keywords (paginated)")
    public Page<ResponseKeyword> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return keywordService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get keyword by extid")
    public ResponseKeyword getByExtid(@PathVariable String extid) {
        return converter.toResponse(keywordService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create keyword")
    public ResponseEntity<ResponseKeyword> create(@Valid @RequestBody RequestKeywordCreate request) {
        Keyword created = keywordService.create(converter.toDomain(request));
        URI location = URI.create("/api/keyword/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update keyword (full or partial)")
    public ResponseKeyword update(@PathVariable String extid, @Valid @RequestBody RequestKeywordUpdate request) {
        converter.validateUpdateRequest(request);
        Keyword updated = keywordService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch keyword (partial update)")
    public ResponseKeyword patch(@PathVariable String extid, @Valid @RequestBody RequestKeywordUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete keyword (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = keywordService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class KeywordConverter {

    Keyword toDomain(RequestKeywordCreate request) {
        return Keyword.builder()
                .name(request.getName())
                .build();
    }

    Keyword toDomain(RequestKeywordUpdate request) {
        return Keyword.builder()
                .name(request.getName())
                .build();
    }

    ResponseKeyword toResponse(Keyword item) {
        return ResponseKeyword.builder()
                .extid(item.getExtid())
                .name(item.getName())
                .build();
    }

    List<ResponseKeyword> toResponse(List<Keyword> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestKeywordUpdate request) {
        if (request.getName() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
