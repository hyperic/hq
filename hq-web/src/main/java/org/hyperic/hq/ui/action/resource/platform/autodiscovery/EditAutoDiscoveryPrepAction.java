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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ScanMethodConfig;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

public class EditAutoDiscoveryPrepAction
    extends NewAutoDiscoveryPrepAction {

    private static ThreadLocal<AIScheduleValue> schedule = new ThreadLocal<AIScheduleValue>();
    
    @Autowired
    public EditAutoDiscoveryPrepAction(AppdefBoss appdefBoss, AIBoss aiBoss) {
        super(appdefBoss, aiBoss);
    }

    /**
     * Create the platform with the attributes specified in the given
     * <code>PlatformForm</code>.
     */
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {
        PlatformAutoDiscoveryForm aForm = (PlatformAutoDiscoveryForm) form;

        loadScanConfig(aForm, request);

        return super.workflow(context, mapping, form, request, response);
    }

    /**
     * loads the ScanConfiguration into the form
     */
    protected void loadScanConfig(PlatformAutoDiscoveryForm aForm, HttpServletRequest request) throws Exception {
        AIScheduleValue sched =  schedule.get();
        ScanMethodConfig[] configs = sched.getConfigObj().getScanMethodConfigs();

        if (configs.length == 0)
            throw new RuntimeException("No ScanMethodConfig objects setup");

        aForm.setName(sched.getScanName());
        aForm.setDescription(sched.getScanDesc());
        String smClass = configs[0].getMethodClass();
        ScanMethod scanMethod = (ScanMethod) Class.forName(smClass).newInstance();
        ConfigSchema schema = scanMethod.getConfigSchema();

        HttpSession session = request.getSession();
        session.setAttribute(Constants.CURR_CONFIG_SCHEMA, schema);
        session.setAttribute(Constants.OLD_CONFIG_RESPONSE, configs[0].getConfig());

        aForm.buildConfigOptions(scanMethod.getConfigSchema(), configs[0].getConfig());
        aForm.setScanMethod(scanMethod.getName());
        // load the schedule
        aForm.populateFromSchedule(sched.getScheduleValue(), request.getLocale());

        PlatformValue pValue = (PlatformValue) RequestUtils.getResource(request);

        Integer sessionId = RequestUtils.getSessionId(request);
        
        List<ServerTypeValue> serverTypes = appdefBoss.findServerTypesByPlatformType(sessionId.intValue(), pValue.getPlatformType().getId(),
            PageControl.PAGE_ALL);
        List<AppdefResourceTypeValue> selSvrs = buildSelectedServerTypes(serverTypes, sched.getConfigObj().getServerSignatures());
        Integer[] selSvrIds = new Integer[selSvrs.size()];
        int i = 0;
        for (Iterator<AppdefResourceTypeValue> it = selSvrs.iterator(); it.hasNext(); i++) {
            ServerTypeValue stv = (ServerTypeValue) it.next();
            selSvrIds[i] = stv.getId();
        }
        aForm.setSelectedServerTypeIds(selSvrIds);
    }

    /**
     * Get the selected server types.
     */
    private List<AppdefResourceTypeValue> buildSelectedServerTypes(List<ServerTypeValue> serverTypes, ServerSignature[] serverSigs) throws Exception {
        List<ServerSignature> serverDetectorList = new ArrayList<ServerSignature>();
        CollectionUtils.addAll(serverDetectorList, serverSigs);

        ServerTypeValue[] types = serverTypes.toArray(new ServerTypeValue[0]);

        return BizappUtils.buildServerTypesFromServerSig(types, serverDetectorList.iterator());
    }
}
