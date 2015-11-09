package org.hyperic.hq.ui.action.resource.group.control;
/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */



import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.control.ControlFormNG;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlControllerNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> subclass that prepares a control action associated
 * with a group.
 */
@Component("newGroupControlTabFormPrepareActionNG")
public class NewGroupFormPrepareActionNG extends
ResourceControlControllerNG  implements ViewPreparer{
	private final Log log = LogFactory.getLog(NewGroupFormPrepareActionNG.class.getName());
    @Resource
	private ControlBoss controlBoss;
    @Resource
    private AppdefBoss appdefBoss;

    /**
     * Create the control action and associate it with the group. populates
     * resourceOrdering in the GroupControlForm.
     */
    public void execute (TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
    	
    	request = getServletRequest();
    	String isEdit=request.getParameter(Constants.CONTROL_BATCH_ID_PARAM);
        if (isEdit==null) {// new group control action
        	try {
                log.trace("preparing new group control action");
                Integer sessionId = RequestUtils.getSessionId(request).intValue();
                GroupControlFormNG gForm = new GroupControlFormNG();

                AppdefEntityID appdefId = RequestUtils.getEntityId(request);

//                List<String> actions = controlBoss.getActions(sessionId, appdefId);
//                List<OptionItem> options = OptionItem.createOptionsList(actions);
//                for (String action : actions) {
//        			 String value = action;
//        		     String label = StringUtil.capitalize(value);
//        		     options.add(label);
//        		}
//                gForm.setControlActions(options);
//                gForm.setNumControlActions(new Integer(options.size()));

                // get the resource ids associated with this group,
                // create an options list, and associate it with the form
               
                AppdefGroupValue group = (AppdefGroupValue) RequestUtils
        				.getResource(request);
                List<AppdefResourceValue> groupMembers;
        		
        		groupMembers = BizappUtilsNG.buildGroupResources(appdefBoss, sessionId, group,
        		    PageControl.PAGE_ALL);


        		Map<String, String> groupOptions = new LinkedHashMap<String, String>();
                for (AppdefResourceValue arv : groupMembers) {

                    
                    groupOptions.put(arv.getId().toString(),arv.getName());
                }
                gForm.setResourceOrderingOptions(groupOptions);
                gForm=buildCurrentcForm(gForm,request);
                if ((!gForm.getInParallel().equals(null)) &&(gForm.getInParallel().equals(Boolean.TRUE))) {
                	 request.setAttribute("parallel","true");
        		}
                request.setAttribute("gForm",gForm);
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			log.error(e,e);
        		}

		} else {
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
		        gForm.setInParallel(GroupControlFormNG.IN_PARALLEL);
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
		        if (gForm.getInParallel().equals(GroupControlFormNG.IN_PARALLEL)) {
               	 request.setAttribute("parallel","true");
       		}
		        request.setAttribute("gForm",gForm);       
		       
		} catch (Exception e) {
			log.error(e,e);
		}
		}


		}
    	       
    
    public static  GroupControlFormNG buildCurrentcForm(GroupControlFormNG form,HttpServletRequest req) {
    	String parallel=req.getParameter("inParallel");
		if (parallel==null || parallel.equals("true")) {
			form.setInParallel(Boolean.TRUE);
		} else {
			form.setInParallel(Boolean.FALSE);
		}
    	return form;
    }
   
}
