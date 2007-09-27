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

package org.hyperic.hq.plugin.postfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.SigarException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PostfixServerDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final String PTQL_QUERY =
        "State.Name.eq=master";

    private Log log =  LogFactory.getLog("PostfixServerDetector");

    static final String SERVER_NAME = "Postfix";
    static final String QUEUE = "Queue";

    protected static final String PROP_PATH = "path";
    protected static final String PROP_QDIR  = "queueDir";
    protected static final String PROP_QUEUE = "queue";
    protected static final String PROP_POSTCONF = "postconf";
    protected static final String PROP_CONFDIR = "configDir";

    private static final HashMap postconfInfoCache = new HashMap();

    private static String postconfBin = null;

    private String getPostconfValue(ConfigResponse config, String key) {
        String confDir = config.getValue(PROP_CONFDIR);
        HashMap info = (HashMap)postconfInfoCache.get(confDir);
        if (info == null) {
            info = getPostconfInfo(config);
            postconfInfoCache.put(confDir, info);
        }
        return (String)info.get(key);
    }

    private HashMap getPostconfInfo(ConfigResponse config) {
        HashMap info = new HashMap();
        Process postconf;

        String crPostconfBin = config.getValue(PROP_POSTCONF);
        String confDir = config.getValue(PROP_CONFDIR);

        // now that PROP_POSTCONF and PROP_CONFDIR gets set before
        // this is called, these should hopefully never be true.
        if (crPostconfBin == null) {
            this.log.warn("postconf not configured, taking a guess...");
            crPostconfBin = "/usr/sbin/postconf";
        }
        if (confDir == null) {
            this.log.warn("postfix configuration directory not configured..");
            confDir = "/etc/postfix";
        }

        try {
            this.log.debug("executing " + crPostconfBin + " -c " + confDir);
            String argv[] = { crPostconfBin, "-c", confDir };
            postconf = Runtime.getRuntime().exec(argv);
        } catch (IOException e) {
            this.log.error("postconf exec failed:" + e);
            return info;
        }

        BufferedReader in = null;
        String line;

        try {
            in = new BufferedReader(
                new InputStreamReader(postconf.getInputStream())
            );

            for (int i = 0; (line = in.readLine()) != null; i++) {
                // XXX:String.split() no workie in java 1.3
                String conf[] = line.split(" = ");
                if (conf.length == 1) {
                    info.put(conf[0], "");
                } else {
                    info.put(conf[0], conf[1]);
                }
            }
        } catch (IOException e) {
            this.log.error("io exception: " + e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }

        return info;
    }

    private String whichPostconf(String envPath) {
        if (envPath == null) {
            return null;
        }
        String[] path = envPath.split(":");

        for (int i=0; i<path.length; i++) {
            File postconf =
                new File(path[i], "postconf");

            if (postconf.exists()) {
                log.debug("Using postconf=" + postconf);
                return postconf.toString();
            }
        }

        return null;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        long[] pids = getPids(PTQL_QUERY);
        String user = System.getProperty("user.name");

        //XXX use getSigar().getProcCred().getEuid() != 0 ?
        if (!"root".equals(user)) {
            int num = pids.length;
            if (num != 0) {
                log.info("Found " + num + " postfix processes" + 
                         " but cannot discover or monitor unless " +
                         "running as root (user.name=" + user + ")");
            }
            return null;
        }

        List servers = new ArrayList();

        for (int i=0; i < pids.length; i++) {
            Map env;
            try {
              env = getSigar().getProcEnv(pids[i]);
            } catch (SigarException e) {
                this.log.debug("must be root to get postfix info...");
                return servers;
            }

            // Config directory is the cwd
            String postfixConfigDir = (String)env.get("config_directory");
            if (postfixConfigDir == null) {
                continue;
            }

            // try to find postconf in the PATH
            // but only if we havent already found one.
            if (postconfBin == null) {
                postconfBin = whichPostconf((String)env.get("PATH"));
                if (postconfBin == null) {
                    log.warn("Unable to find postconf in PATH");
                    continue;
                }
            }

            ConfigResponse productConfig = new ConfigResponse();

            // set PROP_CONFDIR and PROP_POSTCONF here so
            // getPostconfInfo can use it
            productConfig.setValue(PROP_CONFDIR, postfixConfigDir);
            productConfig.setValue(PROP_POSTCONF, postconfBin);

            // get version from postconf output
            String version =
                getPostconfValue(productConfig, "mail_version");

            if (version == null) {
                log.warn("cant find mail_version key in postconf");
                continue;
            }

            String qdir = getPostconfValue(productConfig, "queue_directory");
            if (qdir == null) {
                continue;
            }
            // create server with postfixConfigDir instead of postfix
            // so it will be unique for multiple servers.
            ServerResource server = createServerResource(postfixConfigDir);

            // set a server specific ptql query for process metrics
            File pidFile = new File(qdir, "pid/master.pid");//XXX check exists
            String ptqlQuery = "Pid.PidFile.eq=" + pidFile;

            productConfig.setValue("process.query", ptqlQuery);

            productConfig.setValue(PROP_QDIR, qdir);

            server.setProductConfig(productConfig);
            server.setMeasurementConfig();

            String name =
                getPlatformName() + " " + SERVER_NAME + " " + version;

            // set server name - different if there are multiple
            // servers running
            if (pids.length > 1) {
                name += " (" + postfixConfigDir + ")";
            }

            server.setName(name);

            servers.add(server);
        }

        return servers;
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {

        ArrayList services = new ArrayList();

        String qNames = getPostconfValue(config, "hash_queue_names");

        if (qNames == null) {
            this.log.error("cant find hash_queue_names key in postconf");
            return null;
        }

        String queues[] = qNames.split(", ");

        String qdir = config.getValue(PROP_QDIR);

        for (int i = 0; i < queues.length; i++) {
            String name = queues[i];
            String path = qdir + "/" + name;
            ServiceResource service = new ServiceResource();
            service.setType(this, QUEUE);
            service.setServiceName(name +" "+ QUEUE);

            ConfigResponse productConfig = new ConfigResponse();
            productConfig.setValue(PROP_QUEUE, name);
            productConfig.setValue(PROP_PATH, path);

            service.setProductConfig(productConfig);
            service.setMeasurementConfig();
            //service.setControlConfig();

            this.log.debug("adding postfix " + name + " queue (" + path + ")");
            services.add(service);
        }

        return services;

    }
}
