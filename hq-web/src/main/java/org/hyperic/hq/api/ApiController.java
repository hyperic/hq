package org.hyperic.hq.api;

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
	private final static int MAX_PAGE_SIZE = 1000;
	
	@RequestMapping(method = RequestMethod.GET, value = "/{entityMapping}")
	public @ResponseBody Representation list(@PathVariable String entityMapping, 
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) throws Exception {
		if (page == null) page = 1;
		if (size == null) size = MAX_PAGE_SIZE;
		
		return executeRead(entityMapping, "list", page, size);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{entityMapping}")
	@ResponseStatus(value = HttpStatus.CREATED)
	public @ResponseBody Object create(@PathVariable String entityMapping, HttpServletRequest request) throws Exception {
		return executeCreate(entityMapping, request);
	}
	
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, value = "/{entityMapping}/{id}")
	public @ResponseBody Representation getById(@PathVariable String entityMapping, @PathVariable Long id) throws Exception {
		return executeRead(entityMapping, "getById", id);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{entityMapping}/{id}")
	public @ResponseBody Object update(@PathVariable String entityMapping, @PathVariable Long id, HttpServletRequest request) throws Exception {
		return executeUpdate(entityMapping, id, request);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{entityMapping}/{id}")
	public @ResponseBody void delete(@PathVariable String entityMapping, @PathVariable Long id) throws Exception {
		executeDelete(entityMapping, id);
	}
}