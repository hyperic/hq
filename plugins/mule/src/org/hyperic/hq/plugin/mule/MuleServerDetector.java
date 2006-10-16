package org.hyperic.hq.plugin.mule;

import java.io.File;
import java.util.List;

import org.hyperic.hq.product.jmx.MxServerDetector;

public class MuleServerDetector extends MxServerDetector {
    private static final String URL_EXPR =
        "//property[@name=\"connectorServerUrl\"]/@value";

    private static final String PROP_WRAPPER_PID =
        "-Dwrapper.pid=";

    private static final String PROP_WRAPPER_CWD =
        "wrapper.working.dir=";

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
            String url = getXPathValue(configFile, URL_EXPR);
            if ((url != null) && (url.length() != 0)) {
                process.setURL(url);
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
}
