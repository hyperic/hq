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
package org.hyperic.hq.plugin.websphere;

import java.util.Arrays;
import java.util.List;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.util.InetPortPinger;
import org.hyperic.util.config.ConfigResponse;

public class WebsphereControlPlugin extends ServerControlPlugin {

    private static final String actions[] = {"start", "stop", "restart"};
    private static final List commands = Arrays.asList(actions);
    private InetPortPinger portPinger;
    private String[] ctlArgs = new String[0];

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

    protected String getUsername() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_USERNAME);
    }

    protected String getPassword() {
        return getConfig().getValue(WebsphereProductPlugin.PROP_PASSWORD);
    }

    //for isRunning()
    protected String getRunningHost() {
        return getAdminHost();
    }

    protected String getRunningPort() {
        return getAdminPort();
    }

    @Override
    public void configure(ConfigResponse config)
            throws PluginException {
        super.configure(config);

        String username = getUsername();
        String password = getPassword();

        if ((username != null) && (password != null)) {
            this.ctlArgs = new String[]{
                getServerName(),
                "-username", username,
                "-password", password
            };
        } else {
            this.ctlArgs = new String[]{
                getServerName()
            };
        }

        try {
            int iport = Integer.parseInt(getRunningPort());
            this.portPinger = new InetPortPinger(getRunningHost(),
                    iport,
                    30000);
        } catch (NumberFormatException e) {
            //unlikely: already validated by ConfigSchema
        }
    }

    //XXX websphere has a status port we should try first
    //com.ibm.ws.management.tools.WsServerLauncher falls back
    //to similar code as below; it'll do for now.
    @Override
    protected boolean isRunning() {
        if (this.portPinger == null) {
            return false; //unlikely
        }

        return this.portPinger.check();
    }

    protected int checkIsRunning(String action) {
        if (action.equals("start")) {
            if (isRunning()) {
                setMessage("Server already running on port "
                        + getRunningPort());
                return RESULT_FAILURE;
            }
        } else if (action.equals("stop")) {
            if (!isRunning()) {
                setMessage("No server running on port "
                        + getRunningPort());
                return RESULT_FAILURE;
            }
        }

        return RESULT_SUCCESS;
    }

    @Override
    public List getActions() {
        return commands;
    }

    @Override
    public void doAction(String action, String[] args)
            throws PluginException {
        if (action.equals("start")) {
            setResult(start(args));
        } else if (action.equals("stop")) {
            setResult(stop(args));
        } else if (action.equals("restart")) {
            setResult(restart(args));
        } else {
            // Shouldn't happen
            throw new PluginException("Action '" + action
                    + "' not supported");
        }
    }

    @Override
    protected int doCommand(String action, String[] args) {
        String script = getConfig().getValue(PROP_PROGRAM + "." + action);
        setControlProgram(script);

        getLog().debug("command script=" + script);

        if ((args == null) || (args.length == 0)) {
            args = this.ctlArgs;
        }

        return super.doCommand(script, args);
    }

    // Define control methods
    private int start(String[] args) {
        int res = doCommand("start", args);

        if (res == RESULT_SUCCESS) {
            waitForState(STATE_STARTED);
        }

        return res;
    }

    private int stop(String[] args) {
        int res = doCommand("stop", args);

        if (res == RESULT_SUCCESS) {
            waitForState(STATE_STOPPED);
        }

        return res;
    }

    private int restart(String[] args) {
        int res = stop(args);
        if (res != RESULT_SUCCESS) {
            return res;
        }
        return start(args);
    }
}
