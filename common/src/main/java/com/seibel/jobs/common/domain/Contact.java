package com.seibel.jobs.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Contact extends BaseDomain {
    private Long companyId;
    private Long jobPostingId;
    private String name;
    private String role;
    private String email;
    private String phone;
    private String notes;
}
