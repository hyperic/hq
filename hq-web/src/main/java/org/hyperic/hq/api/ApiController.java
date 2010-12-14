package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/api")
public class ApiController extends BaseController {
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Representation root() throws Exception {
		return new Representation();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{domainName}")
	public @ResponseBody Representation list(@PathVariable String domainName, ListSettings listSettings) throws Exception {
		return readMultipleEntities(domainName, listSettings);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{domainName}")
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody Representation create(@PathVariable String domainName, HttpServletRequest request) throws Exception {
		return createSingleEntity(domainName, request);
	}
	
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, value = "/{domainName}/{id}")
	public @ResponseBody Representation getById(@PathVariable String domainName, @PathVariable Long id) throws Exception {
		return readSingleEntity(domainName, id);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{domainName}/{id}")
	public @ResponseBody Representation update(@PathVariable String domainName, @PathVariable Long id, HttpServletRequest request) throws Exception {
		return updateSingleEntity(domainName, id, request);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{domainName}/{id}")
	public @ResponseBody void delete(@PathVariable String domainName, @PathVariable Long id) throws Exception {
		deleteSingleEntity(domainName, id);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{domainName}/{id}/relationships")
	public @ResponseBody Representation listRelationships(@PathVariable String domainName, @PathVariable Long id, ListSettings listSettings) throws Exception {
		return readRelationships(domainName, id, listSettings);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{domainName}/{fromId}/relationships/{toId}")
	public void deleteRelationship(@PathVariable String domainName, @PathVariable Long fromId, @PathVariable Long toId) throws Exception {
		deleteRelationship(domainName, fromId, toId, null, null);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{domainName}/{id}/relationships/{relationshipName}")
	public @ResponseBody Representation listRelationshipsByName(@PathVariable String domainName, @PathVariable Long id, @PathVariable String relationshipName, @RequestParam(required = false) String direction, ListSettings listSettings) throws Exception {
		return readRelationships(domainName, id, relationshipName, direction, listSettings);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{domainName}/{id}/relationships/{relationshipName}")
	public @ResponseBody Representation createRelationship(@PathVariable String domainName, @PathVariable Long id, @PathVariable String relationshipName, @RequestParam Long toId) throws Exception {
		return createRelationship(domainName, id, toId, relationshipName);
	}
}