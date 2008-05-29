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

package org.hyperic.hq.product;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * Generic detector for use by pure-xml plugins.
 */
public class DaemonDetector
    extends ServerDetector
    implements AutoServerDetector,
               FileServerDetector {

    private static final Log log =
        LogFactory.getLog(DaemonDetector.class.getName());

    private ConfigResponse _platformConfig;

    protected String getProcessQuery() {
        return getTypeProperty("PROC_QUERY");
    }

    protected boolean isSwitch(String arg) {
        return arg.startsWith("-");
    }

    private void addDefine(Map opts, String opt, int ix) {
        String key = opt.substring(2, ix);
        String val = opt.substring(ix+1);
        opts.put(key, val);

        //HQ_AUTOINVENTORY_NAME -> AUTOINVENTORY_NAME 
        //hq.autoinventory.name -> AUTOINVENTORY_NAME 
        if (key.startsWith("hq.")) {
            key = StringUtil.replace(key.toUpperCase(), ".", "_");
        }
        else if (!key.startsWith("HQ_")) {
            return;
        }

        opts.put(key.substring(3), val);
    }

    /**
     * Convert getopt-style process arguments into a Map.
     * @param pid Process id
     * @return Map of -switch => value arguments
     */
    protected Map getProcOpts(long pid) {
        String[] args = getProcArgs(pid);
        int len = args.length;
        Map opts = new HashMap();

        for (int i=0; i<len; i++) {
            String opt = args[i];
            String val;

            if (!isSwitch(opt)) {
                continue;
            }

            //"-p=22122"
            int ix = opt.indexOf('=');
            if (ix != -1) {
                //java -Dfoo=bar or getopt cmd -d -- -Dfoo=bar
                if (opt.startsWith("-D")) {
                    addDefine(opts, opt, ix);
                }
                val = opt.substring(ix+1);
                opt = opt.substring(ix);
            }
            //"-p 22122"
            else if (i+1 < len) {
                val = args[i+1];
                if (isSwitch(val)) {
                    continue;
                }
                i++;
            }
            else {
                continue;
            }

            opts.put(opt, val);
        }

        return opts;
    }

    /**
     * Auto-discover server configuration
     * @param server Auto-discovered server
     * @param pid Process id
     */
    protected void discoverServerConfig(ServerResource server, long pid) {
        Map opts = getProcOpts(pid);
        boolean isDebug = log.isDebugEnabled();
        boolean hasOpts = false;
        ConfigResponse config = server.getProductConfig();

        if (config == null) {
            config = new ConfigResponse();
            //start by setting the defaults
            setProductConfig(server, config);
        }

        for (Iterator it=config.getKeys().iterator(); it.hasNext();) {
            String key = (String)it.next();
            //<property name="port.opt" value="-p"/>
            String opt = getTypeProperty(key + ".opt");

            if (opt == null) {
                continue;
            }

            String val = (String)opts.get(opt);
            if (val != null) {
                config.setValue(key, val);
                hasOpts = true;
                if (isDebug) {
                    log.debug("Set " + key + "=" + val + 
                              ", using " + opt + " from pid=" + pid);
                }
            }
        }

        if (hasOpts) {
            server.setProductConfig(config);
        }

        ConfigResponse
            pconfig = getPlatformConfig(),
            sconfig = server.getProductConfig(),
            oconfig = new ConfigResponse(opts);

        String name =
            formatAutoInventoryName(server.getType(),
                                    pconfig, sconfig, oconfig);

        if (name != null) {
            server.setName(name);
        }

        String id = (String)opts.get(INVENTORY_ID);
        if (id == null) {
            id = server.getIdentifier(); //might be defined in plugin.xml
        }
        server.setIdentifier(formatName(id, pconfig, sconfig, oconfig));

        String installpath = (String)opts.get(INSTALLPATH);
        if (installpath != null) {
            server.setInstallPath(installpath);
        }
    }

    protected ServerResource newServerResource(long pid, String exe) {
        ServerResource server = newServerResource(exe);
        discoverServerConfig(server, pid);
        return server;
    }

    protected ServerResource newServerResource(String exe) {
        ServerResource server = createServerResource(exe);
        //try the defaults
        setProductConfig(server, new ConfigResponse());
        setMeasurementConfig(server, new ConfigResponse());
        return server;
    }

    //XXX should be in ServerDetector?
    protected void setPlatformConfig(ConfigResponse config) {
        _platformConfig = config;
    }

    protected ConfigResponse getPlatformConfig() {
        return _platformConfig;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        setPlatformConfig(platformConfig);

        List servers = getFileResources(platformConfig);

        if (servers.size() != 0) {
            return servers;
        }

        List processes = getProcessResources(platformConfig);
        for (int i=0; i<processes.size(); i++) {
            ServerResource server =
                (ServerResource)processes.get(i);

            if (isInstallTypeVersion(server.getInstallPath())) {
                servers.add(server);
            }
        }

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig,
                                   String path)
        throws PluginException {

        List servers = new ArrayList();
        servers.add(newServerResource(path));
        return servers;
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //e.g. qmail plugin has 1 instance of each service
        String hasBuiltinServices =
            getTypeProperty("HAS_BUILTIN_SERVICES");

        if (!"true".equals(hasBuiltinServices)) {
            return super.discoverServices(config);    
        }

        List services = new ArrayList();
        TypeInfo[] types = this.data.getTypes();

        for (int i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            if (type.getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }
            if (!this.getTypeInfo().getVersion().equals(type.getVersion())) {
                continue;
            }

            ServiceResource service = new ServiceResource();
            service.setType(type.getName());
            String name = getTypeNameProperty(type.getName());
            service.setServiceName(name);
            //try the defaults
            setProductConfig(service, new ConfigResponse());
            setMeasurementConfig(service, new ConfigResponse());
            services.add(service);
        }

        return services;
    }

    /**
     * Check for installed files using the file-scan config without
     * running a full file-scan. 
     */
    protected List getFileResources(ConfigResponse platformConfig)
        throws PluginException {

        List servers = new ArrayList();
        String type = getTypeInfo().getName();
        List includes = this.data.getFileScanIncludes(type);
        if (includes == null) {
            return servers;
        }

        for (int i=0; i<includes.size(); i++) {
            String file = (String)includes.get(i);
            if (new File(file).exists()) {
                servers.add(newServerResource(file));
            }
        }

        return servers;
    }

    /**
     * Process table scan
     */
    protected List getProcessResources(ConfigResponse platformConfig)
        throws PluginException {

        List servers = new ArrayList();

        String query = getProcessQuery();
        if (query == null) {
            log.debug("No PROC_QUERY defined for: " +
                      getTypeInfo().getName());
            return servers;
        }
        else {
            log.debug("Using PROC_QUERY=" + query + " for " +
                      getTypeInfo().getName());
        }

        long[] pids = getPids(query);

        log.debug("'" + query + "' matched " +
                  pids.length + " processes");

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];

            String exe = getProcExe(pid);
            if (exe == null) {
                log.debug("Unable to determine exe for " +
                          query + " pid=" + pid);
                exe = query;
            }

            servers.add(newServerResource(pid, exe));
        }

        return servers;
    }
}
