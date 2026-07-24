package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "customer")
public class CustomerDb extends BaseDb {

    private static final long serialVersionUID = 330515747211210728L;

    @Column(name = "code", length = 8, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 120, nullable = false, unique = true)
    private String name;

    @Column(name = "contact_name", length = 255, nullable = false)
    private String contactName;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "contact_email", length = 255, nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 255, nullable = false)
    private String contactPhone;
}
