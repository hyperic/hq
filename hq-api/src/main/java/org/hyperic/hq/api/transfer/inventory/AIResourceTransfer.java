package org.hyperic.hq.api.transfer.inventory;

import java.util.Collection;

import org.hyperic.hq.api.transfer.inventory.model.AIResource;


public interface AIResourceTransfer {
    public Collection<AIResource> getAIResources();
    
    public Collection<AIResource> approveAIResources();

    public Collection<AIResource> approveAIResources(String regex);

    public Collection<AIResource> getAIResources(Collection <String> ids);

    public Collection<AIResource> getAIResource(String id);

    public Collection<AIResource> approveAIResource(String id, String regex);

}
