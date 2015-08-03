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

package org.hyperic.hq.ui.action.resource.server.monitor.visibility;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTypeDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * Fetch the children resources for the server
 */
@Component("listChildrenActionNG")
public class ListChildrenActionNG extends BaseActionNG implements ViewPreparer {

	protected final Log log = LogFactory.getLog(getClass().getName());

	@Autowired
	private MeasurementBoss measurementBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		AppdefResourceValue resource = RequestUtils
				.getResource(getServletRequest());

		if (resource == null) {
			addActionError(Constants.ERR_RESOURCE_NOT_FOUND);
			return;
		}

		AppdefEntityID entityId = resource.getEntityId();

		int sessionId;
		try {
			sessionId = RequestUtils.getSessionId(getServletRequest())
					.intValue();
			List<ResourceTypeDisplaySummary> internalHealths = measurementBoss
					.findSummarizedServiceCurrentHealth(sessionId, entityId);
			tilesContext.getRequestScope().put(Constants.CTX_SUMMARIES,
					internalHealths);
		} catch (ServletException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (AppdefEntityNotFoundException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		}

	}
}
