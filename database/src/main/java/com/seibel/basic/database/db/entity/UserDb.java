package com.seibel.basic.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user")
public class UserDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456789L;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "email", length = 100, nullable = true)
    private String email;

    @Column(name = "role", length = 20, nullable = false)
    private String role = "USER";
}
