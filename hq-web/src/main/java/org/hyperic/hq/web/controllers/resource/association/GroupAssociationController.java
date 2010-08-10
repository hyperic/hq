package org.hyperic.hq.web.controllers.resource.association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.web.controllers.BaseController;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class GroupAssociationController extends BaseController {
	@Autowired
	public GroupAssociationController(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
		super(appdefBoss, authzBoss);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/resource/associations")
	public @ResponseBody
	Map<String, List<Map<String, Object>>> getAvailableAssociations(
			@RequestParam(RequestParameterKeys.RESOURCE_IDS) String[] resourceAppdefEntityIds,
			HttpSession session) {
		AppdefEntityID[] appdefEntityIds = new AppdefEntityID[resourceAppdefEntityIds.length];

		for (int x = 0; x < resourceAppdefEntityIds.length; x++) {
			appdefEntityIds[x] = new AppdefEntityID(resourceAppdefEntityIds[x]);
		}

		WebUser webUser = getWebUser(session);
		PageList<AppdefGroupValue> availableGroups;
		Map<String, List<Map<String, Object>>> result = new LinkedHashMap<String, List<Map<String, Object>>>();

		try {
			availableGroups = getAppdefBoss().findAllGroupsMemberExclusive(
					webUser.getSessionId(), PageControl.PAGE_ALL,
					appdefEntityIds);
			
			List<Map<String, Object>> groups = new ArrayList<Map<String,Object>>();
			
			for (AppdefGroupValue group : availableGroups) {
				Map<String, Object> groupInfo = new HashMap<String, Object>();
			
				groupInfo.put("id", group.getId());
				groupInfo.put("name", group.getName());
				groupInfo.put("description", group.getDescription());
				groups.add(groupInfo);
			}
			
			result.put("groups", groups);
		} catch (PermissionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/resource/association")
	public String createAssociation(
			@RequestParam(RequestParameterKeys.GROUP_ID) Integer[] groupIds,
			@RequestParam(RequestParameterKeys.RESOURCE_IDS) String[] resourceAppdefEntityIds,
			HttpSession session) {
		String redirectString = "redirect:/app/resource/";

		// ...now check for the resources to be added to the group(s),
		// otherwise it's a no-op...
		try {
			WebUser webUser = getWebUser(session);

			for (String resourceAppdefEntityId : resourceAppdefEntityIds) {
				getAppdefBoss().batchGroupAdd(webUser.getSessionId(),
						new AppdefEntityID(resourceAppdefEntityId), groupIds);
			}
			
			if (groupIds.length == 1) {
				redirectString += "association/" + groupIds[0];
			} else {
				redirectString += "associations";
			}
		} catch (SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (VetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return redirectString;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/resource/association/{groupId}")
	public @ResponseBody
	Map<String, String> getAssociation() {
		return new HashMap<String, String>();
	}
}