/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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

package org.hyperic.hq.plugin.multilogtrack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.DirectoryScanner;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

public class MultiLogTrackServerDetector extends ServerDetector {
    
    private static final Log log = LogFactory.getLog(MultiLogTrackServerDetector.class);
    private static final String SERVER_NAME = "Multi Log Tracker";
    private static final String SERVER_TYPE = "MultiLogTracker 1.0";
    private static final String LOG_FILE_TRACKER_TYPE = "LogFileTracker";
    static final String ENABLE_LOG_SERVICES_PROP = "server.enable_log_services";
    static final String ENABLE_ONLY_METRICS = "enable_only_metrics";
    public static final String OVERRIDE_CHECKS = "override_file_checks";
    private static final Map<String, List<String>> files = new HashMap<String, List<String>>();

    public List<ServerResource> getServerResources(ConfigResponse platformConfig) throws PluginException {
        final List<ServerResource> rtn = new ArrayList<ServerResource>();
        final ConfigResponse productConfig = new ConfigResponse();
        final String serverName = getFqdn(productConfig) + " " + SERVER_NAME;
        final ServerResource server = newServerResource("/tmp", SERVER_TYPE, serverName, productConfig);
        rtn.add(server);
        return rtn;
    }
    
    private String getFqdn(ConfigResponse config) {
        return config.getValue(ProductPlugin.PROP_PLATFORM_FQDN, getPlatformName());
    }
    
    private ServerResource newServerResource(String installdir, String type, String name,
                                             ConfigResponse productConfig) {
        ServerResource server = createServerResource(installdir);
        setProductConfig(server, productConfig);
        // sets a default Measurement Config property with no values
        setMeasurementConfig(server, new ConfigResponse());
        server.setName(name);
        server.setType(type);
        return server;
    }
    
    protected List<ServiceResource> discoverServices(ConfigResponse config) throws PluginException {
        if ("true".equals(config.getValue(ENABLE_LOG_SERVICES_PROP, "true"))) {
            return getServices(config);
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<ServiceResource> getServices(ConfigResponse serverConfig)
    throws PluginException {
        final List<ServiceResource> rtn = new ArrayList<ServiceResource>();
        final List<String> files = new ArrayList<String>();
        final String basedir = getBasedirAndSetFiles(serverConfig, files);
        final String value = serverConfig.getValue("server.log_track.enable");
        final boolean serverHasLogTrack = (value == null || !value.equalsIgnoreCase("true")) ? false : true;
        for (final String file : files) {
            final ConfigResponse serviceConfig = new ConfigResponse();
            final ConfigResponse logConfig = new ConfigResponse();
            if (serverHasLogTrack) {
                logConfig.setValue("service.log_track.enable", "false");
            } else {
                logConfig.setValue("service.log_track.enable", "true");
            }
            final String logfile = basedir + getFileSeparator(basedir) + file;
            serviceConfig.setValue("logfile", logfile);
            rtn.add(newServiceResource(logConfig, logfile, serviceConfig));
        }
        return rtn;
    }
    
    static String getFileSeparator(String exampleDir) {
        if (exampleDir == null) {
            return File.separator;
        }
        if (exampleDir.contains("\\")) {
            return "\\";
        } else if (exampleDir.contains("/")) {
            return "/";
        }
        return File.separator;
    }
    
    static boolean fileCacheIsSet(ConfigResponse cfg) {
        synchronized(files) {
            final String logfilepattern = getLogfilepattern(cfg);
            final String basedir = getBasedir(cfg);
            final String key = logfilepattern + "|" + basedir;
            return files.containsKey(key);
        }
    }
    
    static List<String> getFilesCached(String logfilepattern, String basedir, String includePattern, boolean resetCache) {
        synchronized(files) {
            String key = logfilepattern + "|" + basedir + "|" + includePattern;
            if (resetCache) {
                files.remove(key);
            }
            List<String> tmp;
            if (null == (tmp = files.get(key))) {
                tmp = getFiles(logfilepattern, basedir);
                files.put(key, tmp);
            }
            return files.get(key);
        }
    }
    
    private static List<String> getFiles(String logfilepattern, String basedir) {
        if (logfilepattern == null || basedir == null) {
            return Collections.emptyList();
        }
        final File dir = new File(basedir);
        if (!dir.isDirectory()) {
            log.debug("basedir=" + dir + " is not a directory", new Throwable());
            return Collections.emptyList();
        }
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(basedir);
        final String[] toks = logfilepattern.split(",");
        final List<String> filePatterns = new ArrayList<String>(toks.length);
        for (String tok : toks) {
            while (tok.startsWith("/") || tok.startsWith("\\")) {
                tok = tok.substring(1, tok.length());
            }
            filePatterns.add(tok);
        }
        scanner.setIncludes(filePatterns.toArray(new String[0]));
        scanner.scan();
        return Arrays.asList(scanner.getIncludedFiles());
    }

    static String getBasedir(ConfigResponse config) {
        String basedir = config.getValue("basedir", "").trim();
        return getBasedir(basedir);
    }
    
    static String getBasedir(String basedir) {
        basedir = (basedir == null) ? "" : basedir.trim();
        while (basedir.endsWith("/") || basedir.endsWith("\\")) {
            basedir = basedir.substring(0, basedir.length()-1);
        }
        return basedir;
    }

    static String getLogfilepattern(ConfigResponse config) {
        return config.getValue("logfilepattern", "").trim();
    }

    static String getBasedirAndSetFilesFromCache(ConfigResponse config, Collection<String> files, boolean resetCache) {
        final String logfilepattern = getLogfilepattern(config);
        String basedir = getBasedir(config);
        String includePattern = config.getValue(MultiLogTrackPlugin.INCLUDE_PATTERN, "");
        files.addAll(getFilesCached(logfilepattern, basedir, includePattern, resetCache));
        return basedir;
    }

    private static String getBasedirAndSetFiles(ConfigResponse config, Collection<String> files) {
        final String logfilepattern = getLogfilepattern(config);
        String basedir = getBasedir(config);
        files.addAll(getFiles(logfilepattern, basedir));
        return basedir;
    }
    
    private ServiceResource newServiceResource(ConfigResponse logConfig, String name,
                                               ConfigResponse productConfig) {
        ServiceResource service = new ServiceResource();
        service.setType(this, LOG_FILE_TRACKER_TYPE);
        service.setServiceName(name);
        service.setProductConfig(productConfig);
        service.setMeasurementConfig(logConfig);
        return service;
    }
    
}
