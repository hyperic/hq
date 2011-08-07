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

package org.hyperic.hq.plugin.cloudfoundry.control;

import java.net.MalformedURLException;
import java.util.Properties;

import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryFactory;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudApplicationControlPlugin extends ControlPlugin {

    private static final Log _log = LogFactory.getLog(CloudApplicationControlPlugin.class);

    private Properties _props;

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);
        _props = config.toProperties();
    }

    private String getAppName() {
        return _props.getProperty("resource.name");
    }
    
    public void doAction(String action, String[] args)
        throws PluginException {

        setResult(ControlPlugin.RESULT_FAILURE);

        try {
            CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(_props);
            
            if (cf == null) {
            	setMessage("Login unsuccessful");
                setResult(ControlPlugin.RESULT_FAILURE);
                return;
            }
            
            if (action.equals("start")) {
            	cf.startApplication(getAppName());
            } else if (action.equals("stop")) {
                cf.stopApplication(getAppName());
            } else if (action.equals("restart")) {
                cf.restartApplication(getAppName());
            } else if (action.equals("memory")) {
                if (args.length < 1) {
                    throw new PluginException("Usage: memory reserveration");
                }
                String memory = args[0];
            	cf.updateApplicationMemory(getAppName(), Integer.parseInt(memory));
            } else if (action.equals("instances")) {
                if (args.length < 1) {
                    throw new PluginException("Usage: number of instances");
                }
                String instances = args[0];
            	cf.updateApplicationInstances(getAppName(), Integer.parseInt(instances));
            } else if (action.equals("scaleUp")) {
            	CloudApplication app = cf.getApplication(getAppName());
            	int newCount = app.getInstances() + 1;
            	cf.updateApplicationInstances(getAppName(), newCount);            	
            } else if (action.equals("scaleDown")) {
            	CloudApplication app = cf.getApplication(getAppName());
            	int newCount = app.getInstances() - 1;
            	if (newCount >= 1) {
                	cf.updateApplicationInstances(getAppName(), newCount);            		
            	} else {
                    throw new PluginException("Cannot scale down to " + newCount + " app instances");            		
            	}
            } else {
                throw new PluginException("Unsupported action: " + action);
            }

            // TODO: need to check status of action
        	CloudApplication app = cf.getApplication(getAppName());
        	setMessage(app.getState().toString());
        	setResult(ControlPlugin.RESULT_SUCCESS);
        } catch (PluginException e) {
            setMessage(e.getMessage());
            throw e;
        } catch (Exception e) {
            setMessage(e.getMessage());
            throw new PluginException(action 
                                      + " [appName=" + getAppName()
                                      + "]: " + e.getMessage(), e);
        } finally  {
            //
        }
    }
}
