package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;

public class OperationType implements IdentityAware, PersistenceAware<OperationType> {
    private Integer id;
    private String name;
    private ResourceType resourceType;
    private Integer version;

    public OperationType() {
    }

    public void flush() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Integer getVersion() {
        return this.version;
    }

    public OperationType merge() {
    	return this;
    }

    public void persist() {
    }

    public void remove() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("ResourceType: ").append(getResourceType());
        return sb.toString();
    }

    public static int count() {
    	return 0;
    }

    public static List<OperationType> findAllOperationTypes() {
    	return new ArrayList<OperationType>();
    }

    public static OperationType findById(Long id) {
    	return new OperationType();
    }

    public static List<OperationType> find(Integer firstResult, Integer maxResults) {
    	return new ArrayList<OperationType>();
    }

    public static List<Integer> findOperableResourceIds(final AuthzSubject subj,
                                                 final String resourceTable,
                                                 final String resourceColumn, final String resType,
                                                 final String operation, final String addCond) {
        //TODO Extracted from OperationDAO
        return null;
    }
    
    public static List<OperationType> findAllOrderByName() {
        //TODO implement sorting somewhere
        return null;
    }
}