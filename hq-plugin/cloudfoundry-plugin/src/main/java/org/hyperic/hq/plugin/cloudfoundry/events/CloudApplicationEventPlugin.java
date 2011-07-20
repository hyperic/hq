/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 * 
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.plugin.cloudfoundry.events;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryProxy;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import org.cloudfoundry.client.lib.CrashInfo;
import org.cloudfoundry.client.lib.CrashesInfo;

public class CloudApplicationEventPlugin extends LogTrackPlugin implements Runnable {

    private static final long INTERVAL = 1000 * 60 * 5;
    private static final Log _log = LogFactory.getLog(CloudApplicationEventPlugin.class.getName());
    protected Properties _props;
    private long _lastCheck;

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        _props = config.toProperties();
        setup();
        getManager().addRunnableTracker(this);
    }

    private void setup() throws PluginException {
        _lastCheck = System.currentTimeMillis();
    }
    
    public void shutdown() throws PluginException {
        super.shutdown();
    }

    private void processEvents(List<CrashInfo> events) {        
    	for (CrashInfo crash : events) {    		
            long crashTime = crash.getSince().getTime();
            if (crashTime < _lastCheck) {
                continue;
            }
            reportEvent(crashTime,
                        //XXX how-to map log level?
                        LogTrackPlugin.LOGLEVEL_WARN,
                        "Application Crashed",
                        "Instance=" + crash.getInstance());
        }
    }

    private List<CrashInfo> getCrashes() throws PluginException {
    	List<CrashInfo> crashes = null;
        
    	try {        	
    		CloudFoundryProxy cf = new CloudFoundryProxy(_props);
        	
        	if (cf != null) {
        		String appName = getConfig("resource.name");
        		CrashesInfo info = cf.getCrashes(appName);
        		if (info != null) {
        			crashes = info.getCrashes();
        		}        		
        	}
        } catch (Exception e) {
            throw new PluginException("Could not get crash info: " + e.getMessage(), e);
        }
        
        return crashes;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public void run() {
        try {
            long now = now();
            // TODO: make interval configurable
            if ((now - _lastCheck) < INTERVAL) {
                //XXX checkForUpdates() api?
                return;
            }
            List<CrashInfo> crashes = getCrashes();
            processEvents(crashes);
            _lastCheck = now;
        } catch (PluginException e) {
            _log.error("checkForEvents: " + e, e);
        }
    }
}
