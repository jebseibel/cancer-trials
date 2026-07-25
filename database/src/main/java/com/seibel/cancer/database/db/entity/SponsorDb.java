package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sponsor")
public class SponsorDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456793L;

    @Column(name = "name", length = 255, nullable = false, unique = true)
    private String name;

    @Column(name = "org_class", length = 32)
    private String orgClass;
}
