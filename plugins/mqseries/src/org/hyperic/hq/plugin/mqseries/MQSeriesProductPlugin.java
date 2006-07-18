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

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

public class MQSeriesProductPlugin
    extends ProductPlugin {

    public static final String NAME = "mqseries";

    public static final String SERVER_NAME = "MQSeries";
    public static final String SERVER_DESC = "Server";

    public static final String MGR_NAME = "Queue Manager";
    public static final String Q_NAME   = "Queue";

    public static final String PROP_MGR_NAME = "queue.manager.name";
    public static final String PROP_Q_NAME   = "queue.name";

    public static final String PROP_INSTALLPATH =
        NAME + "." + ProductPlugin.PROP_INSTALLPATH;

    static final String[] DEFAULT_UNIX_INST = {
        "/opt/mqm",
    };

    private static final String[] DEFAULT_WIN32_INST = {
        "C:\\Program Files\\IBM\\Websphere MQ",
    };

    public static final String[] MGR_CONFIG = {
        PROP_MGR_NAME,
        "Queue Manager",
        "MQ_hostname"
    };

    public static final String[] Q_CONFIG = {
        PROP_Q_NAME,
        "Queue",
        "default"
    };

    public static final String[] INTERNAL_SERVICES = {
    };

    public static final String[] DEPLOYED_SERVICES = {
        MGR_NAME,
        Q_NAME,
    };

    public static final String VERSION_5x  = "5.x";

    public static final String VERSION_5_NAME =
        TypeBuilder.composeServerTypeName(SERVER_NAME,
                                          VERSION_5x);

    public static final String VERSION_5_MGR_NAME =
        TypeBuilder.composeServiceTypeName(VERSION_5_NAME,
                                           MGR_NAME);

    public static final String VERSION_5_QUEUE_NAME =
        TypeBuilder.composeServiceTypeName(VERSION_5_NAME,
                                           Q_NAME);

    public MQSeriesProductPlugin() {
        setName(NAME);
    }

    public String[] getClassPath(ProductPluginManager manager) {
        Properties managerProps = manager.getProperties();
        String installDir =
            managerProps.getProperty(PROP_INSTALLPATH);

        if (installDir == null) {
            String[] dirs;
            if (System.getProperty("os.name").startsWith("Windows")) {
                dirs = DEFAULT_WIN32_INST;
            }
            else {
                dirs = DEFAULT_UNIX_INST;
            }

            for (int i=0; i<dirs.length; i++) {
                if (new File(dirs[i]).exists()) {
                    installDir = dirs[i];
                    break;
                }
            }
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

    public GenericPlugin getPlugin(String type, TypeInfo info)
    {
        if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            if (info.getType() == TypeInfo.TYPE_SERVICE) {
                return new MQSeriesMeasurementPlugin();
            }
            else {
                return new MQSeriesServerMeasurementPlugin();
            }
        }
        else if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            switch (info.getType()) {
              case TypeInfo.TYPE_SERVER:
                  if (info.isWin32Platform()) {
                      //return new MQSeriesServerControlPluginWin32();
                  }
                  else {
                      //return new MQSeriesServerControlPlugin();
                  }
                  break;
              case TypeInfo.TYPE_SERVICE:
                  break;
            }
        }
        else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
            if (info.isServer(SERVER_NAME, VERSION_5x)) {
                return new MQSeriesDetector();
            }
        }

        return null;
    }

    public TypeInfo[] getTypes() {
        TypeBuilder types =
            new TypeBuilder(SERVER_NAME, SERVER_DESC,
                            TypeBuilder.UNIX_PLATFORM_NAMES);

        ServerTypeInfo server;

        server = types.addServer(VERSION_5x);

        types.addServices(server, DEPLOYED_SERVICES, INTERNAL_SERVICES);


        //clone servers/services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        return types.getTypes();
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {

        SchemaBuilder schema = new SchemaBuilder(config);

        switch (info.getType()) {
          case TypeInfo.TYPE_SERVER:
            break;
          case TypeInfo.TYPE_SERVICE:
            if (info.isService(MGR_NAME)) {
                schema.add(MGR_CONFIG);
            }
            else if (info.isService(Q_NAME)) {
                schema.add(MGR_CONFIG);
                schema.add(Q_CONFIG);
            }
            break;
        }

        return schema.getSchema();
    }
}
