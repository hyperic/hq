package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

public class GemFireDetector extends ServerDetector implements AutoServerDetector {

    //XXX make public on ServerDetector
    private static final String VERSION_FILE = "VERSION_FILE";
    private static final String VERSION = "VERSION";
    private static final String JAR_FILE = "JAR_FILE";
    private static final String DEF_URL = "service:jmx:rmi://localhost/jndi/rmi://:1099/jmxconnector";
    private static final Log log = LogFactory.getLog(GemFireDetector.class);
    private static final String procQuery = "State.Name.sw=java,Args.*.eq=com.gemstone.gemfire.admin.jmx.internal.AgentLauncher";

    public List getServerResources(ConfigResponse platformConfig)
            throws PluginException {
        String jarFile = getTypeProperty("JAR_FILE");
        List servers = new ArrayList();
        try {
            long[] pids = getPids(procQuery);
            if (log.isDebugEnabled()) {
                log.debug(procQuery + " matched " + pids.length + " processes");
            }

            for (int i = 0; i < pids.length; i++) {
                long pid = pids[i];
                String[] args = getProcArgs(pid);
                String path = null;
                String url = DEF_URL;
                for (int j = 0; j < args.length; j++) {
                    String arg = args[j];
                    if (arg.equals("-classpath")) {
                        List<String> classpath = Arrays.asList(args[j + 1].split(File.pathSeparator));
                        for (String jar : classpath) {
                            //XXX support regexp...
                            if (jar.endsWith(jarFile)) {
                                path = jar.substring(0, jar.length() - jarFile.length());
                            }
                        }
                    } else if (arg.startsWith("rmi-bind-address")) {
                        String host = arg.split("=")[1];
                        url = url.replaceFirst("localhost", host);
                    } else if (arg.startsWith("rmi-port")) {
                        String port = arg.split("=")[1];
                        url = url.replaceFirst("1099", port);
                    }
                }

                if ((path != null) && isInstallTypeVersion(path)) {
                    //config.setValue("jmx.url", url);
                    ServerResource server = createServerResource(path);
                    servers.add(server);
                }
            }
        } catch (Exception ex) {
            log.debug("ERROR: " + ex.getMessage(), ex);
        }

        return servers;
    }

    // XXX sacar la version por JMX
    @Override
    protected boolean isInstallTypeVersion(String installpath) {
        log.debug("[isInstallTypeVersion] " + getTypeInfo().getVersion());
        String versionFile = getTypeProperty(VERSION_FILE);
        String version = getTypeProperty(VERSION);

        boolean res = false;
        if (super.isInstallTypeVersion(installpath)) {
            try {
                String v = readFileAsString(new File(installpath, versionFile));
                res = v.contains("gemfire.jar=" + version);  // XXX make regexpr.
                if (!res) {
                    log.debug("[isInstallTypeVersion] (" + version + ") verison=" + v);
                }
            } catch (IOException ex) {
                log.error("[isInstallTypeVersion] Error!!!", ex);
            }
        }
        return res;
    }

    private static String readFileAsString(File filePath) throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}
