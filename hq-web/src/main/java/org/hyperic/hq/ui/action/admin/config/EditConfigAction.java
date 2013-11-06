/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.admin.config;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.vm.VCConfig;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class EditConfigAction
extends BaseAction {

    private final Log log = LogFactory.getLog(EditConfigAction.class.getName());
    private ConfigBoss configBoss;
    private UpdateBoss updateBoss;
    private VCManager vcManager;
    private SessionManager sessionManager;
    private PlatformManager platformMgr;

    @Autowired
    public EditConfigAction(ConfigBoss configBoss, UpdateBoss updateBoss, VCManager vcManager, SessionManager sessionManager, PlatformManager platformMgr) {
        super();
        this.configBoss = configBoss;
        this.updateBoss = updateBoss;
        this.vcManager = vcManager;
        this.sessionManager = sessionManager;
        this.platformMgr = platformMgr;
    }

    /**
     * Create the cam config with the attributes specified in the given
     * <code>ConfigForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        int sessionId = RequestUtils.getSessionIdInt(request);
        SystemConfigForm cForm = (SystemConfigForm) form;
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        if (cForm.isOkClicked()) {
            if (log.isTraceEnabled())
                log.trace("Getting config");
            if (!cForm.getvCenterURL().isEmpty() && !cForm.getvCenterUser().isEmpty() && !cForm.getvCenterPassword().isEmpty()) {
                handleVCenterSettings(cForm, subject);
            }
            Properties props = cForm.saveConfigProperties(configBoss.getConfig());

            if (log.isTraceEnabled())
                log.trace("Setting config");
            configBoss.setConfig(sessionId, props);

            if (log.isTraceEnabled())
                log.trace("Restarting config service");
            //TODO this previously delegated to removed ProductConfigServiceImpl.restartConfig which registered/unregistered JDBC, LDAP, and/or Kerberos LoginModules
            //configBoss.restartConfig();

            // Set the update mode

            updateBoss.setUpdateMode(sessionId, UpdateStatusMode.findByCode(cForm.getUpdateMode()));
        }

        RequestUtils.setConfirmation(request, "admin.config.confirm.saveSettings");
        return returnSuccess(request, mapping);
    }

    private void handleVCenterSettings(SystemConfigForm cForm, AuthzSubject subject) {    
        try {
            VCConfig vc = vcManager.getVCConfigSetByUI();
            
            if(null != vc) {
                vc.setUrl(cForm.getvCenterURL());
                vc.setUser(cForm.getvCenterUser());
                if (!ConfigResponse.CONCEALED_SECRET_VALUE.equals(cForm.getvCenterPassword())) {
                    vc.setPassword( cForm.getvCenterPassword());
                }
                vcManager.updateVCConfig(vc);
            }
            else {
                vcManager.addVCConfig(cForm.getvCenterURL(), cForm.getvCenterUser(), cForm.getvCenterPassword(), true);
            }
        }catch(Exception e) {
            log.error(e,e);
        }
    }
}
