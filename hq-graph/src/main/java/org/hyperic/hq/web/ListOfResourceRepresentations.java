package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.web.util.UriTemplate;

public class ListOfResourceRepresentations {
	private List<ResourceRepresentation> resources = new ArrayList<ResourceRepresentation>();
	private UriTemplate uri;
	
	public ListOfResourceRepresentations(List<Resource> resources, String baseUri) {
		if (resources != null) {
			for (Resource r : resources) {
				this.resources.add(new ResourceRepresentation(r, "/resources"));
			}
		}
		
		this.uri = new UriTemplate(baseUri);
	}

	public List<ResourceRepresentation> getResources() {
		return resources;
	}
	
	public String getUri() {
		return (uri != null) ? uri.expand().toASCIIString() : null;
	}
}