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

package org.hyperic.hq.plugin.websphere.ejs;

import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;
import org.hyperic.hq.plugin.websphere.wscp.WebsphereCommand;

import org.hyperic.util.config.ConfigResponse;

public abstract class WebsphereRemoteControl
    extends ControlPlugin {

    private WebsphereRemote remote = null;
    private WebsphereCommand cmd;

    private static final String actions[] = { "start", "stop", "restart" };
    private static final List commands    = Arrays.asList(actions);

    public WebsphereRemoteControl() {
        super();
        setName(WebsphereProductPlugin.NAME);
        //give waitForState enough time
        setTimeout(DEFAULT_TIMEOUT * 10);
    }

    protected abstract WebsphereCommand getCommand(ConfigResponse config);

    private WebsphereRemote getRemote()
        throws PluginException {

        if (this.remote == null) {
            this.remote =
                WebsphereRemote.getInstance(getAdminHost(),
                                            getAdminPort());
        }

        return this.remote;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        this.cmd = getCommand(config);
    }

    protected String getAdminHost() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_ADMIN_HOST);
    }

    protected String getAdminPort() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_ADMIN_PORT);
    }

    protected String getServerNode() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_SERVER_NODE);
    }

    protected String getServerName() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_SERVER_NAME);
    }

    protected String getApplicationName() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_APP_NAME);
    }

    protected String getWebappName() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_WEBAPP_NAME);
    }

    protected String getEjbName() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_EJB_NAME);
    }

    protected boolean isRunning() { 
        try {
            return getRemote().isRunning(this.cmd);
        } catch (PluginException e) {
            getLog().error(e.getMessage(), e);
            return false;
        }
    }

    public int checkIsRunning(String action) {
        if (action.equals("start")) {
            if (isRunning()) {
                setMessage(this.cmd.getFullName() + " already running");
                return RESULT_FAILURE;
            }
        }
        else if (action.equals("stop")) {
            if (!isRunning()) {
                setMessage(this.cmd.getFullName() + " not running");
                return RESULT_FAILURE;
            }
        }

        return RESULT_SUCCESS;
    }

    public List getActions() {
        return commands;
    }

    public void doAction(String action)
        throws PluginException
    {
        int res = checkIsRunning(action);
        if (res != RESULT_SUCCESS) {
            setResult(res);
            return;
        }

        if (action.equals("start")) {
            start();
        }
        else if (action.equals("stop")) {
            stop();
        }
        else if (action.equals("restart")) {
            restart();
        }
        else {
            throw new PluginException("Action '" + action + 
                                      "' not supported");
        }
    }

    private void remoteAction(String action) {
        try {
            getRemote().doAction(this, this.cmd, action);
        } catch (PluginException e) {
            getLog().error(e.getMessage(), e);
            setResult(RESULT_FAILURE);
            setMessage(e.getMessage());
        }
    }

    private void start() {

        remoteAction("start");

        if (getResult() == RESULT_SUCCESS) {
            waitForState(STATE_STARTED);
        }
    }

    private void stop() {

        remoteAction("stop");

        if (getResult() == RESULT_SUCCESS) {
            waitForState(STATE_STOPPED);
        }
    }

    private void restart() {
        stop();
        if (getResult() == RESULT_SUCCESS) {
            start();
        }
    }
}
