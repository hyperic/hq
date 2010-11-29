package org.hyperic.hq.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriTemplate;

@Controller
@RequestMapping("/resources")
public class ResourceController {
	private final static String BASE_URI = "/resources";
	
	@RequestMapping(method = RequestMethod.GET, value = "/root-relationships")
	public @ResponseBody ListOfResourceRelationshipRepresentations getRootResourceRelationships() {
		Resource root = Resource.findResourceByName("Root");
		Long id = root.getId();
		
		String uri = new UriTemplate(BASE_URI + "/{id}/relationships").expand(id).toASCIIString();
		
		return new ListOfResourceRelationshipRepresentations(root.getId(), getRelatedResources(id), uri);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody ResourceRepresentation createResource(@RequestBody ResourceRepresentation resource, HttpServletResponse response) {
		Resource r = resource.toDomain();
		
		createResource(r);

		// TODO Figure out a way to do this, trying to do it one step results in a NPE since the underlying state is not yet set
		/*
		for (Map.Entry<String, Object> p : resource.getProperties().entrySet()) {
			r.setProperty(p.getKey(), p.getValue());
		}

		updateResource(r);
		*/
		
		ResourceRepresentation rr = new ResourceRepresentation(r, BASE_URI);
		
		response.setHeader("Location", rr.getUri());

		return rr;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody ResourceRepresentation updateResource(@PathVariable Long id, @RequestBody ResourceRepresentation resource, HttpServletResponse response) {
		Resource r = resource.toDomain();
		
		r.setId(id);
		updateResource(r);

		ResourceRepresentation rr = new ResourceRepresentation(r, BASE_URI);
		
		response.setHeader("Location", rr.getUri());

		return rr;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody ResourceRepresentation getResource(@PathVariable Long id) {
		return new ResourceRepresentation(getResourceById(id), BASE_URI);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void deleteSingleResource(@PathVariable Long id) {
		deleteResource(id);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody ListOfResourceRepresentations getResources(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, HttpServletResponse response) {
		return new ListOfResourceRepresentations(getResources(page, pageSize), BASE_URI);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships")
	public @ResponseBody ListOfResourceRelationshipRepresentations getResourceRelationships(@PathVariable Long id) {
		String uri = new UriTemplate(BASE_URI + "/{id}/relationships").expand(id).toASCIIString();
		
		return new ListOfResourceRelationshipRepresentations(id, getRelatedResources(id), uri);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{fromId}/relationships/{toId}")
	public @ResponseBody ResourceRelationshipRepresentation updateResourceRelationship(@PathVariable Long fromId, @PathVariable Long toId, @RequestParam String name, HttpServletResponse response) {
		Resource from = Resource.findResource(fromId);
		Resource to = Resource.findResource(toId);
		ResourceRelation relationship = from.relateTo(to, name);
		String uri = new UriTemplate(BASE_URI + "/{fromId}/relationships").expand(from.getId()).toASCIIString();
		ResourceRelationshipRepresentation rrr = new ResourceRelationshipRepresentation(relationship, uri + "/{toId}");
		
		response.setHeader("Location", rrr.getUri());
		
		return rrr;
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{fromId}/relationships/{toId}")
	public @ResponseBody void deleteResourceRelationship(@PathVariable Long fromId, @PathVariable Long toId, @RequestParam String name) {
		Resource from = Resource.findResource(fromId);
		Resource to = Resource.findResource(toId);
		
		from.removeRelationship(to, name);
	}
	
	// Core Functions
	protected void createResource(Resource resource) {
		resource.persist();
	}
	
	protected void updateResource(Resource resource) {
		resource.merge();
	}
	
	protected Resource getResourceById(Long id) {
		return Resource.findResource(id);
	}
	
	protected void deleteResource(Long id) {
		Resource resource = Resource.findResource(id);
		
		resource.remove();
	}
	
	protected List<Resource> getResources(Integer page, Integer pageSize) {
		if (page == null && pageSize == null) {
			return Resource.findAllResources();
		} else {
			int firstResult = page * pageSize;
			int maxResults = firstResult + pageSize;
			
			return Resource.findResourceEntries(firstResult, maxResults);
		}
	}
	
	protected Set<ResourceRelation> getRelatedResources(Long id) {
		Resource r = Resource.findResource(id);
		
		return r.getRelationships();
	}
}
