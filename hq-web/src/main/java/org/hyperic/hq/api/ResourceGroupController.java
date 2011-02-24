package org.hyperic.hq.api;

import java.util.List;
import java.util.Set;

import org.hyperic.hq.api.form.ListSettings;
import org.hyperic.hq.api.form.ResourceGroupForm;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.ResourceGroupRep;
import org.hyperic.hq.api.representation.SuccessResponse;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectDAO;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceGroupDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
@RequestMapping("/api/resource-groups")
public class ResourceGroupController extends BaseController {
	private ResourceDao resourceDao;
	private ResourceGroupDao resourceGroupDao;
	private ResourceTypeDao resourceTypeDao;
	private AuthzSubjectDAO authzSubjectDao;
		
	@Autowired
	public ResourceGroupController(ResourceDao resourceDao, ResourceGroupDao resourceGroupDao, ResourceTypeDao resourceTypeDao, AuthzSubjectDAO authzSubjectDao) {
		this.resourceDao = resourceDao;
		this.resourceGroupDao = resourceGroupDao;
		this.resourceTypeDao = resourceTypeDao;
		this.authzSubjectDao = authzSubjectDao;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody SuccessResponse list(ListSettings listSettings) throws Exception {
		List<ResourceGroup> result = resourceGroupDao.find(listSettings.getStartIndex(), listSettings.getEndIndex());
		
		return new SuccessResponse(ListRep.createListRepFromResourceGroups(result));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody SuccessResponse create(@RequestBody ResourceGroupForm form) throws Exception {
		ResourceGroup group = translateFormToDomain(form);
		
		return new SuccessResponse(new ResourceGroupRep(group));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody SuccessResponse getById(@PathVariable Integer id) throws Exception {
		ResourceGroup entity = resourceGroupDao.findById(id);
		
		return new SuccessResponse(new ResourceGroupRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody SuccessResponse update(@PathVariable Integer id, @RequestBody ResourceGroupForm form) throws Exception {
		ResourceGroup group = translateFormToDomain(form);
		
		resourceGroupDao.merge(group);
		
		return new SuccessResponse(new ResourceGroupRep(group));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void delete(@PathVariable Integer id) {
		ResourceGroup entity = resourceGroupDao.findById(id);
		
		entity.remove();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/members")
	public @ResponseBody SuccessResponse listNamedRelationships(@PathVariable Integer id) throws Exception {
		ResourceGroup entity = resourceGroupDao.findById(id);
		Set<Resource> result = entity.getMembers();
		
		return new SuccessResponse(ListRep.createListRepFromResources(result));
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{id}/members")
	public @ResponseBody void createNamedRelationshipTo(@PathVariable Integer id, @RequestParam Integer memberId) {
		ResourceGroup entity = resourceGroupDao.findById(id);
		Resource member = resourceDao.findById(memberId);
		
		entity.addMember(member);
		resourceGroupDao.merge(entity);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/members/{memberId}")
	public @ResponseBody void deleteAllNamedRelationships(@PathVariable Integer id, @PathVariable String name, @PathVariable Integer memberId) {
		ResourceGroup entity = resourceGroupDao.findById(id);
		Resource member = resourceDao.findById(memberId);
		
		entity.removeMember(member);
		resourceGroupDao.merge(entity);
	}
	
	private ResourceGroup translateFormToDomain(ResourceGroupForm form) {
		ResourceGroup group;
		
		if (form.getId() == null) {
		    ResourceType type = resourceTypeDao.findById(form.getResourceTypeId());
			group = new ResourceGroup(form.getName(), type,form.isPrivateGroup());
			resourceGroupDao.persist(group);
		} else {
			group = resourceGroupDao.findById(form.getId());
		}
	
		group.setName(form.getName());
		group.setDescription(form.getDescription());
		group.setLocation(form.getLocation());
		
		// TODO Owner and modifiedby can be set based on the authenticated user, for hardcoding to HQAdmin...
		AuthzSubject subject = authzSubjectDao.findById(1);
		
		group.setOwner(subject);
		group.setModifiedBy(subject.getName());

		return group;
	}
}