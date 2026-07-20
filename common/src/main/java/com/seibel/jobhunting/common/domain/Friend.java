package com.seibel.jobhunting.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Friend extends BaseDomain {
    private String name;
    private String relationship;
    private String email;
    private String phone;
    private String linkedinUrl;
    private LocalDate lastContactedAt;
    private String notes;
}
