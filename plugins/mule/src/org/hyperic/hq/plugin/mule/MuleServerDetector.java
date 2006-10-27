package org.hyperic.hq.plugin.mule;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;

public class MuleServerDetector extends MxServerDetector {
    private static final String URL_EXPR =
        "//property[@name=\"connectorServerUrl\"]/@value";

    private static final String ID_EXPR =
        "/mule-configuration/@id";

    private static final String PROP_WRAPPER_PID =
        "-Dwrapper.pid=";

    private static final String PROP_WRAPPER_CWD =
        "wrapper.working.dir=";

    private static final String PROP_DOMAIN = "domain";

    private static final String DOMAIN_PREFIX = "Mule";

    private Map _ids = new HashMap();

    //attempt to find the xml config file for this server
    //1.3 has a wrapper parent process with the working.dir
    //we use to resolve relative -config files
    private void configureProcess(MxProcess process) {
        String[] args = process.getArgs();
        String wrapperPid = null;
        String config = null;

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith(PROP_WRAPPER_PID)) {
                wrapperPid = arg.substring(PROP_WRAPPER_PID.length());
            }
            else if (arg.equals("-config")) {
                config = args[i+1];
            }
        }

        if (config == null) {
            return;
        }

        File configFile = new File(config);
        if (!configFile.isAbsolute()) {
            if (wrapperPid != null) {
                args =
                    getProcArgs(Long.parseLong(wrapperPid));
                for (int i=0; i<args.length; i++) {
                    String arg = args[i];
                    if (!arg.startsWith(PROP_WRAPPER_CWD)) {
                        continue;
                    }
                    arg = arg.substring(PROP_WRAPPER_CWD.length());
                    configFile = new File(arg, config);
                    break;
                }
            }
        }

        if (configFile.exists()) {
            try {
                Document doc = getDocument(configFile);
                String url = getXPathValue(doc, URL_EXPR);
                process.setURL(url);
                getLog().debug(configFile + " jmx.url=" + url);
                String id = getXPathValue(doc, ID_EXPR);
                _ids.put(url, id);
            } catch (IOException e) {
                getLog().error("Error parsing: " + configFile, e);
            }
        }
    }

    protected List getServerProcessList() {
        //super class will find the process list
        //we go through each to configure jmx.url
        //from the -config file
        List procs = super.getServerProcessList();

        for (int i=0; i<procs.size(); i++) {
            configureProcess((MxProcess)procs.get(i));
        }

        return procs;
    }

    protected void setProductConfig(ServerResource server,
                                    ConfigResponse config) {

        String url = config.getValue(MxUtil.PROP_JMX_URL);
        String id = (String)_ids.get(url);
        if (id == null) {
            //Using the default jmx service url
            id = (String)_ids.get(null);
        }
        if (id != null) {
            config.setValue(PROP_DOMAIN,
                            DOMAIN_PREFIX + "." + id);
        }

        super.setProductConfig(server, config);
    }
}
