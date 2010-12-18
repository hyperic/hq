package org.hyperic.hq.api.representation;

import java.util.Map;

import org.hyperic.hq.api.LinkHelper;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;

public class ResourceGroupRep extends ResourceRep {
	private boolean privateGroup;
	
	public ResourceGroupRep() {
		super();
	}
	
	public ResourceGroupRep(ResourceGroup group) {
		super((Resource) group);
		
		setId(group.getId());
		
		privateGroup = group.isPrivateGroup();
		
		// Override the self and relationship links...
		Map<String, String> links = getLinks();
		
		links.put(RELATIONSHIPS_LABEL, LinkHelper.getCollectionUri(RESOURCE_GROUPS_LABEL, getId(), RELATIONSHIPS_LABEL));
		links.put(MEMBERS_LABEL, LinkHelper.getCollectionUri(RESOURCE_GROUPS_LABEL, getId(), MEMBERS_LABEL));
		links.put(ROLES_LABEL, LinkHelper.getCollectionUri(RESOURCE_GROUPS_LABEL, getId(), ROLES_LABEL));
	}
	
	@Override
	public void setId(Integer id) {
		super.setId(id);
		
		// Update with resource group specific self link...
		getLinks().put(SELF_LABEL, LinkHelper.getInstanceByIdUri(RESOURCE_GROUPS_LABEL, getId()));
	}

	public boolean isPrivateGroup() {
		return privateGroup;
	}

	public void setPrivateGroup(boolean privateGroup) {
		this.privateGroup = privateGroup;
	}

	public Map<String, String> getLinks() {
		return getLinks();
	}
}