package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "location")
public class LocationDb extends BaseDb {

    private static final long serialVersionUID = 4517283960134872591L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "facility", length = 255)
    private String facility;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "state", length = 128)
    private String state;

    @Column(name = "zip", length = 16)
    private String zip;

    @Column(name = "country", length = 128)
    private String country;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;
}
