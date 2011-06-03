/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

public class JBossWebServerDetector extends TomcatServerDetector {

    private static final String EMBEDDED_TOMCAT = "jbossweb-tomcat";
    private String jbossPath;

    @Override
    public String getTypeProperty(String type, String name) {
        String val = super.getTypeProperty(type, name);
        if (name.equals("OBJECT_NAME")) {
            val = val.replace("Catalina", "jboss.web");
        }
        return val;
    }

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        getLog().debug("[getServerResources]");
        List servers = new ArrayList();

        List procs = getServerProcessList();
        for (int i = 0; i < procs.size(); i++) {
            MxProcess process = (MxProcess) procs.get(i);

            String config = "default";
            List args = Arrays.asList(process.getArgs());
            if (args.contains("-c")) {
                config = (String) args.get(args.indexOf("-c") + 1);
            }

            if (isInstallTypeVersion(process.getInstallPath() + "/../server/" + config)) {
                ServerResource server = getServerResource(process);
                ConfigResponse cfg = new ConfigResponse();
                cfg.setValue("jmx.url", getJMXURL(process));
                setProductConfig(server, cfg);
                servers.add(server);
            }
        }
        return servers;
    }

    protected String getJMXURL(MxProcess process){
        return "ptql:" + getProcQuery() + ",Args.*.ct=" + process.getInstallPath();
    }

    @Override
    protected boolean isInstallTypeVersion(String path) {
        String versionFile = getTypeProperty("VERSION_FILE");
        String jbossVersion = getTypeProperty("JBOSS_VERSION");
        boolean ok = false;
        String v = "null";

        File f = new File(path, versionFile);
        if (f.exists()) {
            JarFile jarfile;
            try {
                jarfile = new JarFile(f);
                Manifest manifest = jarfile.getManifest();
                Attributes attrs = (Attributes) manifest.getMainAttributes();
                v = attrs.getValue("Specification-Version");
            } catch (IOException ex) {
                getLog().debug("[isInstallTypeVersion] " + ex.getMessage(), ex);
            }
        }

        ok = v.startsWith(jbossVersion);
        getLog().debug("[isInstallTypeVersion] ok=" + ok + " version" + v + "(" + jbossVersion + ") file=" + f);
        return ok;
    }

    @Override
    protected void setJmxUrl(MxProcess process, ConfigResponse config) {
    }
}
