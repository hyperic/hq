/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.stereotype.Component;

@Component("newGroupFormPrepareActionNG")
public class NewGroupFormPrepareActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory.getLog(NewGroupFormPrepareActionNG.class);
	
	@Resource
    private AppdefBoss appdefBoss;
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
			AppdefEntityTypeID aetid=null;
			
			Portal portal = Portal.createPortal(
					"resource.group.inventory.NewGroup",
					".resource.group.inventory.NewGroup");
			portal.setDialog(true);
			request.setAttribute(Constants.PORTAL_KEY, portal);
			
			
	        GroupFormNG resourceForm = new GroupFormNG();
	        
	        resourceForm.setName( request.getParameter("name") );
	        resourceForm.setLocation( request.getParameter("location") );
	        resourceForm.setDescription( request.getParameter("description") );
	        if (request.getParameter("groupType") != null) {
	        	resourceForm.setGroupType( Integer.valueOf(request.getParameter("groupType")) );
	        }
	        if (request.getParameter("typeAndResourceTypeId") != null) {
	        	resourceForm.setTypeAndResourceTypeId(request.getParameter("typeAndResourceTypeId" ) );
	        }
	        /*
	        if (request.getParameter("resources") != null) {
	        	AppdefEntityTypeID aetid = new AppdefEntityTypeID(request.getParameter("resources"));
	        	resourceForm.setTypeName(aetid.getTypeName());
	        }
	        */
	        
	
	        LinkedHashMap<String, String> groupTypes = this.buildGroupTypes();
	        Integer sessionId = RequestUtils.getSessionId(request);
	
	        HttpSession session = request.getSession();
	
	        List platformTypes, serverTypes, serviceTypes, applicationTypes;
	        String[] eids = (String[]) session.getAttribute(Constants.ENTITY_IDS_ATTR);
	        
	        if (request.getParameter("resources") != null) {
	        	aetid = new AppdefEntityTypeID(request.getParameter("resources"));
	        }

	        if (eids != null) {
	            resourceForm.setEntityIds(eids);
	
	            AppdefResourceType art = null;
	            Integer ff = (Integer) session.getAttribute(Constants.RESOURCE_TYPE_ATTR);
	
	            // HHQ-2839: Cleanup from new group session
	            session.removeAttribute(Constants.ENTITY_IDS_ATTR);
	            session.removeAttribute(Constants.RESOURCE_TYPE_ATTR);
	
	            if (ff != null) {
	                // Only check if resource type is platform, server, or service
	                switch (ff.intValue()) {
	                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
	                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
	                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
	                        // See if they have a common resource type
	                        art = appdefBoss.findCommonResourceType(sessionId.intValue(), eids);
	                        break;
	                    default:
	                        break;
	                }
	            }
	
	            if (art != null) {
	                resourceForm.setGroupType(new Integer(Constants.APPDEF_TYPE_GROUP_COMPAT));
	                aetid = new AppdefEntityTypeID(art);
	                resourceForm.setTypeAndResourceTypeId(aetid.getAppdefKey());
	                resourceForm.setTypeName(art.getName());
	                request.setAttribute("resourceForm", resourceForm);	
	                return ;
	            } else {
	                resourceForm.setGroupType(new Integer(Constants.APPDEF_TYPE_GROUP_ADHOC));
	
	                String mixRes;
	                if (ff != null) {
	                    switch (ff.intValue()) {
	                        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
	                        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
	                        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
	                            resourceForm.setTypeAndResourceTypeId("" + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS +
	                                                             ":-1");
	                            mixRes = "dash.home.DisplayCategory.group." + "plat.server.service";
	                            break;
	                        default:
	                            resourceForm.setTypeAndResourceTypeId(ff.toString() + ":-1");
	                            mixRes = ff.intValue() == AppdefEntityConstants.APPDEF_TYPE_GROUP ? "dash.home.DisplayCategory.group.groups"
	                                                                                             : "dash.home.DisplayCategory.group.application";
	                            break;
	                    }
	
	                    resourceForm.setTypeName(getText(mixRes) );
	                    request.setAttribute("resourceForm", resourceForm);	
	                    return ;
	                }
	            }
	        } else {
	
		        platformTypes = appdefBoss.findViewablePlatformTypes(sessionId.intValue(), PageControl.PAGE_ALL);
		
		        serverTypes = appdefBoss.findViewableServerTypes(sessionId.intValue(), PageControl.PAGE_ALL);
		
		        serviceTypes = appdefBoss.findViewableServiceTypes(sessionId.intValue(), PageControl.PAGE_ALL);
		
		        applicationTypes = appdefBoss.findAllApplicationTypes(sessionId.intValue());
		
		        resourceForm.setPlatformTypes(platformTypes);
		        resourceForm.setServerTypes(serverTypes);
		        resourceForm.setServiceTypes(serviceTypes);
		        resourceForm.setApplicationTypes(applicationTypes);
		        resourceForm.setGroupTypes(groupTypes);
	        }
	        if (aetid != null) {
	        	Integer inpTypeName = aetid.getType();
	        	resourceForm.setRid(aetid.getId());
	        	resourceForm.setType(aetid.getType());
	        	switch (inpTypeName) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    resourceForm.setTypeAndResourceTypeId("" + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS +
                                                     ":-1");
                    resourceForm.setGroupType(1);
                    resourceForm.setTypeName(getText("dash.home.DisplayCategory.group.plat.server.service") );
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                	resourceForm.setTypeName(getText("dash.home.DisplayCategory.group.application") );
                	// Mixed
                	resourceForm.setGroupType(1);
                	resourceForm.setTypeAndResourceTypeId("4:-1");
                    break;
                default:
                	resourceForm.setGroupType(1);
                	resourceForm.setTypeName(aetid.getTypeName());
                    resourceForm.setTypeAndResourceTypeId("" + AppdefEntityConstants.APPDEF_TYPE_GROUP + ":-1");
                	resourceForm.setTypeName(getText("dash.home.DisplayCategory.group.groups") );
                    break;
	        	}
	        }
	        request.setAttribute("resourceForm", resourceForm);	
	        
			
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	
    /**
     * build group types and its corresponding resource string 
     * respresentations from the ApplicationResources.properties file.
     *
     * @return a list 
     */
    private LinkedHashMap<String, String> buildGroupTypes() 
    {
        
    	LinkedHashMap<String, String> groupTypes = new LinkedHashMap<String, String>();

    	groupTypes.put( String.valueOf( Constants.APPDEF_TYPE_GROUP_COMPAT ) , getText("resource.group.inventory.CompatibleClusterResources")  );
    	groupTypes.put( String.valueOf( Constants.APPDEF_TYPE_GROUP_ADHOC ) , getText("resource.group.inventory.MixedResources")  );
        return groupTypes;
    }

}
