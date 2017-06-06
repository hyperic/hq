package org.hyperic.hq.ui.action.resource.group.control;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlControllerNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;



public class EditGroupFormPrepareAction extends
ResourceControlControllerNG  implements ViewPreparer{
	private final Log log = LogFactory.getLog(NewGroupFormPrepareActionNG.class.getName());
    @Resource
	private ControlBoss controlBoss;
    @Resource
    private AppdefBoss appdefBoss;
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		  log.trace("preparing edit group control action");
		  request = getServletRequest();
	        try {
				Integer sessionId = RequestUtils.getSessionId(request).intValue();
				AppdefEntityID appdefId = RequestUtils.getEntityId(request);
				 
	        GroupControlFormNG gForm = new GroupControlFormNG();
	        AppdefGroupValue group = appdefBoss.findGroup(sessionId, appdefId.getId());
	        List<AppdefResourceValue> groupMembers = BizappUtilsNG.buildGroupResources(appdefBoss, sessionId, group,
	            PageControl.PAGE_ALL);

	        Integer trigger = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM);

	        ControlSchedule job = controlBoss.getControlJob(sessionId, trigger);
	        HashMap<String, String> mapOfGroupMembers = new HashMap<String, String>();
			Map<String, String> groupOptions = new LinkedHashMap<String, String>();
	        for (AppdefResourceValue arv : groupMembers) {

	            
	            groupOptions.put(arv.getId().toString(),arv.getName());
	        }
	        gForm.setResourceOrderingOptions(groupOptions);

	        // if this is an ordered control action
	        String resourceOrdering = job.getJobOrderData();
	        if (resourceOrdering != null && !"".equals(resourceOrdering.trim())) {
	            gForm.setInParallel(GroupControlFormNG.IN_ORDER);

	            groupOptions = new LinkedHashMap<String, String>();
	            // comes back in the form of a string list
	            // of group members for ordering 10001,10002,10004. barf.
	            StringTokenizer tok = new StringTokenizer(resourceOrdering, ",");
	            String gmemberId;
	            while (tok.hasMoreTokens()) {
	                gmemberId = tok.nextToken();
	                if (!mapOfGroupMembers.containsKey(gmemberId)) {
	                    // weird, in ordering, but not in group
	                    log.warn("Group control ordering contains id" + " of non group member.");
	                } else {
	                    
	                    groupOptions.put((String) mapOfGroupMembers.get(gmemberId), gmemberId);
	                    mapOfGroupMembers.remove(gmemberId);
	                }
	            }

	            // there are members of the group, that were not contained
	            // in the ordering for some reason
	            if (mapOfGroupMembers.size() != 0) {
	                Set<String> memberIds = mapOfGroupMembers.keySet();
	                Iterator<String> idIterator = memberIds.iterator();

	                while (idIterator.hasNext()) {
	                    gmemberId = idIterator.next();
	                    groupOptions.put((String) mapOfGroupMembers.get(gmemberId), gmemberId);
	                }
	            }

	            gForm.setResourceOrderingOptions(groupOptions);
	        }

	               
	       
	} catch (Exception e) {
		log.error(e,e);
	}
	}


}
