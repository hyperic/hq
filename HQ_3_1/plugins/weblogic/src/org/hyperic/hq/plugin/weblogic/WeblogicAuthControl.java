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

package org.hyperic.hq.plugin.weblogic;

import java.security.PrivilegedAction;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;

/**
 * Helper for using JAAS in control.
 */
public class WeblogicAuthControl
    implements PrivilegedAction {

    private static final boolean useJAAS = WeblogicProductPlugin.useJAAS();

    private static final int RUN_ACTION = 1;
    private static final int RUN_STATUS = 2;

    private WeblogicAction plugin;
    private Metric metric;
    private String action;
    private int method;
    private boolean active = false;

    public WeblogicAuthControl(WeblogicAction plugin,
                               Metric metric) {
        this.plugin = plugin;
        this.metric = metric;
    }

    public Object run() {
        switch (this.method) {
          case RUN_ACTION:
            this.plugin.doWeblogicAction(this.action);
            break;
          case RUN_STATUS:
            if (this.plugin.isWeblogicRunning()) {
                return Boolean.TRUE;
            }
            else {
                return Boolean.FALSE;
            }
          default:
            //only programmar error can trigger this.
            throw new IllegalStateException("unknown run method: " + this.method);
            
        }

        return null;
    }

    public void doAction(String action) {
        if (useJAAS) {
            doActionAs(action);
        }
        else {
            this.plugin.doWeblogicAction(action);
        }
    }

    public boolean isRunning() {
        //active flag = true means we are being
        //called while already authenticated.
        //such as during doActionAs.
        //otherwise we are being polled for control state.
        if (!this.active && useJAAS) {
            return isRunningAs();
        }
        else {
            return this.plugin.isWeblogicRunning();
        }
    }

    public void doActionAs(String action) {
        WeblogicAuth auth = WeblogicAuth.getInstance(this.metric);

        this.active = true;
        this.action = action;
        this.method = RUN_ACTION;

        try {
            auth.runAs(this);
        } catch (SecurityException e) {
            ControlPlugin plugin = 
                (ControlPlugin)this.plugin;
            plugin.setResult(ControlPlugin.RESULT_FAILURE);
            plugin.setMessage(e.getMessage());

            //likely failed since we cant get a login
            //context if the server is stopped,
            //try again w/o a login context.
            if (action.equals("start")) {
                run();
            }
        } finally {
            this.active = false;
        }
    }

    public boolean isRunningAs() {
        Object obj;
        WeblogicAuth auth = WeblogicAuth.getInstance(this.metric);

        this.method = RUN_STATUS;

        try {
            obj = auth.runAs(this);
            return ((Boolean)obj).booleanValue();
        } catch (SecurityException e) {
            return false;
        }
    }
}
