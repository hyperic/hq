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

package org.hyperic.hq.plugin.apache;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

public class ApacheControlPlugin 
    extends ServerControlPlugin {
    
    static final String DEFAULT_SCRIPT = "bin/apachectl";
    static final String DEFAULT_PIDFILE = "logs/httpd.pid";

    public ApacheControlPlugin() {
        super();
        setPidFile(DEFAULT_PIDFILE);
        setControlProgram(DEFAULT_SCRIPT);
    }

    public boolean useSigar() {
        return true;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        validateControlProgram(getTypeInfo().getName());
    }

    // Define control methods

    public void start()
    {
        doCommand("start");

        handleResult(STATE_STARTED);
    }

    // XXX: should we handle encrypted keys?
    public void startssl()
    {
        doCommand("startssl");

        handleResult(STATE_STARTED);
    }

    public void stop()
    {
        doCommand("stop");

        handleResult(STATE_STOPPED);
    }

    public void restart()
    {
        this.doCommand("restart");

        handleResult(STATE_STARTED);
    }

    public void graceful()
    {
        doCommand("graceful");
        
        handleResult(STATE_STARTED);
    }

    public void configtest()
    {
        // state does not change during configtest
        
        doCommand("configtest");
    }
}
