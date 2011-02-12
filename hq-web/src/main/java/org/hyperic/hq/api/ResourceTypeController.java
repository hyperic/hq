package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.api.form.ListSettings;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.OperationTypeRep;
import org.hyperic.hq.api.representation.PropertyTypeRep;
import org.hyperic.hq.api.representation.ResourceTypeRep;
import org.hyperic.hq.api.representation.SuccessResponse;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.inventory.domain.ResourceTypeRelationship;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/root")
	public @ResponseBody SuccessResponse getRoot() throws Exception {
		ResourceType entity = resourceTypeDao.findRoot();
		
		if (entity == null) throw new NotFoundException("Root entity not found");
		
		return new SuccessResponse(new ResourceTypeRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody SuccessResponse list(ListSettings listSettings) throws Exception {
		List<ResourceType> result = resourceTypeDao.find(listSettings.getStartIndex(), listSettings.getEndIndex());
		
		return new SuccessResponse(ListRep.createListRepFromResourceTypes(result));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody SuccessResponse create(@RequestBody ResourceTypeRep form) throws Exception {
		ResourceType type = translateFormToDomain(form);
		
	
		return new SuccessResponse(new ResourceTypeRep(type));
	}
	
	// TODO Find a way to make this case insensitive...
	// TODO I'd rather this map to /{name} but it conflicts w/ the /{id} mapping.  Figure out a way to do this...
	@RequestMapping(method = RequestMethod.GET, value = "/name:{name}")
	public @ResponseBody SuccessResponse getById(@PathVariable String name) throws Exception {
		ResourceType entity = resourceTypeDao.findByName(name);

		if (entity == null) throw new NotFoundException("No entity not found with name [" + name + "]");
		
		return new SuccessResponse(new ResourceTypeRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public @ResponseBody SuccessResponse getById(@PathVariable Integer id) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);

		if (entity == null) throw new NotFoundException("No entity not found with id [" + id + "]");
		
		return new SuccessResponse(new ResourceTypeRep(entity));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public @ResponseBody SuccessResponse update(@PathVariable Integer id, @RequestBody ResourceTypeRep form) throws Exception {
		ResourceType type = translateFormToDomain(form);

		resourceTypeDao.merge(type);
		
		return new SuccessResponse(new ResourceTypeRep(type));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public @ResponseBody void delete(@PathVariable Integer id) {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity != null) {
			entity.remove();
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships")
	public @ResponseBody SuccessResponse listRelationships(@PathVariable Integer id) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity == null) throw new NotFoundException("No entity not found with id [" + id + "]");
		
		Set<ResourceTypeRelationship> relationships = entity.getRelationships();
		Map<String, List<ResourceTypeRep>> result = new HashMap<String, List<ResourceTypeRep>>();
		
		for (ResourceTypeRelationship relation : relationships) {
			String name = relation.getName();
			List<ResourceTypeRep> rts;
			
			if (result.containsKey(name)) {
				rts = result.get(name);
			} else {
				rts = new ArrayList<ResourceTypeRep>();
			}
			
			ResourceTypeRep rep = null;
			
			if (relation.getTo().getId().equals(id)) {
				rep = new ResourceTypeRep(relation.getFrom());
			} else if (relation.getFrom().getId().equals(id)) {
				rep = new ResourceTypeRep(relation.getTo());
			}
			
			if (rep != null) {
				rts.add(rep);
			}
		}
		
		return new SuccessResponse(result);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/relationships/{name}")
	public @ResponseBody SuccessResponse listNamedRelationships(@PathVariable Integer id, @PathVariable String name) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity == null) throw new NotFoundException("No entity not found with id [" + id + "]");
		
		Set<ResourceType> result = entity.getResourceTypesTo(name);
		
		return new SuccessResponse(ListRep.createListRepFromResourceTypes(result));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/relationships/{name}/{toId}")
	public @ResponseBody void createNamedRelationships(@PathVariable Integer id, @PathVariable String name, @PathVariable Integer toId) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity == null) throw new NotFoundException("No entity not found with id [" + id + "]");
		
		ResourceType otherEntity = resourceTypeDao.findById(toId);
		
		if (otherEntity == null) throw new NotFoundException("No (other)entity not found with id [" + toId + "]");
		
		entity.relateTo(otherEntity, name);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/relationships/{name}/{toId}")
	public @ResponseBody void deleteNamedRelationships(@PathVariable Integer id, @PathVariable String name, @PathVariable Integer toId) throws Exception {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity == null) throw new NotFoundException("No entity not found with id [" + id + "]");
		
		ResourceType otherEntity = resourceTypeDao.findById(toId);
		
		if (otherEntity == null) throw new NotFoundException("No (other)entity not found with id [" + toId + "]");
		
		entity.removeRelationship(otherEntity, name);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/relationships/{name}")
	public @ResponseBody void deleteAllNamedRelationships(@PathVariable Integer id, @PathVariable String name) {
		ResourceType entity = resourceTypeDao.findById(id);
		
		if (entity != null) {
			entity.removeRelationships(name);
		}
	}
	
	private ResourceType translateFormToDomain(ResourceTypeRep form) {
		ResourceType type;
		
		if (form.getId() == null) {
			type = new ResourceType(form.getName());
			resourceTypeDao.persist(type);
		} else {
			type = resourceTypeDao.findById(form.getId());
		}
		
		type.setName(form.getName());
		type.setDescription(form.getDescription());
		
		Plugin plugin = pluginDao.findByName(form.getPluginName());
		
		type.setPlugin(plugin);
		
		if (form.getOperationTypes() != null) {
			
			for (OperationTypeRep opRep : form.getOperationTypes()) {
				OperationType opType = new OperationType(opRep.getName());
				type.addOperationType(opType);
			}
			
		}
		
		if (form.getPropertyTypes() != null) {
			for (PropertyTypeRep propRep : form.getPropertyTypes()) {
				PropertyType propType = new PropertyType(propRep.getName(),String.class);
				
				propType.setDefaultValue(propRep.getDefaultValue());
				propType.setDescription(propRep.getDescription());
				propType.setHidden(propRep.isHidden());
				propType.setOptional(propRep.isOptional());
				propType.setSecret(propRep.isSecret());
				propType.setResourceType(type);
				type.addPropertyType(propType);
			}
		}
		
		return type;
	}
}
