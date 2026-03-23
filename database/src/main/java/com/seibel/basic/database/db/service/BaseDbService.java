package com.seibel.basic.database.db.service;

import com.seibel.basic.common.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseDbService {

    private final String entityName;

    protected BaseDbService(String entityName) {
        this.entityName = entityName;
    }

    protected String getCreatedMessage(String extid) {
        return String.format("%s with extid=%s created successfully", entityName, extid);
    }

    protected String getUpdatedMessage(String extid) {
        return String.format("%s with extid=%s updated successfully", entityName, extid);
    }

    protected String getDeletedMessage(String extid) {
        return String.format("%s with extid=%s deleted successfully", entityName, extid);
    }

    protected String getFoundMessage(String extid) {
        return String.format("%s with extid=%s found successfully", entityName, extid);
    }

    protected String getFoundMessageByType(String type, int count) {
        return String.format("%s records (%s) found: %d", entityName, type, count);
    }

    protected String getFoundFailureMessage(String extid) {
        return String.format("%s with extid=%s not found", entityName, extid);
    }

    protected String getOperationFailureMessage(String operation, String extid) {
        return String.format("%s operation failed for %s with extid=%s",
                capitalize(operation), entityName, extid);
    }

    /**
     * Handles exceptions from repository calls and maps them to your standardized exceptions.
     * @param operation The CRUD operation name ("create", "update", "delete", "retrieve")
     * @param extid The entity external ID (optional for bulk ops)
     * @param e The original exception
     * @throws ServiceException for create/update/delete
     * @throws ServiceException for retrieve
     */
    protected void handleException(String operation, String extid, Exception e) {

        String message = getOperationFailureMessage(operation, extid);
        log.error("{} - cause: {}", message, e.getMessage(), e);
        throw new ServiceException(message, e);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
