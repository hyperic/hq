/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.zimbra.five;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.LogFileTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;

public class ZimbraServerDetector extends ServerDetector implements AutoServerDetector {

    private static final String PROCESS_NAME = "zmmailboxdmgr";
    private static final String PROCESS_DIR = "/libexec";
    private static final String ZMSTATS_DIR = "zmstat";
    private static final String PTQL_QUERY = "State.Name.eq={0}";
    private static final String PROCESS_PID_QUERY = "Pid.SudoPidFile.eq={0}";
    private static final String PROCESS_EXEC = "State.Name.eq={0}";
    private static final String PROCESS_EXEC_2 = "State.Name.ct={0}";
    // private static final String PROCESS_CHILD_QUERY = "State.Ppid.eq={0,number,#}";
    private static Log log = LogFactory.getLog(ZimbraServerDetector.class);

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("getServerResources(" + platformConfig + ")");

        Object args[] = {PROCESS_NAME};
        long[] pids = getPids(MessageFormat.format(PTQL_QUERY, args));
        List list_servers = new ArrayList();
        for (int n = 0; n < pids.length; n++) {
            long pid = pids[n];
            try {
                String exe = getProcExe(pid);
                String path = exe.substring(0, exe.length() - (PROCESS_DIR + File.separator + PROCESS_NAME).length());
                log.debug("[getServerResources] proc: (" + pid + ") " + exe + " (" + path + ")");

                // check version
                if (!checkVersion(path, getTypeInfo().getVersion())) {
                    return list_servers;
                }
                //**************

                File zmStatsDir = new File(path, ZMSTATS_DIR);
                log.debug("[getServerResources] zmStatsDir=" + zmStatsDir.getCanonicalPath() + " (" + (zmStatsDir.exists() ? "OK" : "No OK") + ")");
                if (zmStatsDir.exists()) {
                    ConfigResponse productConfig = new ConfigResponse();
                    productConfig.setValue("installpath", path);

                    ServerResource server = new ServerResource();
                    server.setType(this);
                    server.setName(getPlatformName() + " " + getTypeInfo().getName());
                    server.setInstallPath(path);
                    server.setIdentifier("zimbra " + path);

                    // server.setProductConfig(productConfig);
                    setProductConfig(server, productConfig);

                    ConfigResponse metricConfig = new ConfigResponse();
                    metricConfig.setValue(LogFileTrackPlugin.PROP_FILES_SERVER,
                            "log/mailbox.log");
                    server.setMeasurementConfig(metricConfig,
                            LogFileTrackPlugin.LOGLEVEL_WARN,
                            false);

                    ConfigResponse controlConfig = new ConfigResponse();
                    controlConfig.setValue(ServerControlPlugin.PROP_PROGRAMPREFIX, "/usr/bin/sudo -u zimbra");  // RHEL 5.0
                    controlConfig.setValue(ServerControlPlugin.PROP_TIMEOUT, "300");
                    server.setControlConfig(controlConfig);

                    list_servers.add(server);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return list_servers;
    }
    private final static Service[] SERVICES = {
        new Service("MySQL", "/db/mysql.pid"),
        new Service("Postfix", "/data/postfix/spool/pid/master.pid"),
        new Service("Log Watch", "/log/logswatch.pid"),
        new Service("Logger MySQL", "/logger/db/mysql.pid"),
        new Service("OpenLDAP", "/openldap/var/run/slapd.pid"),
        new Service("Swatch", "/log/swatch.pid"),
        new Service("MTA Config", "/log/zmmtaconfig.pid"),
        new Service("memcached", "/log/memcached.pid"),
        new Service("ClamAV", "/log/clamd.pid", "log/clamd.log"),
        new Service("Convertd Monitor", "/log/zmconvertdmon.pid", "log/zmconvertd.log"),
        new Service("Jetty Process", "/log/zmmailboxd_java.pid")
    };
    private final static Service[] MULTI_SERVICES = {
        new Service("AMaViS", "/log/amavisd.pid", null, "amavisd"),
        new Service("HTTPD", "/log/httpd.pid"),
        new Service("NGINX", "/log/nginx.pid", "log/nginx.log"),
        new Service("Cyrus SASL", "/cyrus-sasl/state/saslauthd.pid")
    };
    private final static Service[] OTHER_SERVICES = {
        new Service("MTAQueue Stats", "/zmstat/mtaqueue.csv"),
        new Service("VM Stats", "/zmstat/vm.csv")
    };

    private ServiceResource createService(Service serviceData, String path) {
        ServiceResource service = new ServiceResource();
        service.setServiceName(serviceData.getName());
        service.setType(this, serviceData.getName());
        service.setDescription(path);

        ConfigResponse metricConfig = new ConfigResponse();
        if (serviceData.getLog() != null) {
            metricConfig.setValue(LogFileTrackPlugin.PROP_FILES_SERVICE, serviceData.getLog());
        } else {
            metricConfig.setValue("service.log_track.enable", "false");
        }
        service.setMeasurementConfig(metricConfig);

        return service;
    }

    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("discoverServices(" + config + ")");
        List services = new ArrayList();

        for (int n = 0; n < OTHER_SERVICES.length; n++) {
            Service serviceData = OTHER_SERVICES[n];
            File csvFile = new File(config.getValue("installpath"), serviceData.getPidFile());
            if (csvFile.exists()) {
                ServiceResource service = createService(serviceData, csvFile.getAbsolutePath());

                ConfigResponse props = new ConfigResponse();
                setProductConfig(service, props);

                service.setCustomProperties(new ConfigResponse());

                services.add(service);
            } else {
                log.debug("OTHER_SERVICES '" + serviceData.getName() + "(" + serviceData.getPidFile() + ")' not found");
            }
        }

        for (int n = 0; n < SERVICES.length; n++) {
            Service serviceData = SERVICES[n];
            File pidFile = new File(config.getValue("installpath"), serviceData.getPidFile());
            Object args[] = {pidFile.getAbsolutePath()};
            String q = MessageFormat.format(PROCESS_PID_QUERY, args);
            long[] pids = getPids(q);
            if (pids.length > 0) {
                ServiceResource service = createService(serviceData, pidFile.getAbsolutePath());
                ConfigResponse props = new ConfigResponse();
                props.setValue("process.query", q);
                setProductConfig(service, props);
                service.setCustomProperties(new ConfigResponse());
                services.add(service);
            } else {
                log.debug("SERVICES '" + serviceData.getName() + "(" + serviceData.getPidFile() + ")' not found");
            }
        }

        for (int n = 0; n < MULTI_SERVICES.length; n++) {
            Service serviceData = MULTI_SERVICES[n];
            log.debug("[discoverServices] -> " + serviceData.getName());
            File pidFile = new File(config.getValue("installpath"), serviceData.getPidFile());
            log.debug("pidFile='" + pidFile + "'");
            Object args[] = {pidFile.getAbsolutePath()};
            String pQuery = MessageFormat.format(PROCESS_PID_QUERY, args);
            log.debug("pQuery --> '" + pQuery + "'");
            long[] pids = getPids(pQuery);
            if (pids.length > 0) {
                long pid = pids[0];
                String exec = getProcExe(pid);
                log.debug("'" + serviceData.getName() + "' exec='" + exec + "'");

                ServiceResource service = createService(serviceData, pidFile.getAbsolutePath());

                ConfigResponse props = new ConfigResponse();
                if (serviceData.getProcess() != null) {
                    Object[] args2 = {serviceData.getProcess()};
                    props.setValue("process.query", MessageFormat.format(PROCESS_EXEC_2, args2));
                } else {
                    Object[] args2 = {new File(exec).getName()};
                    props.setValue("process.query", MessageFormat.format(PROCESS_EXEC, args2));
                }
                props.setValue("process.status", pidFile.getAbsolutePath());
                setProductConfig(service, props);
                log.debug("process.query = '" + props.getValue("process.query") + "'");
                log.debug("process.status ='" + pidFile.getAbsolutePath() + "'");

                service.setCustomProperties(new ConfigResponse());

                services.add(service);
            } else {
                log.debug("MULTI_SERVICES '" + serviceData.getName() + "(" + serviceData.getPidFile() + ")' not found");
            }
            log.debug("[discoverServices] <- " + serviceData.getName());
        }

        long[] Convertd_pids = getPids("State.Name.eq=java,Args.*.eq=com.zimbra.cs.convertd.TransformationServer");
        if (Convertd_pids.length > 0) {
            ServiceResource service = new ServiceResource();
            service.setServiceName("Convertd");
            service.setType(this, "Convertd");

            ConfigResponse props = new ConfigResponse();
            setProductConfig(service, props);

            ConfigResponse metricConfig = new ConfigResponse();
            metricConfig.setValue(LogFileTrackPlugin.PROP_FILES_SERVICE, "log/convertd.log"); //relative to installpath
            service.setMeasurementConfig(metricConfig);
            service.setCustomProperties(new ConfigResponse());

            services.add(service);
        }

        // Stats Process
        File dir_pids = new File(config.getValue("installpath"), "/zmstat/pid");
        String[] pids_files = dir_pids.list(new PIDFilter());
        for (int n = 0; n < pids_files.length; n++) {
            String pid_file = pids_files[n];
            Object args[] = {pid_file};
            String q = MessageFormat.format(PROCESS_PID_QUERY, args);
            long[] pids = getPids(q);
            if (pids.length > 0) {
                services.add(cerateStatService(pid_file));
            }
        }

        return services;
    }
    private static Pattern p = Pattern.compile("zmstat-(.*).pid");

    private ServiceResource cerateStatService(String file) {
        log.debug("cerateStatService('" + file + "')");
        Matcher m = p.matcher(file);
        String name = file;
        if (m.matches()) {
            name = m.group();
        }
        ServiceResource servicio = new ServiceResource();
        servicio.setServiceName(name);
        servicio.setType(this, "Stats process");
        servicio.setDescription(file);

        ConfigResponse props = new ConfigResponse();
        props.setValue("pid-file", file);
        setProductConfig(servicio, props);

        ConfigResponse metricConfig = new ConfigResponse();
        metricConfig.setValue("service.log_track.enable", "false");
        servicio.setMeasurementConfig(metricConfig);

        servicio.setCustomProperties(new ConfigResponse());
        return servicio;

    }

    private static class PIDFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".pid");
        }
    }

    private boolean checkVersion(String path, String version) {
        boolean res = false;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Execute exec = new Execute(new PumpStreamHandler(output));
            exec.setCommandline(
                    new String[]{"sudo", "-u", "zimbra", path + "/bin/zmcontrol", "-v"});
            int rc = exec.execute();
            String out = output.toString().trim();
            if (getLog().isDebugEnabled()) {
                getLog().debug("output of '" + path + "/bin/zmcontrol -v' : " + out);
            }
            if (rc == 0) {
                Pattern p = Pattern.compile(version.toLowerCase().replaceAll("x", "\\\\d*"));
                Matcher m = p.matcher(out);
                res = m.find();
                if (!res) {
                    getLog().debug("m -->" + m);
                }
            }
        } catch (Exception e) {
            getLog().warn("Could not get the version of mysql: " + e.getMessage(), e);
        }
        return res;
    }
}
