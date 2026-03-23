package com.seibel.basic.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "purchase")
public class PurchaseDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456789L;

    @Column(name = "customer", length = 50, nullable = false, unique = true)
    private String customer;

    @Column(name = "item", length = 255, nullable = false)
    private String items;

    @Column(name = "status", length = 255, nullable = false)
    private String status;
}
