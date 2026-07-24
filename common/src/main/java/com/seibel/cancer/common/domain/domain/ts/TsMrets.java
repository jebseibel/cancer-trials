package com.seibel.cancer.common.domain.domain.ts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TsMrets extends BaseTsDomain {

    // Direct API fields from /v1/public/rec/generators
    private String name;                  // Generator name (API: name)
    private String facilityName;          // API: facility_name
    private String trackingSystemId;      // API: mrets_id
    private String originalRegistryId;    // API: original_registry_id
    private String state;                 // API: state_province
    private String country;               // API: country
    private String county;                // API: county
    private String region;                // API: region
    private String ownershipType;         // API: ownership_type
    private String firstOperation;        // API: commenced_operation_date
    private BigDecimal nameplateCapacity; // API: nameplate_capacity
    private String eiaNumber;             // API: eia_number
    private String wiRrc;                 // API: wi_rrc_unit_id
    private String reportingEntity;       // API: qre
    private String genType;               // API: gen_type (imported/normal)
    private String generatorStatus;       // API: status (active/inactive/imported)
    private Boolean isMultiFuel;          // API: is_multi_fuel

    // From include=owner relationship
    private String owner;                 // API: owner.name (from included contact)

    // Fields that may come from relationships (fuel_sources, eligibilities)
    private String resourceType;          // From fuel_type relationship
    private String fuelSources;           // From fuel_sources relationship
    private String eligibility;           // From eligibilities relationship
}
