package org.hyperic.hq.api;

import java.util.List;
import java.util.Set;

import org.hyperic.hq.api.form.ListSettings;
import org.hyperic.hq.api.form.ResourceTypeForm;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.ResourceTypeRep;
import org.hyperic.hq.api.representation.SuccessResponse;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
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
@RequestMapping("/api/resource-types")
public class ResourceTypeController extends BaseController {
	private ResourceTypeDao resourceTypeDao;
	private PluginDAO pluginDao;
	
	@Autowired
	public ResourceTypeController(ResourceTypeDao resourceTypeDao, PluginDAO pluginDao) {
		this.resourceTypeDao = resourceTypeDao;
		this.pluginDao = pluginDao;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody SuccessResponse list(ListSettings listSettings) throws Exception {
		List<ResourceType> result = resourceTypeDao.find(listSettings.getStartIndex(), listSettings.getEndIndex());
		
		return new SuccessResponse(ListRep.createListRepFromResourceTypes(result));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody SuccessResponse create(@RequestBody ResourceTypeForm form) throws Exception {
		ResourceType type = translateFormToDomain(form);
		
		return new SuccessResponse(new ResourceTypeRep(type));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody SuccessResponse getById(@PathVariable Integer id) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);

		return new SuccessResponse(new ResourceTypeRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody SuccessResponse update(@PathVariable Integer id, @RequestBody ResourceTypeForm form) throws Exception {
		ResourceType type = translateFormToDomain(form);

		type.merge();
		
		return new SuccessResponse(new ResourceTypeRep(type));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void delete(@PathVariable Integer id) {
		ResourceType entity = resourceTypeDao.findById(id);
		
		entity.remove();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships/{name}")
	public @ResponseBody SuccessResponse listNamedRelationships(@PathVariable Integer id, @PathVariable String name) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);
		Set<ResourceType> result = entity.getResourceTypesFrom(name);
		
		return new SuccessResponse(ListRep.createListRepFromResourceTypes(result));
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{id}/relationships/{name}")
	public @ResponseBody void deleteAllNamedRelationships(@PathVariable Integer id, @PathVariable String name, @RequestParam Integer toId) {
		ResourceType entity = resourceTypeDao.findById(id);
		ResourceType otherEntity = resourceTypeDao.findById(toId);
		
		entity.relateTo(otherEntity, name);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/relationships/{name}")
	public @ResponseBody void deleteAllNamedRelationships(@PathVariable Integer id, @PathVariable String name) {
		ResourceType entity = resourceTypeDao.findById(id);
		
		entity.removeRelationships(name);
	}
	
	private ResourceType translateFormToDomain(ResourceTypeForm form) {
		ResourceType type;
		
		if (form.getId() == null) {
		    Plugin plugin = pluginDao.findByName(form.getPluginName());
			type =  resourceTypeDao.create(form.getName(), plugin);
		} else {
			type = resourceTypeDao.findById(form.getId());
		}
		type.setDescription(form.getDescription());
		
		return type;
	}
}