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

/*
 * Created on Aug 6, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.common.inventory;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.ConstantsNG;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.ResourceControllerNG;
import org.hyperic.hq.ui.util.RequestUtils;

public abstract class ResourceInventoryPortalActionNG extends
		ResourceControllerNG {

	public String viewResource() throws Exception {
		super.setNavMapLocation(ConstantsNG.INVENTORY_LOC_TYPE);
		AppdefEntityID entityId = RequestUtils.getEntityId(this.request);
		this.checkResourceConfigured(entityId);
		return ConstantsNG.INVENTORY_LOC_TYPE;
	}

	protected void editConfig(Portal portal) throws Exception {
		setResource(true);
		portal.setDialog(true);
		getServletRequest().setAttribute(ConstantsNG.PORTAL_KEY, portal);
	}
}
