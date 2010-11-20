package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.web.util.UriTemplate;

public class ListOfGroupRepresentations {
	private List<GroupRepresentation> groups = new ArrayList<GroupRepresentation>();
	private UriTemplate uri;
	
	public ListOfGroupRepresentations(List<ResourceGroup> groups, String baseUri) {
		if (groups != null) {
			for (ResourceGroup g : groups) {
				this.groups.add(new GroupRepresentation(g, "/groups"));
			}
		}
		
		this.uri = new UriTemplate(baseUri);
	}

	public List<GroupRepresentation> getGroups() {
		return groups;
	}
	
	public String getUri() {
		return (uri != null) ? uri.expand().toASCIIString() : null;
	}
}