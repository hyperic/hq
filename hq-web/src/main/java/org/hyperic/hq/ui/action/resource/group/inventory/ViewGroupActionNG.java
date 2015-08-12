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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.MessageResources;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("viewGroupActionNG")
public class ViewGroupActionNG extends BaseActionNG implements ViewPreparer {

	protected final Log log = LogFactory.getLog(ViewGroupActionNG.class);
	@Autowired
	private AppdefBoss appdefBoss;

	/**
	 * @return a group type label from the list of group labels
	 */
	private String getGroupTypeLabel(AppdefGroupValue group, List groupLabels) {
		Iterator gIterator = groupLabels.iterator();

		while (gIterator.hasNext()) {
			Map item = (Map) gIterator.next();
			Integer groupType = (Integer) item.get("value");
			if (groupType.intValue() == group.getGroupType())
				return (String) item.get("label");
		}

		return "";
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		request = getServletRequest();
		try {
			int sessionId = RequestUtils.getSessionIdInt(request);

			PageControl pc = RequestUtils.getPageControl(request, "ps", "pn",
					"so", "sc");
			AppdefGroupValue group = (AppdefGroupValue) RequestUtils
					.getResource(request);

			if (group == null) {
				addActionError(getText("resource.group.inventory.error.GroupNotFound"));
				return;
			}

			List<AppdefResourceValue> appdefValues = BizappUtils
					.buildGroupResources(appdefBoss, sessionId, group, pc);

			if (group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP) {
				Map<String, Integer> typeMap = AppdefResourceValue
						.getResourceTypeCountMap(appdefValues);
				request.setAttribute(Constants.RESOURCE_TYPE_MAP_ATTR, typeMap);
			} else if (group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS) {
				request.setAttribute(
						Constants.RESOURCE_TYPE_MAP_ATTR,
						appdefBoss.getResourceTypeCountMap(sessionId,
								group.getId()));
			}

			request.setAttribute(Constants.APPDEF_ENTRIES_ATTR, appdefValues);

			RemoveGroupResourcesForm rmGroupForm = new RemoveGroupResourcesForm();
			int ps = RequestUtils.getPageSize(request, "ps");
			rmGroupForm.setPs(new Integer(ps));

			request.setAttribute(Constants.GROUP_REMOVE_MEMBERS_FORM_ATTR,
					rmGroupForm);

			// set the group type label
			List groupLabels = BizappUtils.buildGroupTypes(request);

			String groupType = getGroupTypeLabel(group, groupLabels);
			request.setAttribute(Constants.GROUP_TYPE_LABEL, groupType);

		} catch (ServletException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (ObjectNotFoundException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (SessionException e) {
			log.error(e);
		}
	}
}
