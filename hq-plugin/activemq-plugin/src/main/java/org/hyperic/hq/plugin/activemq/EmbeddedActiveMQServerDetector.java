package org.hyperic.hq.plugin.activemq;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;

/**
 * Server detector for activemq brokers embedded in Tomcat/tc Server instances.
 * Exists to optimize recursive file scanning and to set unique identifiers for
 * any embedded activemq instances that are discovered
 * @author jhickey
 * 
 */
public class EmbeddedActiveMQServerDetector
    extends MxServerDetector {
	
	boolean recursive = false;
	private final static String RECURSIVE_PROP = "activemq.search.recursive";

	public void init(PluginManager manager) throws PluginException {
	    super.init(manager);
	    recursive = "true".equalsIgnoreCase(manager.getProperty(RECURSIVE_PROP, "false"));
	    getLog().debug(RECURSIVE_PROP + "=" + recursive);
	}


    @Override
    protected File findVersionFile(File dir, Pattern pattern) {
        // In an Embedded ActiveMQ instance, we know we are starting with
        // CATALINA_BASE
        // Give preferential search treatment to webapps/*/WEB-INF/lib for
        // performance gains
        File libDir = new File(dir, "lib");
            if (libDir.exists()) {
                File versionFile = super.findVersionFile(libDir, pattern);
                if (versionFile != null) {
                    return versionFile;
                }
            }
    
            File webappsDir = new File(dir, "webapps");
            if (webappsDir.exists()) {
                for( File app: webappsDir.listFiles()) {
                    if (app.isDirectory()) {
                        File wlibDir = new File(app, "WEB-INF" + File.separator + "lib");
                        if (wlibDir.exists()) {
                            File versionFile = super.findVersionFile(wlibDir, pattern);
                            if (versionFile != null) {
                                return versionFile;
                            }
                        }
                    } else if (app.getName().endsWith(".war")) {
                        try {
                            JarFile war = new JarFile(app);
                            Enumeration files = war.entries();
                            while (files.hasMoreElements()) {
                                final String fileName = files.nextElement().toString();
                                if (pattern.matcher(fileName).find()) {
                                    return new File(app + "!" + fileName);
                                }
                            }
                        } catch (IOException ex) {
                            getLog().debug("Error: '"+app+"': "+ex.getMessage(),ex);
                        }
                    }
                }
            }
   
            if (recursive) {
                return super.findVersionFile(dir, pattern);
            }
            return null;

    }

    @Override
    protected ServerResource getServerResource(MxProcess process) {
        ServerResource server = super.getServerResource(process);
        String catalinaBase = server.getInstallPath();
        
        File hq = findVersionFile(new File(catalinaBase), Pattern.compile("hq-common.*\\.jar"));
        if (hq != null) {
            server.setName(getPlatformName()+" HQ ActiveMQ Embedded "+getTypeInfo().getVersion());
        }

        server.setIdentifier(catalinaBase + " Embedded ActiveMQ");
        return server;
    }

}
