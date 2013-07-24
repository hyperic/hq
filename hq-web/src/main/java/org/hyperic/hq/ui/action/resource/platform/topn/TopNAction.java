package org.hyperic.hq.ui.action.resource.platform.topn;
/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.plugin.system.TopReport.TOPN_SORT_TYPE;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This action class is used by the TopN Popup screen. It's main use
 * is to generate the JSON objects required for display into the UI.
 */
public class TopNAction extends BaseAction {

    private final Log log = LogFactory.getLog(TopNAction.class.getName());
    private final AuthzBoss authzBoss;
    private final MeasurementBoss measurementBoss;
    private final AppdefBoss appdefBoss;
    private final DataManager dataManager;
    private final ResourceManager resourceManager;

    @Autowired
    public TopNAction(AuthzBoss authzBoss, MeasurementBoss measurementBoss, AppdefBoss appdefBoss,
                      DataManager dataManager, ResourceManager resourceManager) {
        super();
        this.authzBoss = authzBoss;
        this.measurementBoss = measurementBoss;
        this.appdefBoss = appdefBoss;
        this.dataManager = dataManager;
        this.resourceManager = resourceManager;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        AppdefEntityID eid = RequestUtils.getEntityId(request);
        String time;
        long ts = System.currentTimeMillis();
        try {
            time = RequestUtils.getStringParameter(request, "time");
        } catch (ParameterNotFoundException e) {
            return null;
        }

        long longTime = new SimpleDateFormat("MM/dd/yyyy hh:mm aa").parse(time).getTime();
        int rid = resourceManager.findResource(eid).getId();
        String data = dataManager.getTopNDataAsString(rid, longTime, TOPN_SORT_TYPE.CPU);
        if(data == null){
            data = "Data unavailable";
        }
        JSONObject topN = new JSONObject();
        topN.put("topn", data);

        response.getWriter().write(topN.toString());
        return null;
    }
}