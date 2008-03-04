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

package org.hyperic.hq.bizapp.server.action.integrate;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.hqu.rendit.RenditServer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.pager.PageControl;

public class OpenNMSAction implements ActionInterface {
    private static final String SERVER = "server";
    private static final String IP     = "ip";
    private static final String PORT   = "port";

    private static Log _log = LogFactory.getLog(OpenNMSAction.class);
    private static boolean _loaded = false;
    
    private String _server;
    private String _ip;
    private String _port = "5817";

    public OpenNMSAction() {
        _loaded = true;
    }
    
    public static Boolean isLoaded() {
        return new Boolean(_loaded);
    }

    public String execute(AlertInterface alert, ActionExecutionInfo info)
        throws ActionExecuteException {
        AlertDefinitionInterface alertdef =
            alert.getAlertDefinitionInterface();

        Resource resource = alertdef.getResource();
        
        Map params = new HashMap();
        
        params.put("alertDef", alertdef);
        params.put("alert", alert);
        params.put("action", info);
        params.put("resource", resource);
        params.put("host", _server);
        params.put("ip", _ip);
        params.put("port", _port);

        // Look up the platform
        AppdefEntityID aeid = new AppdefEntityID(alertdef.getAppdefType(),
                                                 alertdef.getAppdefId());
        AppdefEntityValue arv =
            new AppdefEntityValue(aeid, AuthzSubjectManagerEJBImpl.getOne()
                                          .getOverlordPojo());
        try {
            List platforms = arv.getAssociatedPlatforms(PageControl.PAGE_ALL);
            params.put("platform", platforms.get(0));
        } catch (AppdefEntityNotFoundException e) {
            params.put("platform", null);
        } catch (PermissionException e) {
            // Should never happen
            _log.error("Overlord does not have permission to look up " +
                       "associated platform for " + aeid);
            params.put("platform", null);
        }
        
        return renderTemplate("opennms_notify.gsp", params);
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
        throws InvalidActionDataException {
        init(config);
    }

    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        response.setValue(SERVER, _server);
        
        if (_ip != null && _ip.length() > 0)
            response.setValue(IP, _ip);
        
        response.setValue(PORT, _port);
        
        return response;
    }

    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();

        // Server
        StringConfigOption server =
            new StringConfigOption(SERVER, "OpenNMS Server", "localhost");
        server.setMinLength(1);
        server.setOptional(false);
        res.addOption(server);

        // IP
        StringConfigOption ip =
            new StringConfigOption(IP, "OpenNMS IP", "127.0.0.1");
        ip.setMinLength(7);
        ip.setOptional(true);
        res.addOption(ip);

        // Port
        StringConfigOption port =
            new StringConfigOption(PORT, "OpenNMS Port", "5817");
        port.setMinLength(1);
        port.setOptional(false);
        res.addOption(port);

        return res;
    }

    public String getImplementor() {
        return getClass().getName();
    }

    public void init(ConfigResponse config)
        throws InvalidActionDataException {
        _server = config.getValue(SERVER);
        _ip = config.getValue(IP);
        _port = config.getValue(PORT);        
    }

    public void setImplementor(String implementor) {}

    private String renderTemplate(String filename, Map params) {
        File templateDir = new File(HQApp.getInstance().getResourceDir(),
                                    "alertTemplates");
        File templateFile = new File(templateDir, filename);
        StringWriter output = new StringWriter();
        try {
            RenditServer.getInstance().renderTemplate(templateFile, params, 
                                                      output);
            
            if (_log.isDebugEnabled())
                _log.debug("Template rendered\n" + output.toString());
        } catch(Exception e) {
            _log.warn("Unable to render template", e);
        }
        return output.toString();
    }
    
    public String getServer() {
        return _server;
    }
    
    public void setServer(String server) {
        _server = server;
    }

    public String getIp() {
        return _ip;
    }

    public void setIp(String ip) {
        _ip = ip;
    }

    public String getPort() {
        return _port;
    }

    public void setPort(String port) {
        // Keep default unless explicitly change
        if (port != null && port.length() > 0)
            _port = port;
    }
}
