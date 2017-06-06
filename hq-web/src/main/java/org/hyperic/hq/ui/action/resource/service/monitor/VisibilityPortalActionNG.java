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

package org.hyperic.hq.ui.action.resource.service.monitor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceVisibilityPortalActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A <code>BaseDispatchAction</code> that sets up service monitor portals.
 */
@Component("serviceMonitorPortalActionNG")
@Scope("prototype")
public class VisibilityPortalActionNG extends ResourceVisibilityPortalActionNG {

	private final Log log = LogFactory.getLog(VisibilityPortalActionNG.class
			.getName());
	private static final String ERR_SERVER_PERMISSION = "resource.service.monitor.visibility.error.ServerPermission";

	@Autowired
	private MeasurementBoss measurementBoss;

	public String currentHealth() throws Exception {
		setResource();

		findServersHealths(getServletRequest());

		super.currentHealth();

		Portal portal = Portal.createPortal(
				"resource.service.monitor.visibility.CurrentHealthTitle",
				".resource.service.monitor.visibility.CurrentHealth");
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "monitorServiceCurrentHealth";
	}

	public String resourceMetrics() throws Exception {
		setResource();

		findServersHealths(getServletRequest());

		super.resourceMetrics();

		// findHostHealths(request);
		Portal portal = Portal.createPortal(
				"resource.service.monitor.visibility.ServiceMetricsTitle",
				".resource.service.monitor.visibility.ServiceMetrics");
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "monitorServiceReourceMetrics";
	}

	public String performance() throws Exception {
		setResource();

		super.performance();

		Portal portal = Portal.createPortal(
				"resource.service.monitor.visibility.PerformanceTitle",
				".resource.service.monitor.visibility.Performance");
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "performance";
	}

	private void findServersHealths(HttpServletRequest request)
			throws Exception {
		AppdefEntityID entityId = null;
		Exception thrown = null;

		try {
			int sessionId = RequestUtils.getSessionId(request).intValue();
			entityId = RequestUtils.getEntityId(request);

			// for a regular service, there can only be one
			PageControl pc = PageControl.PAGE_ALL;

			ServiceValue sv = appdefBoss.findServiceById(sessionId,
					entityId.getId());
			// check for platform services
			List<ResourceDisplaySummary> healths = null;
			if (sv.getServer().getServerType().getVirtual()) {
				request.setAttribute(Constants.PLATFORM_SERVICE_ATTR, "true");
				AppdefEntityID platId = appdefBoss.findPlatformByDependentID(
						sessionId, entityId).getEntityId();
				healths = measurementBoss.findPlatformsCurrentHealth(sessionId,
						platId, pc);
			} else {
				// for a "clustered services" there'd be many... does
				// that get handled here or the group monitoring?
				healths = measurementBoss.findServersCurrentHealth(sessionId,
						entityId, pc);

			}
			request.setAttribute(Constants.HOST_HEALTH_SUMMARIES_ATTR, healths);
		} catch (AppdefEntityNotFoundException e) {
			thrown = e;
			addActionError(getText(Constants.ERR_RESOURCE_NOT_FOUND));
		} catch (PermissionException e) {
			thrown = e;
			request.setAttribute(Constants.ERR_SERVER_HEALTH_ATTR,
					ERR_SERVER_PERMISSION);
		} finally {
			if (thrown != null && log.isDebugEnabled()) {
				log.debug("resource [" + entityId + "] access error", thrown);
			}
		}

	}
}
