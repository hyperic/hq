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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class will set the ConfigResponse for resources.This is applicable for
 * Platforms, Servers and Services.
 * 
 */
public class EditConfigPropertiesAction
    extends BaseAction {

    public static final String ERR_NOMSG = "resource.common.error.ConfigError.NoMessage";
    public static final String ERR_CONFIG = "resource.common.error.ConfigError";
    private final Log log = LogFactory.getLog(EditConfigPropertiesAction.class.getName());
    private AppdefBoss appdefBoss;
    private ProductBoss productBoss;

    @Autowired
    public EditConfigPropertiesAction(AppdefBoss appdefBoss, ProductBoss productBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.productBoss = productBoss;
    }

    /**
     * Edit the resource Configuration properties with
     * <code>ResourceConfigForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        ResourceConfigForm cfgForm = (ResourceConfigForm) form;
        AppdefEntityID aeid = new AppdefEntityID(cfgForm.getType().intValue(), cfgForm.getRid());

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                forwardParams.put(Constants.ACCORDION_PARAM, "5");
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                forwardParams.put(Constants.ACCORDION_PARAM, "3");
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                forwardParams.put(Constants.ACCORDION_PARAM, "2");
                break;
        }

        ActionForward forward = checkSubmit(request, mapping, form, forwardParams);
        AllConfigResponses allConfigs = new AllConfigResponses();
        AllConfigResponses allConfigsRollback = new AllConfigResponses();
        String prefix;
        if (forward != null) {
            // HACK to redirect back to Problem Resources portlet if the
            // request originated there.
            if (request.getParameter("todash") != null) {
                return mapping.findForward("dash");
            }
            return forward;
        }

        try {

            Integer sessionId = RequestUtils.getSessionId(request);
            int sessionInt = sessionId.intValue();

            String[] cfgTypes = ProductPlugin.CONFIGURABLE_TYPES;
            int i, numConfigs = cfgTypes.length;
            ConfigSchema[] schemas = new ConfigSchema[cfgTypes.length];
            ConfigResponse[] oldConfigs = new ConfigResponse[cfgTypes.length];
            ConfigResponse[] newConfigs = new ConfigResponse[cfgTypes.length];

            // Save the original resource
            allConfigs.setResource(aeid);
            allConfigsRollback.setResource(aeid);

            ConfigResponseDB oldConfig = productBoss.getConfigResponse(sessionInt, aeid);

            // get the configSchemas and existing configs
            for (i = 0; i < numConfigs; i++) {
                try {
                    byte[] oldCfgBytes = null;
                    if (cfgTypes[i].equals(ProductPlugin.TYPE_PRODUCT)) {
                        oldCfgBytes = oldConfig.getProductResponse();
                    } else if (cfgTypes[i].equals(ProductPlugin.TYPE_MEASUREMENT)) {
                        oldCfgBytes = oldConfig.getMeasurementResponse();
                    } else if (cfgTypes[i].equals(ProductPlugin.TYPE_CONTROL)) {		
                         oldCfgBytes = oldConfig.getControlResponse();
            			 if ( (aeid.isPlatform()) && (oldCfgBytes == null)) {
            				// in case of platform if there is no control config - control actions are not supported
            				// notice - empty control response is created for server/service (not too good - we rely that in this case getPlugin of control
            				// manager will return no supported - better not to create control plugin - in this case too.) -change for 6.0??
            				blankoutConfig(i, allConfigs, allConfigsRollback);
            				if (log.isDebugEnabled()) {
            				    log.debug("Blanking for " + cfgTypes[i] + "->(not supported)")	;
            				}
            				continue;
            			 }
                    } else if (cfgTypes[i].equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
                        oldCfgBytes = oldConfig.getResponseTimeResponse();
                    }

                    if (oldCfgBytes == null) {
                        oldConfigs[i] = new ConfigResponse();
                    } else {
                        oldConfigs[i] = ConfigResponse.decode(oldCfgBytes);
                    }

                    schemas[i] = productBoss.getConfigSchema(sessionInt, aeid, cfgTypes[i], oldConfigs[i]);
                    allConfigsRollback.setConfig(i, oldConfigs[i]);
                    allConfigsRollback.setSupports(i, true);
                    allConfigs.setSupports(i, true);
                } catch (PluginNotFoundException e) {
                    // No plugin support for this config type - skip ahead to
                    // the next type.
                    log.debug(cfgTypes[i] + " PluginNotFound for " + aeid + " : " + e);
                    log.debug("Blanking for " + cfgTypes[i] + "->(not supported)");
                    blankoutConfig(i, allConfigs, allConfigsRollback);
                }
            }

            AppdefResourceValue updatedResource = appdefBoss.findById(sessionInt, aeid);

            // get the new configs based on UI form fields
            for (i = 0; i < numConfigs; i++) {

                // Don't bother configuring things that aren't supported.
                if (!allConfigs.getSupports(i))
                    continue;

                // Load new configs from requestParams
                if (i == ProductPlugin.CFGTYPE_IDX_PRODUCT) {
                    prefix = ProductPlugin.TYPE_PRODUCT + ".";
                    newConfigs[i] = BizappUtils.buildSaveConfigOptions(prefix, request, new ConfigResponse(),
                        schemas[i], null);
                    // Make current form values appear in case of error.
                    cfgForm.setResourceConfigOptions(BizappUtils.buildLoadConfigOptions(prefix, schemas[i],
                        newConfigs[i]));
                    allConfigs.setShouldConfig(i, true);
                    allConfigs.setConfig(i, newConfigs[i]);
                    allConfigsRollback.setShouldConfig(i, true);

                } else {
                    if (i == ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME) {
                        if (aeid.isServer() || aeid.isPlatform()) {
                            // On servers, RT always gets an empty config
                            allConfigs.setConfig(i, new ConfigResponse());
                            allConfigs.setShouldConfig(i, true);
                            allConfigsRollback.setShouldConfig(i, true);

                        } else if (!aeid.isService() || !allConfigs.supportsMetricConfig()) {
                            // Otherwise, only configure response time for
                            // services, and only if the plugin has metric
                            // support.
                            blankoutConfig(i, allConfigs, allConfigsRollback);
                            continue;
                        }
                    }
                    if (i == ProductPlugin.CFGTYPE_IDX_CONTROL &&  !aeid.isService() && !aeid.isServer() && !aeid.isPlatform() ) {
                        // Control is only supported on platforms, servers and services
                        blankoutConfig(i, allConfigs, allConfigsRollback);
                        continue;
                    }

                    newConfigs[i] = new ConfigResponse(schemas[i]);
                    allConfigs.setConfig(i, newConfigs[i]);
                    prefix = cfgTypes[i] + ".";
                    Boolean wereChanges = BizappUtils.populateConfig(request, prefix, schemas[i], newConfigs[i],
                        oldConfigs[i]);
                    if (wereChanges == null) {
                        // This means the schema had no options, so the concept
                        // of changes doesn't make much sense. In these cases,
                        // we'll just keep the empty ConfigResponse
                        allConfigs.setShouldConfig(i, true);
                        allConfigsRollback.setShouldConfig(i, true);

                    } else if (wereChanges.equals(Boolean.TRUE)) {
                        // There were changes. Cool, we'll keep them.
                        allConfigs.setShouldConfig(i, true);
                        allConfigsRollback.setShouldConfig(i, true);

                    } else {
                        // There were no changes. We discard the config
                        // response. This way, when we do the server-side
                        // bizapp stuff, we don't re-configure things that
                        // have not changed.

                        // But AHA! There is a perverted edge case here: if
                        // this is a service and the config prop names for the
                        // service
                        // are the same as those for the server, and the service
                        // has
                        // never been configured, then we won't think anything
                        // has
                        // changed when in fact it has. See bug 8251.
                        boolean reallyWereChanges = false;
                        try {
                            productBoss.getMergedConfigResponse(sessionInt, cfgTypes[i], aeid, true);
                        } catch (ConfigFetchException cfe) {
                            // OK, so there really were changes because the
                            // config doesn't
                            // exist!
                            reallyWereChanges = true;
                            allConfigs.setShouldConfig(i, true);
                            allConfigsRollback.setShouldConfig(i, true);
                        }
                        if (!reallyWereChanges) {
                            allConfigs.setShouldConfig(i, false);
                            allConfigsRollback.setShouldConfig(i, false);
                        }
                    }

                    switch (i) {
                        case ProductPlugin.CFGTYPE_IDX_MEASUREMENT:
                            // If metric config was setup, then setup runtime AI
                            // flag.
                            if ((aeid.isServer() || aeid.isService()) && (allConfigs.getMetricConfig() != null)) {
                                boolean rollback, runtimeAI;
                                if (aeid.isServer()) {
                                    rollback = ((ServerValue) updatedResource).getRuntimeAutodiscovery();
                                    runtimeAI = cfgForm.getServerBasedAutoInventory();
                                } else {
                                    rollback = false;
                                    runtimeAI = true; // XXX
                                                      // !((ServiceValue)updatedResource).getWasAutodiscovered();
                                }
                                allConfigsRollback.setEnableRuntimeAIScan(rollback);
                                allConfigs.setEnableRuntimeAIScan(runtimeAI);
                            }
                            cfgForm.setMonitorConfigOptions(BizappUtils.buildLoadConfigOptions(prefix, schemas[i],
                                newConfigs[i]));
                            break;

                        case ProductPlugin.CFGTYPE_IDX_CONTROL:
                            cfgForm.setControlConfigOptions(BizappUtils.buildLoadConfigOptions(prefix, schemas[i],
                                newConfigs[i]));
                            break;

                        case ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME:
                            // enable the RT metrics based on input from ui
                            allConfigs.setEnableServiceRT(cfgForm.getServiceRTEnabled());
                            allConfigs.setEnableEuRT(cfgForm.getEuRTEnabled());
                            cfgForm.setRtConfigOptions(BizappUtils.buildLoadConfigOptions(prefix, schemas[i],
                                newConfigs[i]));
                            break;
                    }
                }
            }

            // call the uber setter in the AppdefBoss
            appdefBoss.setAllConfigResponses(sessionInt, allConfigs, allConfigsRollback);

            RequestUtils.setConfirmation(request, "resource." + aeid.getTypeName() +
                                                  ".inventory.confirm.EditConfigProperties", updatedResource.getName());

            // HACK to redirect back to Problem Resources portlet if the
            // request originated there.
            if (request.getParameter("todash") != null) {
                return mapping.findForward("dash");
            }
            return returnSuccess(request, mapping, forwardParams);
        } catch (InvalidConfigException e) {
            log.error("Invalid config " + e);
            RequestUtils.setErrorWithNullCheck(request, e, ERR_NOMSG, ERR_CONFIG);
            cfgForm.validationErrors = true;
            return returnFailure(request, mapping);
        } catch (InvalidOptionValueException e) {
            log.error("Invalid config " + e);
            RequestUtils.setErrorWithNullCheck(request, e, ERR_NOMSG, ERR_CONFIG);
            return returnFailure(request, mapping);
        } catch (ConfigFetchException e) {
            log.error("General configuration set error " + e, e);
            RequestUtils.setErrorWithNullCheck(request, e, ERR_NOMSG, ERR_CONFIG);
            cfgForm.validationErrors = true;
            return returnFailure(request, mapping);
        } catch (EncodingException e) {
            log.error("Encoding error " + e);
            RequestUtils.setErrorWithNullCheck(request, e, ERR_NOMSG, ERR_CONFIG);
            cfgForm.validationErrors = true;
            return returnFailure(request, mapping);
        } catch (PluginNotFoundException e) {
            log.error("Plugin not found " + e);
            RequestUtils.setErrorObject(request, "resource.common.inventory.error.PluginNotFound", e.getMessage());
            return returnFailure(request, mapping);
        } catch (PluginException e) {
            log.error("Exception occured in plugin " + e);
            RequestUtils.setErrorObject(request, "resource.common.inventory.error.agentNotReachable", e.getMessage());
            cfgForm.validationErrors = true;
            return returnFailure(request, mapping);
        }
    }

    private void blankoutConfig(int i, AllConfigResponses c1, AllConfigResponses c2) {
        c1.setConfig(i, null);
        c2.setConfig(i, null);
        c1.setSupports(i, false);
        c2.setSupports(i, false);
        c1.setShouldConfig(i, false);
        c2.setShouldConfig(i, false);
    }
}
