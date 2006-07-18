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

package org.hyperic.hq.plugin.jboss;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;

//This plugin uses the StateString attribute
//to determine if a service is running or not
//so it can throw an error on start() if already
//started and same for stop() if already stopped.
public class JBossStateServiceControlPlugin
    extends JBossServiceControlPlugin {

    protected boolean isRunning() {
        Metric metric = getConfiguredMetric();
        Object value;

        try {
            value = JBossUtil.getRemoteMBeanValue(metric);
            if (value == null) {
                return false;
            }
        } catch (Exception e) {
            getLog().debug("isRunning: " + e.getMessage(), e);
            return false;
        }
        
        if (metric.getAttributeName().equals(DEFAULT_ATTRIBUTE)) {
            return value.equals("Started");
        }

        //for service extensions assume if we can get the value
        //its running.
        return true;
    }

    public void doAction(String action, String[] args)
        throws PluginException {

        if (args.length == 0) {
            if (action.equals("start")) {
                start();
                return;
            }
            else if (action.equals("stop")) {
                stop();
                return;
            }
            else if (action.equals("restart")) {
                restart();
                return;
            }
        }

        super.doAction(action, args);
    }

    private void handleResult(int result, String stateWanted) {
        // don't bother waiting for the desired state if the startup
        // script does not return 0.
        if (result != RESULT_SUCCESS) {
            setResult(result);

            return;
        }

        String state = waitForState(stateWanted);

        if (!state.equals(stateWanted)) {
            setResult(RESULT_FAILURE);
            setMessage("service still in state " + state);
        }
        else {
            setResult(result);
        }
    }

    // control methods

    private void start() {
        if (isRunning()) {
            setResult(RESULT_FAILURE);
            setMessage("Service already started");
            return;
        }
        invokeMethod("start");

        handleResult(getResult(), STATE_STARTED);
    }

    private void stop() {
        if (!isRunning()) {
            setResult(RESULT_FAILURE);
            setMessage("Service already stopped");
            return;
        }

        invokeMethod("stop");

        handleResult(getResult(), STATE_STOPPED);
    }

    private void restart() {
        boolean hadToStop = false;
        if (isRunning()) {
            hadToStop = true;
            stop();
        }

        if (!hadToStop || (getResult() == RESULT_SUCCESS)) {
            start();
        }
    }
}
