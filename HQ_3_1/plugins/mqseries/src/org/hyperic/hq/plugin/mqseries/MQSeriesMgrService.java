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

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.sigar.win32.RegistryKey;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeBuilder;

import org.hyperic.util.StringUtil;

public class MQSeriesMgrService
    extends MQSeriesService {

    //XXX might need to unhardcode.
    public static final File QMGRS_PATH = new File("/var/mqm/qmgrs");

    private static final Log log =
        LogFactory.getLog(MQSeriesMgrService.class.getName());

    private String name;
    private String fullname;
    private String type;
    private String serverType;
    private Properties productConfig = new Properties();

    public MQSeriesMgrService() { }

    public String getTypeName() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getFullName() {
        return this.fullname;
    }

    public Map getProductConfig() {
        this.productConfig.setProperty(MQSeriesProductPlugin.PROP_MGR_NAME,
                                       getName());
        return this.productConfig;
    }

    public String getServerType() {
        return this.serverType;
    }

    private static String[] findQmgrs(File path) {
        log.debug("Scanning for queue managers in: " + path);

        File[] dirs = path.listFiles(new FileFilter() {
            public boolean accept(File dir) {
                if (dir.getName().equals("@SYSTEM")) {
                    return false;
                }
                if (dir.isDirectory() &&
                    new File(dir, "queues").exists())
                {
                    return true;
                }
                return false;
            }
        });

        String[] names = new String[dirs.length];

        for (int i=0; i<dirs.length; i++) {
            names[i] = dirs[i].getName();
        }

        return names;
    }

    private static String[] findQmgrsRegistry()
        throws PluginException {

        String MQ_MGR_KEY =
            MQSeriesProductPlugin.MQ_KEY +
            "\\Configuration\\QueueManager";
        RegistryKey key = null;

        try {
            key =
                RegistryKey.LocalMachine.openSubKey(MQ_MGR_KEY);
            String[] queues = key.getSubKeyNames();
            log.debug(MQ_MGR_KEY + " contains " + queues.length + " queues");
            return queues;
        } catch (Win32Exception e) {
            String msg = "Failed to open: " + MQ_MGR_KEY;
            throw new PluginException(msg, e);
        } finally {
            if (key != null) {
                key.close();
            }
        }
    }

    public static List findServices(String serverType)
        throws PluginException {

        ArrayList services = new ArrayList();
        String[] qmgrs;

        if (GenericPlugin.isWin32()) {
            qmgrs = findQmgrsRegistry();

            if (qmgrs.length == 0) {
                String path =
                    MQSeriesProductPlugin.getRegistryWorkPath();

                if (path != null) {
                    qmgrs = findQmgrs(new File(path, "Qmgrs"));
                }
            }
        }
        else {
            qmgrs = findQmgrs(QMGRS_PATH);
        }

        for (int i=0; i<qmgrs.length; i++) {
            MQSeriesMgrService mgr = new MQSeriesMgrService();

            //queue mgr names have '.' encoded to '!' in the
            //windows registry and /var/mqm/qmgrs/ on unix platforms
            mgr.name = StringUtil.replace(qmgrs[i], "!", ".");

            mgr.serverType = serverType;

            mgr.type = 
                TypeBuilder.composeServiceTypeName(serverType,
                                                   MQSeriesProductPlugin.MGR_NAME);
            mgr.fullname = mgr.name;

            services.add(mgr);
            services.addAll(MQSeriesQueueService.findServices(mgr));
        }

        return services;
    }
}
