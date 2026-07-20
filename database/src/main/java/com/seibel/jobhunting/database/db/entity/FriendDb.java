package com.seibel.jobhunting.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "friend")
public class FriendDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "relationship", length = 255)
    private String relationship;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "linkedin_url", length = 1024)
    private String linkedinUrl;

    @Column(name = "last_contacted_at")
    private LocalDate lastContactedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
