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
    protected static final String PROP_IDX = "index";
    protected static final String PROP_QDIR  = "queueDir";
    protected static final String PROP_QUEUE = "queue";
    protected static final String PROP_POSTCONF = "postconf";
    protected static final String PROP_CONFDIR = "configDir";

    private static final HashMap postconfInfo = new HashMap();

    private static String postconfBin = null;

    public void getPostconfInfo(String index, ConfigResponse config) {
        Process postconf;

        String crPostconfBin =
            config.getValue(PROP_POSTCONF);
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
            return;
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
                    postconfInfo.put(conf[0] + index, "");
                } else {
                    postconfInfo.put(conf[0] + index, conf[1]);
                }
            }
        } catch (IOException e) {
            this.log.error("io exception: " + e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }

    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);

        for (int i=0; i < pids.length; i++) {

            // Config directory is the cwd
            String postfixConfigDir;
            try {
                postfixConfigDir =
                    getSigar().getProcEnv(pids[i], "config_directory");
            }
            catch (SigarException e) {
                //throw new PluginException(e);
                //XXX: non-root users will get a permissions error here.
                this.log.debug("must be root to get postfix info...");
                return servers;
            }

            // try to find postconf in the PATH
            // but only if we havent already found one.
            if (postconfBin == null) {
                String[] path;
                try {
                    path = getSigar().getProcEnv(pids[i], "PATH").split(":");
                }
                catch (SigarException e) {
                    throw new PluginException(e);
                }
                boolean foundPostconf = false;
                for (int j=0; j < path.length; j++) {
                    postconfBin = path[j] + "/postconf";
                    this.log.debug("checking for " + postconfBin);
                    File pcFile = new File(path[j] + "/postconf");

                    if (pcFile.exists()) {
                        this.log.debug("found " + postconfBin);
                        foundPostconf = true;
                        break;
                    }
                }
                if (!foundPostconf) {
                    this.log.warn("cant find postconf in path!");
                    postconfBin = null;
                }
            }

            String postfix = getProcExe(pids[i]);
            if (postfix != null) {
                // postfix master process is running

                ConfigResponse productConfig = new ConfigResponse();

                // set PROP_CONFDIR and PROP_POSTCONF here so
                // getPostconfInfo can use it
                productConfig.setValue(PROP_CONFDIR, postfixConfigDir);
                productConfig.setValue(PROP_POSTCONF, postconfBin);

                // get version from postconf output
                String version = (String)postconfInfo.get("mail_version" + i);

                // if the version is null, we havent ran postconf
                // for this server yet...
                if (version == null) {
                    getPostconfInfo(String.valueOf(i), productConfig);
                    version = (String)postconfInfo.get("mail_version" + i);
                    if (version == null) {
                       this.log.error("cant find mail_version key in postconf");
                    }
                }

                // get queue directory from postconf
                //String qdir = (String)postconfInfo.get("queue_directory" + i);
                // get queue directory from process env with sigar
                String qdir;
                try {
                    qdir = getSigar().getProcEnv(pids[i], "queue_directory");
                }
                catch (SigarException e) {
                    throw new PluginException(e);
                }

                // create server with postfixConfigDir instead of postfix
                // so it will be unique for multiple servers.
                ServerResource server =  createServerResource(postfixConfigDir);

                // set a server specific ptql query so avail works
                //XXX: Pid will probably not work with restarts, etc.
                String ptqlQuery = PTQL_QUERY + ",Pid.Pid.eq=" + pids[i];
                productConfig.setValue("process.query", ptqlQuery);

                productConfig.setValue(PROP_QDIR, qdir);
                productConfig.setValue(PROP_IDX, String.valueOf(i));
                server.setProductConfig(productConfig);
                server.setMeasurementConfig();

                // set server name - different if there are multiple
                // servers running
                if (pids.length > 1) {
                    server.setName(getPlatformName() + " " + SERVER_NAME
                        + " " + version + " (" + postfixConfigDir + ")");
                }
                else {
                    server.setName(getPlatformName() + " " + SERVER_NAME
                        + " " + version);
                }

                servers.add(server);
            }
        }

        return servers;
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {

        ArrayList services = new ArrayList();

        String index = config.getValue(PROP_IDX);
        String qNames = (String)postconfInfo.get("hash_queue_names" + index);

        // if qNames is null, we need to run postconf for this server
        if (qNames == null) {
            getPostconfInfo(index, config);
            qNames = (String)postconfInfo.get("hash_queue_names" + index);
            if (qNames == null) {
                this.log.error("cant find hash_queue_names key in postconf");
            }
        }

        // XXX:String.split() no workie in java 1.3
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
            // set index to the same value as the server.
            productConfig.setValue(PROP_IDX, index);

            service.setProductConfig(productConfig);
            service.setMeasurementConfig();
            //service.setControlConfig();

            this.log.debug("adding postfix " + name + " queue (" + path + ")");
            services.add(service);
        }

        return services;

    }
}
