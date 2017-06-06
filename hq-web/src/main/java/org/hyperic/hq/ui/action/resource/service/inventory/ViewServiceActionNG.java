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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.inventory.RemoveResourceGroupsFormNG;
import org.hyperic.hq.ui.beans.ConfigValues;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Action loads a service for viewing, including the General Properties, Type
 * and Host Properties and Configuration Properties.The Configuration Text
 * specified in the general Properties are not loaded here.
 */
@Component("viewServiceActionNG")
public class ViewServiceActionNG extends BaseActionNG implements ViewPreparer {

	@Autowired
	protected AppdefBoss appdefBoss;
	@Autowired
	protected ProductBoss productBoss;

	protected void setConfigModifier(HttpServletRequest request,
			AppdefEntityID entityId) {
	};

	protected void setUIOptions(ServiceValue service,
			HttpServletRequest request, ConfigSchema config,
			ConfigResponse oldResponse) {
		List<ConfigValues> uiMonitorOptions = ActionUtils.getConfigValues(
				config, oldResponse);

		request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS, uiMonitorOptions);
		request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
				new Integer(uiMonitorOptions.size()));
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		Log log = LogFactory.getLog(ViewServiceActionNG.class.getName());
		request = getServletRequest();
		try {
			ServiceValue service = (ServiceValue) RequestUtils
					.getResource(request);

			Integer sessionId = RequestUtils.getSessionId(request);
			AppdefEntityID entityId = service.getEntityId();

			PageControl pcg = RequestUtils.getPageControl(request, "psg",
					"png", "sog", "scg");

			List<AppdefGroupValue> groups = appdefBoss
					.findAllGroupsMemberInclusive(sessionId.intValue(), pcg,
							service.getEntityId());
			// check to see if this thing is a platform service
			if (service.getServer().getServerType().getVirtual()) {
				// find the platform resource and add it to the request scope
				try {
					PlatformValue pv = appdefBoss.findPlatformByDependentID(
							sessionId.intValue(), entityId);
					request.setAttribute(Constants.PARENT_RESOURCE_ATTR, pv);
				} catch (PermissionException pe) {
					// TODO Would like to able to fall back and grab the name
					// through other means
					// which isn't easily done right now. Only thing we should
					// prevent
					// in the case of an error is plain text instead of a link.
					log.error("insufficient permissions for parent platform ",
							pe);

					addActionError("resource.service.inventory.error.ViewParentPlatformPermission");
					request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS, new ArrayList());
					request.setAttribute(
							Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
							new Integer(0));
					request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS,
							new ArrayList());
					request.setAttribute(
							Constants.MONITOR_CONFIG_OPTIONS_COUNT,
							new Integer(0));
					return ;
				}
			}
			request.setAttribute(Constants.ALL_RESGRPS_ATTR, groups);
			if (service == null) {
				addActionError(getText("resource.service.error.ServiceNotFound"));
				return ;
			}

			// create and initialize the remove resource groups form
			RemoveResourceGroupsFormNG rmGroupsForm = new RemoveResourceGroupsFormNG();
			rmGroupsForm.setRid(service.getId());
			rmGroupsForm.setType(new Integer(service.getEntityId().getType()));

			int psg = RequestUtils.getPageSize(request, "psg");
			rmGroupsForm.setPsg(new Integer(psg));

			request.setAttribute(
					Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR,
					rmGroupsForm);

			log.debug("AppdefEntityID = " + entityId.toString());

			ConfigSchema config = new ConfigSchema();
			ConfigResponse oldResponse = new ConfigResponse();
			boolean editConfig = false;

			try {
				oldResponse = productBoss.getMergedConfigResponse(
						sessionId.intValue(), ProductPlugin.TYPE_PRODUCT,
						entityId, false);
				config = productBoss.getConfigSchema(sessionId.intValue(),
						entityId, ProductPlugin.TYPE_PRODUCT, oldResponse);

				editConfig = true;
			} catch (ConfigFetchException e) {
				log.warn("Managed Exception ConfigFetchException caught "
						+ e.toString());
			} catch (PluginNotFoundException e) {
				log.warn("Managed Exception PluginNotFoundException caught "
						+ e.toString());
			} catch (EncodingException e) {
				log.error(e);
			} catch (PluginException e) {
				log.error(e);
			}

			List<ConfigValues> uiProductOptions = ActionUtils.getConfigValues(
					config, oldResponse);

			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS,
					uiProductOptions);
			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
					new Integer(uiProductOptions.size()));

			config = new ConfigSchema();
			oldResponse = new ConfigResponse();

			try {
				oldResponse = productBoss.getMergedConfigResponse(
						sessionId.intValue(), ProductPlugin.TYPE_MEASUREMENT,
						entityId, false);
				config = productBoss.getConfigSchema(sessionId.intValue(),
						entityId, ProductPlugin.TYPE_MEASUREMENT, oldResponse);
			} catch (ConfigFetchException e) {
				// do nothing
			} catch (PluginNotFoundException e) {
				// do nothing
			} catch (EncodingException e) {
				log.error(e);
			} catch (PluginException e) {
				log.error(e);
			}

			setUIOptions(service, request, config, oldResponse);

			config = new ConfigSchema();

			oldResponse = productBoss.getMergedConfigResponse(
					sessionId.intValue(), ProductPlugin.TYPE_CONTROL, entityId,
					false);
			try {
				config = productBoss.getConfigSchema(sessionId.intValue(),
						entityId, ProductPlugin.TYPE_CONTROL, oldResponse);
			} catch (PluginNotFoundException e) {
				// do nothing
			} catch (PluginException e) {
				log.error(e);
			}

			List<ConfigValues> uiControlOptions = ActionUtils.getConfigValues(
					config, oldResponse);

			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS,
					uiControlOptions);
			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT,
					new Integer(uiControlOptions.size()));

			request.setAttribute(Constants.AUTO_INVENTORY, new Boolean(service
					.getServer().getRuntimeAutodiscovery()));

			if (!editConfig) {
				addActionError("resource.common.inventory.error.serverConfigNotSet");
			}
			request.setAttribute(Constants.EDIT_CONFIG, new Boolean(editConfig));

			setConfigModifier(request, entityId);

			

		} catch (ApplicationException e) {
			log.error("unable to retrieve configuration properties ", e);
			addActionError("resource.common.inventory.error.configRetrieveError");
			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS,
					new ArrayList());
			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
					new Integer(0));
			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS,
					new ArrayList());
			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
					new Integer(0));
			
		} catch (ServletException e) {
			
			log.error(e);
		} catch (EncodingException e) {
			log.error(e);
		}

	}
}
