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
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.util.config.ConfigResponse;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Autowired;

public class EditConfigPrepAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(EditConfigPrepAction.class.getName());
    private ConfigBoss configBoss;
    private UpdateBoss updateBoss;
    private VCManager vcManager;

    @Autowired
    public EditConfigPrepAction(ConfigBoss configBoss, UpdateBoss updateBoss, VCManager vcManager) {
        super();
        this.configBoss = configBoss;
        this.updateBoss = updateBoss;
        this.vcManager = vcManager;
    }

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        SystemConfigForm cForm = (SystemConfigForm) form;

        if (log.isTraceEnabled()) {
            log.trace("getting config");
        }

        Properties props = configBoss.getConfig();
        cForm.loadConfigProperties(props);
        cForm.loadVCProps(vcManager.getVCConfigSetByUI());

        // Set the update mode
        UpdateStatusMode upMode = updateBoss.getUpdateMode();
        cForm.setUpdateMode(upMode.getCode());
        
        // Set the HQ SNMP local engine id
        String localEngineID = "0x" + new OctetString(MPv3.createLocalEngineID());
        request.setAttribute(Constants.SNMP_LOCAL_ENGINE_ID, localEngineID);
        
        // set "#CONCEALED_SECRET_VALUE#" to be returned to the ui
        String vCenterPassword = cForm.getvCenterPassword();
        if ((vCenterPassword!=null) && !vCenterPassword.equals("")) {
            cForm.setvCenterPassword(ConfigResponse.CONCEALED_SECRET_VALUE);
        }
        return null;
    }
}
