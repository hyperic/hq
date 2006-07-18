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

package org.hyperic.hq.plugin.jboss.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployment.SerializableDeploymentInfo;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

public class DataSourceQuery extends GenericServiceQuery {
    private static final String MAIN_DEPLOYER =
        "jboss.system:service=MainDeployer";

    private static final String LIST_DEPLOYED = "listDeployedModules";

    private static final Log log = LogFactory.getLog("DataSourceQuery");

    private static HashMap deployers = new HashMap();

    private String ds;

    public void getAttributes(RMIAdaptor mServer)
        throws PluginException {

        super.getAttributes(mServer);
        String err =
            "Failed to invoke " +
            MAIN_DEPLOYER + "." + LIST_DEPLOYED + ": ";

        try {
            HashMap descriptors = getDescriptors(mServer);
            String file = (String)descriptors.get(getName());
            this.ds = file;
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            log.error(err + e, e);
        } catch (VerifyError e) {
            log.error(err + e, e);
        }
    }

    public Properties getResourceConfig() {
        Properties config = super.getResourceConfig();
        if (this.ds != null) {
            config.setProperty("descriptor", this.ds);
        }
        return config;
    }

    public void initialize() {
        //called before queryMBeans
        super.initialize();
        deployers.clear();
    }

    private HashMap getDescriptors(RMIAdaptor mServer)
        throws PluginException {

        String url = getURL();
        HashMap deployer = (HashMap)deployers.get(url);
        if (deployer != null) {
            return deployer;
        }

        String serviceName = getProperty("SERVICE");
        String base = ((ServerQuery)getParent()).getServerURL();

        HashMap descriptors = new HashMap();
        StopWatch timer = new StopWatch();

        Collection deployed;

        try {
            ObjectName mainDeployer =
                new ObjectName(MAIN_DEPLOYER);

            deployed = (Collection)mServer.invoke(mainDeployer,
                                                  LIST_DEPLOYED,
                                                  new Object[0],
                                                  new String[0]);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        log.debug(url + ".listDeployed took: " + timer);

        for (Iterator it=deployed.iterator(); it.hasNext();) {
            SerializableDeploymentInfo info =
                (SerializableDeploymentInfo)it.next();
            String file = info.url.toString();
            if (!file.endsWith("-ds.xml")) {
                continue;
            }
            if (file.startsWith(base)) {
                file = file.substring(base.length(), file.length());
            }
            for (Iterator iter=info.mbeans.iterator(); iter.hasNext();) {
                ObjectName name = (ObjectName)iter.next();
                String service = name.getKeyProperty("service");
                if (!serviceName.equals(service)) {
                    continue;
                }
                String ds = name.getKeyProperty("name");
                descriptors.put(ds, file);
            }
        }
        
        deployers.put(url, descriptors);

        return descriptors;
    }
}
