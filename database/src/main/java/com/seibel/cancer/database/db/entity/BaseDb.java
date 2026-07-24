package com.seibel.cancer.database.db.entity;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.converter.ActiveEnumConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
public abstract class BaseDb implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    protected Long id;

    @Column(name = "extid", length = 36, nullable = false, unique = true)
    protected String extid;

    @Column(name = "created_at", nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @Column(name = "active")
    @Convert(converter = ActiveEnumConverter.class)
    protected ActiveEnum active;
}
