package org.hyperic.hq.api;

import java.util.List;

import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/api/resources")
public class ResourceController extends BaseController {
	private final static String DOMAIN_NAME = "resources";

	private ResourceDao resourceDao;
	
	@Autowired
	public ResourceController(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Representation<Resource> list(ListSettings listSettings) throws Exception {
		List<Resource> result = resourceDao.find(listSettings.getStartIndex(), listSettings.getEndIndex());
		
		return new Representation<Resource>(result, DOMAIN_NAME);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public Representation<Resource> create(Resource entity) throws Exception {
		entity.persist();
		
		return new Representation<Resource>(entity, DOMAIN_NAME);
	}
}