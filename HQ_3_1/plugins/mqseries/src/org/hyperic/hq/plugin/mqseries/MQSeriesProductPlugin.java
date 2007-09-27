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
import java.io.IOException;

import java.util.Properties;

import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class MQSeriesProductPlugin
    extends ProductPlugin {

    public static final String SERVER_NAME = "MQSeries";

    public static final String MGR_NAME = "Queue Manager";
    public static final String Q_NAME   = "Queue";

    public static final String PROP_MGR_NAME = "queue.manager.name";
    public static final String PROP_Q_NAME   = "queue.name";

    public static final String PROP_INSTALLPATH =
        "mqseries." + ProductPlugin.PROP_INSTALLPATH;

    static final String[] DEFAULT_UNIX_INST = {
        "/opt/mqm",
    };

    private static final String[] DEFAULT_WIN32_INST = {
        "C:\\Program Files\\IBM\\Websphere MQ",
    };

    static String MQ_KEY;

    static String getRegistryValue(String name) {
        RegistryKey key = null;

        try {
            key = RegistryKey.LocalMachine.openSubKey(MQ_KEY);
            return key.getStringValue(name).trim();
        } catch (Win32Exception e) {
            return null;
        } finally {
            if (key != null) {
                key.close();
            }
        }
    }

    static String getRegistryFilePath() {
        return getRegistryValue("FilePath");
    }

    static String getRegistryWorkPath() {
        return getRegistryValue("WorkPath");
    }

    private String findInstallDir() {
        String installDir = null;
        String[] dirs;
        if (isWin32()) {
            if ((installDir = getRegistryFilePath()) != null) {
                return installDir;
            }
            dirs = DEFAULT_WIN32_INST;
        }
        else {
            dirs = DEFAULT_UNIX_INST;
        }

        for (int i=0; i<dirs.length; i++) {
            if (new File(dirs[i]).exists()) {
                return dirs[i];
            }
        }        

        return null;
    }

    public String[] getClassPath(ProductPluginManager manager) {
        if (isWin32()) {
            String prop = "mqseries.regkey";
            MQ_KEY = getProperty(prop);
            if (MQ_KEY == null) {
                throw new IllegalArgumentException(prop +
                                                   " property undefined");
            }
        }

        Properties managerProps = manager.getProperties();
        String installDir =
            managerProps.getProperty(PROP_INSTALLPATH);

        if (installDir == null) {
            installDir = findInstallDir();
        }

        if (installDir == null) {
            return new String[0];
        }

        mapLibrary(installDir);

        return new String[] {
            installDir + "/java/lib/com.ibm.mq.jar",
            installDir + "/java/lib/connector.jar",
        };
    }

    private void mapLibrary(String installDir) {
        //ibm still loves 8.3 filenames.
        final String mqlib = "mqjbnd05";

        try {
            System.loadLibrary(mqlib);
            return; //can already find it.
        } catch (UnsatisfiedLinkError e) {
            //we will map it based on installDir
        }

        File lib = 
            new File(installDir + "/java/lib",
                     System.mapLibraryName(mqlib));

        try {
            //PluginLoader will use this to map when
            //mqseries does System.loadLibrary("mqjbnd05")
            System.setProperty("net.covalent.lib." + mqlib,
                               lib.getCanonicalPath());
        } catch (IOException e) {
            //notgonnahappen.
        }
    }
}
