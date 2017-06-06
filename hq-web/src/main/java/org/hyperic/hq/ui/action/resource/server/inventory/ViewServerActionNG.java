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

package org.hyperic.hq.ui.action.resource.server.inventory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ServerValue;
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
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Action loads a server for viewing, including the General Properties, Type and
 * Host Properties and Configuration Properties.The Configuration Text specified
 * in the general Properties are not loaded here.
 */
@Component("viewServerActionNG")
public class ViewServerActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewServerActionNG.class
			.getName());

	@Autowired
	private AppdefBoss appdefBoss;
	@Autowired
	private ProductBoss productBoss;

	protected void setConfigModifier(HttpServletRequest request,
			AppdefEntityID entityId) {
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			request = getServletRequest();
			ServerValue server = (ServerValue) RequestUtils
					.getResource(request);
			if (server == null) {
				addActionError(getText("resource.server.error.ServerNotFound"));
				return;
			}
			// load the server groups

			int sessionId = RequestUtils.getSessionId(request).intValue();

			AppdefEntityID entityId = server.getEntityId();
			log.trace("getting service count for server");
			// PageControl pc = new PageControl(0, -1, 1,SortAttribute.SERVICE_TYPE);
			PageControl pc = RequestUtils.getPageControl(getServletRequest(), "pss", "pns", "sos", "scs");
			Collection<AppdefResourceValue> services = appdefBoss
					.findServicesByServer(sessionId, server.getId(), pc);
			request.setAttribute(Constants.NUM_CHILD_RESOURCES_ATTR,
					new Integer( ( (PageList<AppdefResourceValue>) services).getTotalSize()   ));
			request.setAttribute("ChildResources",services);
			

			log.trace("getting service type map for server");
			Map<String, Integer> typeMap = AppdefResourceValue
					.getServiceTypeCountMap(services);
			request.setAttribute(Constants.RESOURCE_TYPE_MAP_ATTR, typeMap);

			BizappUtilsNG.setRuntimeAIMessage(sessionId, request, server,
					appdefBoss);

			PageControl pcg = RequestUtils.getPageControl(request, "psg",
					"png", "sog", "scg");

			List<AppdefGroupValue> groups = appdefBoss
					.findAllGroupsMemberInclusive(sessionId, pcg,
							server.getEntityId());

			request.setAttribute(Constants.ALL_RESGRPS_ATTR, groups);

			// create and initialize the remove resource groups form
			RemoveResourceGroupsFormNG rmGroupsForm = new RemoveResourceGroupsFormNG();
			rmGroupsForm.setRid(server.getId());
			rmGroupsForm.setType(new Integer(server.getEntityId().getType()));

			int psg = RequestUtils.getPageSize(request, "psg");
			rmGroupsForm.setPsg(new Integer(psg));

			request.setAttribute(
					Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR,
					rmGroupsForm);

			ConfigResponse oldResponse = productBoss.getMergedConfigResponse(
					sessionId, ProductPlugin.TYPE_PRODUCT, entityId, false);

			ConfigSchema config = productBoss.getConfigSchema(sessionId,
					entityId, ProductPlugin.TYPE_PRODUCT, oldResponse);

			boolean platformWithAgent = false;

			// it is no longer a ahack as we are doing the right thing now.
			try {
				if (appdefBoss.findResourceAgent(server.getPlatform()
						.getEntityId()) != null)
					platformWithAgent = true;
			} catch (AgentNotFoundException e) {
				// do nothing as platformAithAgent is already false
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
				oldResponse = productBoss.getMergedConfigResponse(sessionId,
						ProductPlugin.TYPE_MEASUREMENT, entityId, false);

				config = productBoss.getConfigSchema(sessionId, entityId,
						ProductPlugin.TYPE_MEASUREMENT, oldResponse);

				if (server.getWasAutodiscovered()) {
					request.setAttribute(Constants.SERVER_BASED_AUTO_INVENTORY,
							new Integer(0));
				} else {
					request.setAttribute(Constants.SERVER_BASED_AUTO_INVENTORY,
							new Integer(1));
					request.setAttribute(
							Constants.SERVER_BASED_AUTO_INVENTORY_VALUE,
							new Boolean(server.getRuntimeAutodiscovery()));
				}
			} catch (ConfigFetchException e) {
				// do nothing
			} catch (PluginNotFoundException e) {
				// do nothing
			} catch (EncodingException e) {
				log.error(e);
			} catch (PluginException e) {
				log.error(e);
			}

			List<ConfigValues> uiMonitorOptions = ActionUtils.getConfigValues(
					config, oldResponse);

			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS,
					uiMonitorOptions);
			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
					new Integer(uiMonitorOptions.size()));

			config = new ConfigSchema();
			oldResponse = new ConfigResponse();

			try {
				oldResponse = productBoss.getMergedConfigResponse(sessionId,
						ProductPlugin.TYPE_CONTROL, entityId, false);

				config = productBoss.getConfigSchema(sessionId, entityId,
						ProductPlugin.TYPE_CONTROL, oldResponse);
			} catch (ConfigFetchException e) {
				// do nothing
			} catch (PluginNotFoundException e) {
				// do nothing
			} catch (EncodingException e) {
				log.error(e);
			} catch (PluginException e) {
				log.error(e);
			}

			List<ConfigValues> uiControlOptions = ActionUtils.getConfigValues(
					config, oldResponse);

			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS,
					uiControlOptions);
			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT,
					new Integer(uiControlOptions.size()));
			request.setAttribute(Constants.AUTO_INVENTORY,
					new Boolean(server.getRuntimeAutodiscovery()));

			request.setAttribute(Constants.EDIT_CONFIG, new Boolean(
					platformWithAgent));
			if (!platformWithAgent)
				addActionError(getText(
						"resource.common.inventory.error.noAgent", "noAgent"));

			setConfigModifier(request, entityId);
			return;

		} catch (ApplicationException e) {
			addActionError(getText("resource.common.inventory.error.configRetrieveError"));
			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS,
					new ArrayList());
			request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
					new Integer(0));
			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS,
					new ArrayList());
			request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
					new Integer(0));
			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS,
					new ArrayList());
			request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT,
					new Integer(0));
			return;
		} catch (ServletException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		} catch (EncodingException e) {
			log.error(e);
		} catch (PluginException e) {
			log.error(e);
		}

	};

}
