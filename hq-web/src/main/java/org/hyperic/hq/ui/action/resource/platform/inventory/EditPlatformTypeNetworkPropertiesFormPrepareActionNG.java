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

package org.hyperic.hq.ui.action.resource.platform.inventory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * form for editing a platform's type and network properties.
 */
@Component("editPlatformTypeNetworkPropertiesFormPrepareActionNG")
public class EditPlatformTypeNetworkPropertiesFormPrepareActionNG extends
		BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory
			.getLog(EditPlatformTypeNetworkPropertiesFormPrepareActionNG.class
					.getName());
	@Resource
	private AppdefBoss appdefBoss;

	/**
	 * Retrieve the data necessary to display the
	 * <code>TypeNetworkPropertiesForm</code> page.
	 * 
	 */
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		try {
			PlatformFormNG editForm = new PlatformFormNG();
			request = getServletRequest();
			clearErrorsAndMessages();
			
			Integer platformId = Integer.parseInt( request.getParameter("rid") );
			Integer sessionId = RequestUtils.getSessionId(request);
			PlatformValue platform = appdefBoss.findPlatformById(sessionId.intValue(), platformId);

			if (platform == null) {
				addActionError(getText(Constants.ERR_PLATFORM_NOT_FOUND));
				return;
			} else {
				editForm.loadPlatformValue(platform);

				log.trace("getting all platform types");
				List<PlatformTypeValue> platformTypes = appdefBoss
						.findAllPlatformTypes(sessionId.intValue(),
								PageControl.PAGE_ALL);
				editForm.setResourceTypes(platformTypes);

				String usedIpPort = "";
				if (platform.getAgent() != null) {
					usedIpPort = platform.getAgent().getAddress() + ":"
							+ platform.getAgent().getPort();
				}
				BizappUtilsNG.populateAgentConnections(sessionId.intValue(),
						appdefBoss, request, editForm, usedIpPort);

				// respond to an add or remove click- we do this here
				// rather than in the edit Action because in between
				// these two actions, struts repopulates the form bean and
				// resets numIps to whatever value was submitted in the
				// request.
				
				
				// the form is being set up for the first time. ignore if it is add or remove ips
				if ( request.getParameter("add.x") == null && request.getParameter("remove.x") == null ) { 
					IpValue[] savedIps = platform.getIpValues();
					int numSavedIps = savedIps != null ? savedIps.length : 0;
					for (int i = 0; i < numSavedIps; i++) {
						editForm.setIp(i, savedIps[i]);
					}
				}


				
				// load all the created ip fields
		        String[] curAddresses = request.getParameterValues("addresses");
		        String[] selMACAddresses = request.getParameterValues("mACAddresses");
		        String[] selNetMasks = request.getParameterValues("netmasks");
		        
		        int curIpsSize = editForm.getIps().length;

		        if (curAddresses != null && curAddresses.length >0 ) {
		        	int cnt=0;
		        	for (String curAddress : curAddresses ){
		        		if (cnt >= curIpsSize) {
		        			editForm.setIp(cnt, new IpValue(curAddresses[cnt], selNetMasks[cnt], selMACAddresses[cnt]));
		        		}
		        		cnt++;
		        	}
		        }
		        editForm.setNumIps(editForm.getIps().length);
				
		        if ( request.getParameter("add.x")!= null ) {
		            int nextIndex = editForm.getNumIps();
		            for (int i = 0; i < nextIndex + 1; i++) {
		                IpValue oldIp = editForm.getIp(i);
		                if (oldIp == null) {
		                	editForm.setIp(i, new IpValue());
		                }
		            }
		    		setHeaderResources();
		    		request.setAttribute("AddAction", Boolean.TRUE);
		           
		        } else if ( request.getParameter("remove.x")!= null ) {
		            int ri = Integer.parseInt(request.getParameter("remove.x"));
		
		            IpValue[] oldIps = editForm.getIps();
		            if (oldIps != null) {
		                // remove the indicated ip, leaving all others
		                // intact
		                ArrayList<IpValue> oldIpsList = new ArrayList<IpValue>(Arrays.asList(oldIps));
		                oldIpsList.remove(ri);
		                IpValue[] newIps = oldIpsList.toArray(new IpValue[0]);
		
		                // automatically sets numIps
		                editForm.setIps(newIps);
		            } 
		    		setHeaderResources();
		    		request.setAttribute("RemoveAction", Boolean.TRUE);
		        }
		        
		        editForm.setNumIps(editForm.getIps().length);
			}
				 	

			// the OSType dropdown is NOT editable in edit mode hence the false
			request.setAttribute(Constants.PLATFORM_OS_EDITABLE, Boolean.FALSE);
			request.setAttribute("editForm", editForm);
			
	        Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformTypeNetworkPropertiesTitle",
	                ".resource.platform.inventory.EditPlatformTypeNetworkProperties");
	        portal.setDialog(true);
	        request.setAttribute(Constants.PORTAL_KEY, portal);
	        request.setAttribute(Constants.TITLE_PARAM_ATTR, editForm.getName());

		} catch (Exception e) {
			log.error(e);
		} 
	}
}
