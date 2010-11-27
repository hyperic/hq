package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.plugin.domain.ResourceTypeRelation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriTemplate;

@Controller
@RequestMapping("/resourcetypes")
public class ResourceTypeController {
	private final static String BASE_URI = "/resourcetypes";
	
	@RequestMapping(method = RequestMethod.GET, value = "/root-relationships")
	public @ResponseBody ListOfResourceTypeRelationshipRepresentations getRootResourceTypeRelationships() {
		ResourceType root = ResourceType.findResourceTypeByName("System");
		Long id = root.getId();
		
		String uri = new UriTemplate(BASE_URI + "/{id}/relationships").expand(id).toASCIIString();
		
		return new ListOfResourceTypeRelationshipRepresentations(root.getId(), getRelatedResourceTypes(id), uri);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody ResourceTypeRepresentation createResourceType(@RequestBody ResourceTypeRepresentation resourceType, HttpServletResponse response) {
		ResourceType rt = resourceType.toDomain();
		
		createResourceType(rt);

		ResourceTypeRepresentation rtp = new ResourceTypeRepresentation(rt, BASE_URI);
		
		response.setHeader("Location", rtp.getUri());

		return rtp;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody ResourceTypeRepresentation updateResourceType(@PathVariable Long id, @RequestBody ResourceTypeRepresentation resourceType, HttpServletResponse response) {
		ResourceType rt = resourceType.toDomain();
		
		rt.setId(id);
		updateResourceType(rt);

		ResourceTypeRepresentation rtp = new ResourceTypeRepresentation(rt, BASE_URI);
		
		response.setHeader("Location", rtp.getUri());

		return rtp;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody ResourceTypeRepresentation getResourceType(@PathVariable Long id) {
		return new ResourceTypeRepresentation(getResourceTypeById(id), BASE_URI);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void deleteSingleResourceType(@PathVariable Long id) {
		deleteResourceType(id);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody ListOfResourceTypeRepresentations getResourceTypes(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, HttpServletResponse response) {
		return new ListOfResourceTypeRepresentations(getResourceTypes(page, pageSize), BASE_URI);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships")
	public @ResponseBody ListOfResourceTypeRelationshipRepresentations getResourceTypeRelationships(@PathVariable Long id) {
		String uri = new UriTemplate(BASE_URI + "/{id}/relationships").expand(id).toASCIIString();
		
		return new ListOfResourceTypeRelationshipRepresentations(id, getRelatedResourceTypes(id), uri);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{fromId}/relationships/{toId}")
	public @ResponseBody ResourceTypeRelationshipRepresentation updateResourceTypeRelationship(@PathVariable Long fromId, @PathVariable Long toId, @RequestParam String name, HttpServletResponse response) {
		ResourceType from = ResourceType.findResourceType(fromId);
		ResourceType to = ResourceType.findResourceType(toId);
		ResourceTypeRelation relationship = from.relateTo(to, name);
		ResourceTypeRelationshipRepresentation rtrr = new ResourceTypeRelationshipRepresentation(relationship, "/" + fromId + "/relationships/{toId}");
		
		response.setHeader("Location", rtrr.getUri());
		
		return rtrr;
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{fromId}/relationships/{toId}")
	public @ResponseBody void deleteResourceTypeRelationship(@PathVariable Long fromId, @PathVariable Long toId, @RequestParam String name) {
		ResourceType from = ResourceType.findResourceType(fromId);
		ResourceType to = ResourceType.findResourceType(toId);
		
		from.removeRelationship(to, name);
	}
	
	// Core Functions
	protected void createResourceType(ResourceType resourceType) {
		resourceType.persist();
	}
	
	protected void updateResourceType(ResourceType resourceType) {
		resourceType.merge();
	}
	
	protected ResourceType getResourceTypeById(Long id) {
		return ResourceType.findResourceType(id);
	}
	
	protected void deleteResourceType(Long id) {
		ResourceType resourceType = ResourceType.findResourceType(id);
		
		resourceType.remove();
	}
	
	protected List<ResourceType> getResourceTypes(Integer page, Integer pageSize) {
		if (page == null && pageSize == null) {
			return ResourceType.findAllResourceTypes();
		} else {
			int firstResult = page * pageSize;
			int maxResults = firstResult + pageSize;
			
			return ResourceType.findResourceTypeEntries(firstResult, maxResults);
		}
	}
	
	protected List<ResourceTypeRelation> getRelatedResourceTypes(Long id) {
		ResourceType resourceType = ResourceType.findResourceType(id);
		
		return new ArrayList<ResourceTypeRelation>(resourceType.getRelationships());
	}
}