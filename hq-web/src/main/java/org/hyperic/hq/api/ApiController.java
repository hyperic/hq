package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.api.representation.LinkedRepresentation;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.SimpleRep;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/api")
public class ApiController extends BaseController {
	@RequestMapping(method = RequestMethod.GET)
	public ListRep getApi() {
		List<SimpleRep> list = new ArrayList<SimpleRep>();
		
		list.add(new SimpleRep(null, LinkedRepresentation.RESOURCES_LABEL, LinkHelper.getDomainUri(LinkedRepresentation.RESOURCES_LABEL)));
		list.add(new SimpleRep(null, LinkedRepresentation.RESOURCE_GROUPS_LABEL, LinkHelper.getDomainUri(LinkedRepresentation.RESOURCE_GROUPS_LABEL)));
		list.add(new SimpleRep(null, LinkedRepresentation.RESOURCE_TYPES_LABEL, LinkHelper.getDomainUri(LinkedRepresentation.RESOURCE_TYPES_LABEL)));

		Map<String, String> links = new HashMap<String, String>();
		
		links.put(LinkedRepresentation.SELF_LABEL, LinkHelper.getRootUri());
		
		return new ListRep(list, links);
	}
}