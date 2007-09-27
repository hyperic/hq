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

package org.hyperic.hq.product.jmx;

import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class MxServerControlPlugin extends ServerControlPlugin {

    private String objectName;
    private List actions;

    private static final List ACTIONS =
        Arrays.asList(new String[] { "start", "stop", "restart" });

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        this.actions = super.getActions();
        if (this.actions.size() == 0) {
            this.actions = ACTIONS;
        }
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        this.objectName =
            MxControlPlugin.configureObjectName(this);
    }

    public List getActions() {
        return this.actions;
    }

    protected boolean isBackgroundCommand() {
        return false;
    }

    protected String[] getArgs(String action) {
        String[] args = new String[] { action };
        String cmdline = getTypeProperty(action + ".args");

        if (cmdline != null) {
            cmdline = Metric.translate(cmdline, getConfig());
            String[] opts = StringUtil.explodeQuoted(cmdline);
            return (String[])ArrayUtil.combine(args, opts);
        }
        else {
            return args;
        }
    }

    protected String getObjectName() {
        return this.objectName;
    }

    protected int invokeMethod(String objectName,
                               String operation,
                               String[] args) {

        MxControlPlugin.invokeMethod(this, objectName,
                                     operation, args);
        return getResult();
    }

    protected int invokeMethod(String objectName, String operation) {
        return invokeMethod(objectName, operation, new String[0]);
    }

    protected int invokeMethod(String operation) {
        return invokeMethod(getObjectName(), operation);
    }

    protected int invokeMethod(String operation, String[] args) {
        return invokeMethod(getObjectName(), operation, args);
    }

    public void doAction(String action, String[] args)
        throws PluginException {

        //XXX stop may or may not be a jmx operation
        if (action.equals("start")) {
            start();
        }
        else if (action.equals("restart")) {
            restart();
        }
        else {
            invokeMethod(getObjectName(), action, args);
        }
    }

    public int start() {
        return doCommand(getArgs("start"));
    }

    public int stop() {
        return doCommand(getArgs("stop"));
    }

    public int restart() {
        stop();
        return start();
    }
}
