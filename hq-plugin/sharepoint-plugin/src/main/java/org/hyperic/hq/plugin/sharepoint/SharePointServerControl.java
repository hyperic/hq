/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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
package org.hyperic.hq.plugin.sharepoint;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

public class SharePointServerControl extends Win32ControlPlugin {

    List<String> services, webs;
    private Log log = LogFactory.getLog(SharePointServerDetectorDefault.class);

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        log.debug("[configure] config=" + config);

        String w = config.getValue(SharePoint.PROP_C_WEBS);
        if (w != null) {
            webs = Arrays.asList(w.split(","));
        } else {
            webs = new ArrayList<String>();
        }

        String s = config.getValue(SharePoint.PROP_C_SERVICES);
        if (s != null) {
            services = Arrays.asList(s.split(","));
        } else {
            services = new ArrayList<String>();
        }

        setTimeout(60);
    }

    @Override
    public List getActions() {
        return Arrays.asList("start", "stop", "restart");
    }

    @Override
    public void doAction(String action) throws PluginException {
        for (int i = 0; i < webs.size(); i++) {
            String web = webs.get(i).trim();
            if (action.equals("start")) {
                controlWebServer("start", web);
            } else if (action.equals("stop")) {
                controlWebServer("stop", web);
            } else if (action.equals("restart")) {
                controlWebServer("stop", web);
                controlWebServer("start", web);
            }
        }

        for (int i = 0; i < services.size(); i++) {
            String s = services.get(i).trim();
            if (action.equals("start")) {
                startService(s);
            } else if (action.equals("stop")) {
                stopService(s);
            } else if (action.equals("restart")) {
                stopService(s);
                startService(s);
            }
        }
        setResult(RESULT_SUCCESS);
    }

    private void stopService(String service) throws PluginException {
        try {
            Service s = new Service(service);
            log.debug("[stopService] stoping:" + s.getConfig().getDisplayName());
            if (isRunning(s)) {
                s.stop();
                waitForStop(s);
            }
            log.debug("[stopService] Done");
        } catch (Win32Exception ex) {
            throw new PluginException("Error stoping service '" + service + "'", ex);
        }
    }

    private void startService(String service) throws PluginException {
        try {
            Service s = new Service(service);
            log.debug("[startService] staring:" + s.getConfig().getDisplayName());
            if (!isRunning(s)) {
                s.start();
                waitForStart(s);
            }
            log.debug("[startService] Done");
        } catch (Win32Exception ex) {
            throw new PluginException("Error starting service '" + service + "'", ex);
        }
    }

    private void waitForStart(Service svc) throws PluginException, Win32Exception {
        int timeout = getTimeoutMillis();
        long timeStart = System.currentTimeMillis();

        while (!isRunning(svc) && (System.currentTimeMillis() - timeStart) < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        if (!isRunning(svc)) {
            throw new PluginException("Service '" + svc.getConfig().getDisplayName() + "' not stopped - Timeout");
        }
    }

    private void waitForStop(Service svc) throws PluginException, Win32Exception {
        int timeout = getTimeoutMillis();
        long timeStart = System.currentTimeMillis();

        while (isRunning(svc) && (System.currentTimeMillis() - timeStart) < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        if (isRunning(svc)) {
            throw new PluginException("Service '" + svc.getConfig().getDisplayName() + "' not stopped - Timeout");
        }
    }

    private void controlWebServer(String action, String webserver) throws PluginException {
        log.debug("[controlWebServer] action=" + action + ", webserver=" + webserver);
        String[] cmd = {IisMetaBase.APPCMD, action, "site", webserver};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecuteWatchdog wdog = new ExecuteWatchdog(60 * 1000);
        Execute exec = new Execute(new PumpStreamHandler(output), wdog);
        exec.setCommandline(cmd);
        try {
            int exitStatus = exec.execute();
            if (exitStatus != 0 || wdog.killedProcess()) {
                throw new PluginException("exitStatus:" + exitStatus + ", output=" + output.toString());
            }
            log.debug("[controlWebServer] action=" + action + ", webserver=" + webserver + ", output=" + output);
        } catch (Exception e) {
            log.debug("[controlWebServer] action=" + action + ", webserver=" + webserver + ", error=" + e.getMessage(), e);
            throw new PluginException(Arrays.asList(cmd) + ": " + e);
        }
    }
}
