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

package org.hyperic.hq.ui.action.resource.hub;

import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;

/**
 * 
 * Fetch the designated metrics for a resource
 */
@Component(value = "resourceDesignatedMetricsActionNG")
@Scope(value = "prototype")
public class ResourceDesignatedMetricsActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory.getLog(getClass());
	private final HashSet<String> categories = new HashSet<String>();
	@Autowired
	private MeasurementBoss measurementBoss;

	public ResourceDesignatedMetricsActionNG() {
		super();

		categories.add(MeasurementConstants.CAT_AVAILABILITY);
		categories.add(MeasurementConstants.CAT_UTILIZATION);
		categories.add(MeasurementConstants.CAT_THROUGHPUT);
	}

	
	public void execute(TilesRequestContext tilesRequestCtx, AttributeContext attributeContext) {
		Attribute attribute = attributeContext.getAttribute(Constants.ENTITY_ID_PARAM);
		String appdefKey = ((attribute == null)? "" : (String)attribute.getValue());
		AppdefEntityID entityId = new AppdefEntityID(appdefKey);
		
		try {
			int sessionId = RequestUtils.getSessionId(getServletRequest()).intValue();
			List<MeasurementTemplate> designates = measurementBoss
					.getDesignatedTemplates(sessionId, entityId, categories);
			tilesRequestCtx.getRequestScope().put(Constants.CTX_SUMMARIES, designates);
		} catch (ServletException e) {
			log.error(e);	
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (AppdefEntityNotFoundException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		}
	}
}
