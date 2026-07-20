package com.seibel.jobhunting.web.controller;

import com.seibel.jobhunting.common.domain.Customer;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ValidationException;
import com.seibel.jobhunting.service.CustomerService;
import com.seibel.jobhunting.web.request.RequestCustomerCreate;
import com.seibel.jobhunting.web.request.RequestCustomerUpdate;
import com.seibel.jobhunting.web.response.ResponseCustomer;
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
@RequestMapping("/api/customer")
@Validated
@Tag(name = "Customer", description = "Customer CRUD endpoints")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerConverter converter = new CustomerConverter();

    @GetMapping
    @Operation(summary = "List customers (paginated)")
    public Page<ResponseCustomer> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) ActiveEnum active
    ) {
        return customerService.findAll(pageable, active).map(converter::toResponse);
    }

    @GetMapping("/{extid}")
    @Operation(summary = "Get customer by extid")
    public ResponseCustomer getByExtid(@PathVariable String extid) {
        return converter.toResponse(customerService.findByExtid(extid));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    public ResponseEntity<ResponseCustomer> create(@Valid @RequestBody RequestCustomerCreate request) {
        Customer created = customerService.create(converter.toDomain(request));
        URI location = URI.create("/api/customer/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    @Operation(summary = "Update customer (full or partial)")
    public ResponseCustomer update(@PathVariable String extid, @Valid @RequestBody RequestCustomerUpdate request) {
        converter.validateUpdateRequest(request);
        Customer updated = customerService.update(extid, converter.toDomain(request));
        return converter.toResponse(updated);
    }

    @PatchMapping("/{extid}")
    @Operation(summary = "Patch customer (partial update)")
    public ResponseCustomer patch(@PathVariable String extid, @Valid @RequestBody RequestCustomerUpdate request) {
        return update(extid, request);
    }

    @DeleteMapping("/{extid}")
    @Operation(summary = "Delete customer (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        boolean deleted = customerService.delete(extid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

class CustomerConverter {

    Customer toDomain(RequestCustomerCreate request) {
        return Customer.builder()
                .code(request.getCode())
                .name(request.getName())
                .contactName(request.getContactName())
                .description(request.getDescription())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
    }

    Customer toDomain(RequestCustomerUpdate request) {
        return Customer.builder()
                .code(request.getCode())
                .name(request.getName())
                .contactName(request.getContactName())
                .description(request.getDescription())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
    }

    ResponseCustomer toResponse(Customer item) {
        return ResponseCustomer.builder()
                .extid(item.getExtid())
                .code(item.getCode())
                .name(item.getName())
                .contactName(item.getContactName())
                .description(item.getDescription())
                .contactEmail(item.getContactEmail())
                .contactPhone(item.getContactPhone())
                .build();
    }

    List<ResponseCustomer> toResponse(List<Customer> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestCustomerUpdate request) {
        if (request.getCode() == null &&
                request.getName() == null &&
                request.getContactName() == null &&
                request.getDescription() == null &&
                request.getContactEmail() == null &&
                request.getContactPhone() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
