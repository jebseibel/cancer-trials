package com.seibel.jobhunting.testutils;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.BaseDb;

import java.time.LocalDateTime;

/**
 * Shared base class for all DomainBuilder* utilities.
 * Adds consistent initialization for BaseDb and BaseCsvDb entities.
 */
public abstract class DomainBuilderBase extends DomainBuilderUtils {

    protected static void setBaseSyncFields(BaseDb item) {
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setActive(ActiveEnum.ACTIVE);
        item.setDeletedAt(null);
    }
}
