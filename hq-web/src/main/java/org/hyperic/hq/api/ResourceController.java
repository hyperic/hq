package org.hyperic.hq.api;

import java.util.List;
import java.util.Set;

import org.hyperic.hq.api.form.ListSettings;
import org.hyperic.hq.api.form.ResourceForm;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.ResourceRep;
import org.hyperic.hq.api.representation.SuccessResponse;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentDAO;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectDAO;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/api/resources")
public class ResourceController extends BaseController {
	private ResourceDao resourceDao;
	private ResourceTypeDao resourceTypeDao;
	private AgentDAO agentDao;
	private AuthzSubjectDAO authzSubjectDao;
	
	@Autowired
	public ResourceController(ResourceDao resourceDao, ResourceTypeDao resourceTypeDao, AgentDAO agentDao, AuthzSubjectDAO authzSubjectDao) {
		this.resourceDao = resourceDao;
		this.resourceTypeDao = resourceTypeDao;
		this.agentDao = agentDao;
		this.authzSubjectDao = authzSubjectDao;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody SuccessResponse list(ListSettings listSettings) throws Exception {
		List<Resource> resources = resourceDao.find(listSettings.getStartIndex(), listSettings.getEndIndex());
		
		return new SuccessResponse(ListRep.createListRepFromResources(resources));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody SuccessResponse create(@RequestBody ResourceForm form) throws Exception {
		Resource resource = translateFormToDomain(form);

		resource.persist();
		
		return new SuccessResponse(new ResourceRep(resource));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody SuccessResponse getById(@PathVariable Integer id) throws Exception {
		Resource entity = resourceDao.findById(id);
		
		return new SuccessResponse(new ResourceRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody SuccessResponse update(@PathVariable Integer id, @RequestBody ResourceForm form) throws Exception {
		Resource resource = translateFormToDomain(form);

		resource.merge();
		
		return new SuccessResponse(new ResourceRep(resource));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void delete(@PathVariable Integer id) {
		Resource entity = resourceDao.findById(id);
		
		entity.remove();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships/{name}")
	public @ResponseBody SuccessResponse listNamedRelationships(@PathVariable Integer id, @PathVariable String name) throws Exception {
		Resource entity = resourceDao.findById(id);
		Set<Resource> resources = entity.getResourcesFrom(name);
		
		return new SuccessResponse(ListRep.createListRepFromResources(resources));
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{id}/relationships/{name}")
	public @ResponseBody void createNamedRelationshipTo(@PathVariable Integer id, @PathVariable String name, @RequestParam Integer toId) {
		Resource entity = resourceDao.findById(id);
		Resource otherEntity = resourceDao.findById(toId);
		
		entity.relateTo(otherEntity, name);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/relationships/{name}")
	public @ResponseBody void deleteAllNamedRelationships(@PathVariable Integer id, @PathVariable String name) {
		Resource entity = resourceDao.findById(id);
		
		entity.removeRelationships(name);
	}
	
	private Resource translateFormToDomain(ResourceForm form) {
		Resource resource;
		
		if (form.getId() == null) {
			resource = new Resource();
		} else {
			resource = resourceDao.findById(form.getId());
		}
		
		ResourceType type = resourceTypeDao.findById(form.getResourceTypeId());
		Agent agent = agentDao.findById(form.getAgentId());
		
		resource.setAgent(agent);
		resource.setType(type);
		resource.setName(form.getName());
		resource.setDescription(form.getDescription());
		resource.setLocation(form.getLocation());
		
		// TODO Owner and modifiedby can be set based on the authenticated user, for hardcoding to HQAdmin...
		AuthzSubject subject = authzSubjectDao.findById(1);
		
		resource.setOwner(subject);
		resource.setModifiedBy(subject.getName());

		return resource;
	}
}