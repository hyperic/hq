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

package org.hyperic.hq.plugin.mqseries;

import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;

public class MQSeriesServerControlPlugin
    extends ServerControlPlugin {

    private static final String DEFAULT_SCRIPT = "mqseries_control.sh";

    private static final String ACTIONS[]  = { "start", "stop", "restart" };
    private static final List COMMANDS     = Arrays.asList(ACTIONS);

    public MQSeriesServerControlPlugin() {
        super();
        setControlProgram(DEFAULT_SCRIPT);
    }

    public List getActions() {
        return COMMANDS;
    }

    public void doAction(String action)
        throws PluginException {

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
            // Shouldn't happen
            throw new PluginException("Action '" + action +
                                      "' not supported");
        }
    }

    // control methods

    private void start() {

        doCommand("start");

        handleResult(STATE_STARTED);
    }

    private void stop() {

        doCommand("stop");

        handleResult(STATE_STOPPED);
    }

    private void restart() {

        doCommand("restart");

        handleResult(STATE_STARTED);
    }
}
