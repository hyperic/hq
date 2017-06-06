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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.stereotype.Component;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * form for creating a platform.
 */
@Component("newPlatformFormPrepareActionNG")
public class NewPlatformFormPrepareActionNG extends BaseActionNG implements
		ViewPreparer {
	
	private final Log log = LogFactory.getLog(NewPlatformFormPrepareActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	        PlatformFormNG newForm = new PlatformFormNG();
	
	        Integer sessionId = RequestUtils.getSessionId(request);
	
	        List<PlatformTypeValue> resourceTypes = new ArrayList<PlatformTypeValue>();
	        List<PlatformTypeValue> platformTypes = appdefBoss.findAllPlatformTypes(sessionId.intValue(),
	            PageControl.PAGE_ALL);
	        // XXX need a finder for device types, this will do for the moment.
	        for (PlatformTypeValue pType : platformTypes) {
	            if (PlatformDetector.isSupportedPlatform(pType.getName())) {
	                continue;
	            }
	            resourceTypes.add(pType);
	        }
	        newForm.setResourceTypes(resourceTypes);
	
	        BizappUtilsNG.populateAgentConnectionsNG(sessionId.intValue(), appdefBoss, request, newForm, "");
 

	        // the OSType dropdown is editable in new create mode hence the true
	        
	        // Check for populated values
	        newForm.setName(request.getParameter("name"));
	        String selResourceType = request.getParameter("resourceType");
	        if ( (selResourceType!= null) && !selResourceType.equals("") ) {
	        	newForm.setResourceType(Integer.valueOf(selResourceType));
	        }
	        newForm.setFqdn(request.getParameter("fqdn"));
	        newForm.setDescription(request.getParameter("description"));
	        newForm.setLocation(request.getParameter("location"));
	        
	        String[] curAddresses = request.getParameterValues("addresses");
	        String[] selMACAddresses = request.getParameterValues("MACAddresses");
	        String[] selNetMasks = request.getParameterValues("netmasks");
	        if (curAddresses != null && curAddresses.length >0 ) {
	        	newForm.setAddresses(curAddresses);
	        	newForm.setNumIps(curAddresses.length);
	        }
	        if (selMACAddresses != null && selMACAddresses.length >0 ) {
	        	newForm.setMACAddresses(selMACAddresses);
	        }
	        if (selNetMasks != null && selNetMasks.length >0 ) {
	        	newForm.setNetmasks(selNetMasks);
	        }
	        if (curAddresses != null && curAddresses.length >0 ) {
	        	int cnt=0;
	        	for (String curAddress : curAddresses ){
	        		newForm.setIp(cnt, new IpValue(curAddresses[cnt], selNetMasks[cnt], selMACAddresses[cnt]));
	        		cnt++;
	        	}
	        }
	        // respond to an add or remove click- we do this here
	        // rather than in NewPlatformAction because in between
	        // these two actions, struts repopulates the form bean and
	        // resets numIps to whatever value was submitted in the
	        // request.
	
	        if ( request.getParameter("add.x")!= null ) {
	            int nextIndex = newForm.getNumIps();
	            for (int i = 0; i < nextIndex + 1; i++) {
	                IpValue oldIp = newForm.getIp(i);
	                if (oldIp == null) {
	                	newForm.setIp(i, new IpValue());
	                }
	            }
	    		setHeaderResources();
	    		request.setAttribute("AddAction", Boolean.TRUE);
	           
	        } else if ( request.getParameter("remove.x")!= null ) {
	            int ri = Integer.parseInt(request.getParameter("remove.x"));
	
	            IpValue[] oldIps = newForm.getIps();
	            if (oldIps != null) {
	                // remove the indicated ip, leaving all others
	                // intact
	                ArrayList<IpValue> oldIpsList = new ArrayList<IpValue>(Arrays.asList(oldIps));
	                oldIpsList.remove(ri);
	                IpValue[] newIps = oldIpsList.toArray(new IpValue[0]);
	
	                // automatically sets numIps
	                newForm.setIps(newIps);
	            } 
	    		setHeaderResources();
	    		request.setAttribute("RemoveAction", Boolean.TRUE);
	        }
	        newForm.setNumIps(newForm.getIps().length);
	        request.setAttribute(Constants.PLATFORM_OS_EDITABLE, Boolean.TRUE);
	        request.setAttribute("resourceForm",newForm);
	        request.setAttribute("editForm",newForm);
	        
	        Portal portal = Portal.createPortal("resource.platform.inventory.NewPlatformTitle",
	                ".resource.platform.inventory.NewPlatform");
	            portal.setDialog(true);
	            getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
	            
		} catch (Exception ex) {
			log.error(ex,ex);
		}
	}
	
	
    private void resetFormIps(PlatformFormNG form) {
        IpValue[] ips = new IpValue[1];
        ips[0] = new IpValue();
        // automatically sets numIps
        form.setIps(ips);
    }

}
