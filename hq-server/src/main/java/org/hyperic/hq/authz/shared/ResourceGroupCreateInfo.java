package org.hyperic.hq.authz.shared;

public class ResourceGroupCreateInfo {

    private String name;

    private String location;

    private boolean privateGroup;

    private String description;
    
    private int groupTypeId;
    
    private int groupEntResType=-1;
    
    private int groupEntType=-1;

    public ResourceGroupCreateInfo(String name, String description, String location,
                                   boolean privateGroup, int groupTypeId) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.privateGroup = privateGroup;
        this.groupTypeId = groupTypeId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isPrivateGroup() {
        return privateGroup;
    }

    public String getDescription() {
        return description;
    }

    public int getGroupTypeId() {
        return groupTypeId;
    }

    public int getGroupEntResType() {
        return groupEntResType;
    }

    public void setGroupEntResType(int groupEntResType) {
        this.groupEntResType = groupEntResType;
    }

    public int getGroupEntType() {
        return groupEntType;
    }

    public void setGroupEntType(int groupEntType) {
        this.groupEntType = groupEntType;
    }
    
}
