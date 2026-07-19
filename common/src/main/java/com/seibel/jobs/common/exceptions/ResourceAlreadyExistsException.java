package com.seibel.jobs.common.exceptions;

public class ResourceAlreadyExistsException extends BaseServiceException {

    private final String resourceType;
    private final String identifier;

    public ResourceAlreadyExistsException(String resourceType, String identifier) {
        super(String.format("%s already exists with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}