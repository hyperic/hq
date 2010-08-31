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

package org.hyperic.hq.product;

import java.util.Arrays;
import java.util.List;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for control plugins.
 */
public class Win32ControlPlugin extends ControlPlugin {

    public static final String PROP_SERVICENAME = "service_name";

    protected Log log;

    protected String serviceName = null;
    protected String installPrefix = null;
    protected Service svc;

    private static final String actions[] = { "start", "stop", "restart" };
    private static final List commands    = Arrays.asList(actions);

    public Win32ControlPlugin() {
        this.log = LogFactory.getLog(this.getClass().getName());
    }

    protected Log getLog() {
        return this.log;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String val) {
        this.serviceName = val;
    }

    protected boolean isServiceRequired() {
        return true;
    }

    public String getInstallPrefix() {
        return this.installPrefix;
    }

    public void setInstallPrefix(String val) {
        this.installPrefix = val;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        String val;
        
        // Check to see if the plugin defined the service name.  If not,
        // then the plugin must have queried the user, so we will have to
        // check the configResponse.
        if ((val = getServiceName()) == null) {
            val = config.getValue(Win32ControlPlugin.PROP_SERVICENAME);
            if (val != null) {
                setServiceName(val);
            }
        }

        val = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        if (val != null) {
            setInstallPrefix(val);
        }

        try {
            if (getServiceName() == null) {
                //XXX sigar should do this check
                throw new Win32Exception("Service name cannot be null");
            }
            svc = new Service(getServiceName());
        } catch (Win32Exception e) {
            String msg =
                "Could not open Windows Service: " +
                getServiceName();
            if (isServiceRequired()) {
                throw new PluginException(msg);
            }
            else {
                log.debug(msg);
            }
        }
    }

    protected boolean isRunning() {
        String resp = detectState();
        // This is kind of bogus, but Windows service's don't match
        // exactly to our model, so anything other than STOPPED or
        // UNKNOWN is running.
        if (resp.equals(STATE_STARTED) ||
            resp.equals(STATE_STARTING) ||
            resp.equals(STATE_STOPPING)) {
            return true;
        } else {
            return false;
        }
    }

    // We can't really return RESTARTING for a Windows service.  The
    // problem is that all services have a different way of restarting,
    // and it isn't clear that the Service Manager return codes are
    // extensible.  If a Windows service can return RESTARTING, it will
    // have to overload this method.  To take Apache as an example, 
    // during a restart Apache will return START_PENDING to the service
    // manager, so detectState will return STATE_STARTING.
    protected String detectState() {
        switch (svc.getStatus()) {
            case Service.SERVICE_START_PENDING:
                return STATE_STARTING;
            case Service.SERVICE_STOPPED:
                return STATE_STOPPED;
            case Service.SERVICE_RUNNING:
                return STATE_STARTED;
            case Service.SERVICE_STOP_PENDING:
                return STATE_STOPPING;
            case Service.SERVICE_CONTINUE_PENDING:
            case Service.SERVICE_PAUSE_PENDING:
            case Service.SERVICE_PAUSED:
                return STATE_UNKNOWN;
        }
        return STATE_UNKNOWN;
    }

    protected String waitForState(String wantedState)
    {
        int timeout = getTimeoutMillis();
        long timeStart = System.currentTimeMillis();
        String state = detectState();

        while (!state.equals(wantedState) &&
               (System.currentTimeMillis() - timeStart) < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
            state = detectState();
        }

        return state;
    }

    public List getActions() {
        return commands;
    }

    public void doAction(String action)
        throws PluginException
    {
        try {
            if (action.equals("start")) {
                svc.start();
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("stop")) {
                svc.stop();
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("restart")) {
                svc.stop((long)getTimeoutMillis());
                svc.start();
                setResult(RESULT_SUCCESS);
                return;
            }
        } catch (Win32Exception e) {
            setResult(RESULT_FAILURE);
            throw new PluginException(action + " " + getServiceName() +
                                      " failed: " + e.getMessage());
        }
        throw new PluginException("Action '" + action +
                                  "' not supported");
    }
}
