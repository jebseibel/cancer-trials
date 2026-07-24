package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class BaseUniqueDb extends BaseDb {

    @Column(name = "unique_id", length = 64)
    protected String uniqueId;

    @Column(name = "version", length = 16)
    protected String version;
}
