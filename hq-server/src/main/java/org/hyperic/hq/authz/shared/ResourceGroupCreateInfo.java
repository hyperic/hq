package org.hyperic.hq.authz.shared;

public class ResourceGroupCreateInfo {

    private String name;

    private String location;

    private boolean privateGroup;

    private String description;

    public ResourceGroupCreateInfo(String name, String description, String location,
                                   boolean privateGroup) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.privateGroup = privateGroup;
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

}
