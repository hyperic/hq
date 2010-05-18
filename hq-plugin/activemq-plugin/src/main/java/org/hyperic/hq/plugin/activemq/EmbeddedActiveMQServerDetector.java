package org.hyperic.hq.plugin.activemq;

import java.io.File;
import java.util.regex.Pattern;

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

    @Override
    protected File findVersionFile(File dir, Pattern pattern) {
        // In an Embedded ActiveMQ instance, we know we are starting with
        // CATALINA_BASE
        // Give preferential search treatment to webapps/*/WEB-INF/lib for
        // performance gains
        File webapps = new File(dir + File.separator + "webapps");
        if (webapps.exists()) {
            File[] apps = webapps.listFiles();
            for (File app : apps) {
                if (app.isDirectory() &&
                    new File(app + File.separator + "WEB-INF" + File.separator + "lib").exists()) {
                    File versionFile = super.findVersionFile(new File(app + File.separator +
                                                                      "WEB-INF" + File.separator +
                                                                      "lib"), pattern);
                    if (versionFile != null) {
                        return versionFile;
                    }
                }
            }
        }
        return super.findVersionFile(dir, pattern);
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
