package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
@RequestMapping("/groups")
public class GroupController {
	private final static String BASE_URI = "/groups";
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody GroupRepresentation createGroup(@RequestBody GroupRepresentation group, HttpServletResponse response) {
		ResourceGroup g = group.toDomain();
		
		createGroup(g);

		GroupRepresentation gr = new GroupRepresentation(g, BASE_URI);
		
		response.setHeader("Location", gr.getUri());

		return gr;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody GroupRepresentation updateGroup(@PathVariable Long id, @RequestBody GroupRepresentation group, HttpServletResponse response) {
		ResourceGroup r = group.toDomain();
		
		r.setId(id);
		updateGroup(r);

		GroupRepresentation rr = new GroupRepresentation(r, BASE_URI);
		
		response.setHeader("Location", rr.getUri());

		return rr;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody GroupRepresentation getGroup(@PathVariable Long id) {
		return new GroupRepresentation(getGroupById(id), BASE_URI);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void deleteSingleGroup(@PathVariable Long id) {
		deleteGroup(id);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody ListOfGroupRepresentations getGroups(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, HttpServletResponse response) {
		return new ListOfGroupRepresentations(getGroups(page, pageSize), BASE_URI);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/members")
	public @ResponseBody ListOfResourceRepresentations getMembersById(@PathVariable Long id) {
		String uri = new UriTemplate(BASE_URI + "/{id}/members").expand(id).toASCIIString();
		
		return new ListOfResourceRepresentations(getMembers(id), uri);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/members/{memberId}")
	public @ResponseBody GroupRepresentation updateResourceRelationship(@PathVariable Long id, @PathVariable Long memberId) {
		ResourceGroup group = ResourceGroup.findResourceGroup(id);		
		Resource resource = Resource.findResource(memberId);
		
		group.addMember(resource);
		group.merge();
		
		return new GroupRepresentation(group, BASE_URI);
	}

	// Core Functions
	protected void createGroup(ResourceGroup group) {
		group.persist();
	}
	
	protected void updateGroup(ResourceGroup group) {
		group.merge();
	}
	
	protected ResourceGroup getGroupById(Long id) {
		return ResourceGroup.findResourceGroup(id);
	}
	
	protected void deleteGroup(Long id) {
		ResourceGroup group = ResourceGroup.findResourceGroup(id);
		
		group.remove();
	}
	
	protected List<ResourceGroup> getGroups(Integer page, Integer pageSize) {
		if (page == null && pageSize == null) {
			return ResourceGroup.findAllResourceGroups();
		} else {
			int firstResult = page * pageSize;
			int maxResults = firstResult + pageSize;
			
			return ResourceGroup.findResourceGroupEntries(firstResult, maxResults);
		}
	}
	
	protected List<Resource> getMembers(Long id) {
		ResourceGroup group = ResourceGroup.findResourceGroup(id);
		
		return new ArrayList<Resource>(group.getMembers());
	}
}