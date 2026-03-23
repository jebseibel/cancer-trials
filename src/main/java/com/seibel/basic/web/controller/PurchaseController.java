package com.seibel.basic.web.controller;

import com.seibel.basic.common.domain.Purchase;
import com.seibel.basic.common.enums.ActiveEnum;
import com.seibel.basic.common.exceptions.ValidationException;
import com.seibel.basic.service.PurchaseService;
import com.seibel.basic.web.request.RequestPurchaseCreate;
import com.seibel.basic.web.request.RequestPurchaseUpdate;
import com.seibel.basic.web.response.ResponsePurchase;
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
@RequestMapping("/api/purchase")
@Validated
@Tag(name = "Purchase", description = "Purchase CRUD endpoints")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchaseConverter converter = new PurchaseConverter();

    @GetMapping
    @Operation(summary = "List purchases (paginated)")
    public Page<ResponsePurchase> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "customer") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return purchaseService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get purchase by extid")
    public ResponsePurchase getByExtid(@PathVariable String extid) {
        return converter.toResponse(purchaseService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create purchase")
    public ResponseEntity<ResponsePurchase> create(@Valid @RequestBody RequestPurchaseCreate request) {
        Purchase created = purchaseService.create(converter.toDomain(request));
        URI location = URI.create("/api/purchase/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update purchase (full or partial)")
    public ResponsePurchase update(@PathVariable String extid, @Valid @RequestBody RequestPurchaseUpdate request) {
        converter.validateUpdateRequest(request);
        Purchase updated = purchaseService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch purchase (partial update)")
    public ResponsePurchase patch(@PathVariable String extid, @Valid @RequestBody RequestPurchaseUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete purchase (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = purchaseService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class PurchaseConverter {

    Purchase toDomain(RequestPurchaseCreate request) {
        return Purchase.builder()
                .customer(request.getCustomer())
                .items(request.getItems())
                .status(request.getStatus())
                .build();
    }

    Purchase toDomain(RequestPurchaseUpdate request) {
        return Purchase.builder()
                .customer(request.getCustomer())
                .items(request.getItems())
                .status(request.getStatus())
                .build();
    }

    ResponsePurchase toResponse(Purchase item) {
        return ResponsePurchase.builder()
                .extid(item.getExtid())
                .customer(item.getCustomer())
                .items(item.getItems())
                .status(item.getStatus())
                .build();
    }

    List<ResponsePurchase> toResponse(List<Purchase> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestPurchaseUpdate request) {
        if (request.getCustomer() == null &&
                request.getItems() == null &&
                request.getStatus() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
