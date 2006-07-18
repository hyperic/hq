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

package org.hyperic.hq.plugin.websphere.wscp;

import java.util.ArrayList;

import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.websphere.WebsphereControlPlugin;

import org.hyperic.util.config.ConfigResponse;

/**
 * Base class for implementing control via wscp.
 */
public abstract class WscpControl extends WebsphereControlPlugin {

    private WebsphereController controller;
    private WebsphereCommand cmd;
    private String action;

    protected String getDefaultScript() {
        return WebsphereController.WSCP;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        this.controller =
            new WebsphereController(getInstallPrefix(),
                                    getAdminHost(),
                                    getAdminPort());

        this.controller.setWscp(getControlProgram());

        this.cmd = getCommand(config);
    }

    protected abstract WebsphereCommand getCommand(ConfigResponse config);

    public String[] getCommandArgs() {
        ArrayList args = this.controller.getArgs(this.cmd, this.action);
        return (String[])args.toArray(new String[0]);
    }

    public int doCommand(String action) {
        String script = getControlProgram();

        //XXX ugly
        this.action = action;

        int res = super.doCommand(script, new String[0]);
        setResult(res);
        return getResult(); //setResult may have changed it
    }

    public void setResult(int result) {
        super.setResult(result);
        if (result != RESULT_SUCCESS) {
            return;
        }

        String message = getMessage().trim();

        if (message.length() > 0) {
            //ARGH: wscp always exits with status 0 even
            //if there is an error and prints it to stdout
            //starting with the error code, like so:
            //WSCP0061E: Object not found :

            if (message.startsWith("WSCP")) {
                super.setResult(RESULT_FAILURE);
            }
        }
    }
}
