package org.hyperic.hq.plugin.mule;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxServerControlPlugin;

public class MuleServerControlPlugin extends MxServerControlPlugin {

    private String getMuleHome() {
        String homeProp =
            getTypeProperty(MuleServerDetector.PROC_HOME_PROPERTY);
        return getConfig(homeProp);        
    }

    //ObjectName for control actions, different from ObjectName for monitoring.
    protected String getObjectName() {
        String id = getConfig(MuleServerDetector.PROP_DOMAIN);
        return id + ":" + "name=WrapperManager";
    }

    protected String[] getCommandEnv() {
        return new String[] {
            "MULE_HOME=" + getMuleHome()
        };
    }

    public String getControlProgram() {
        return getMuleHome() + "/bin/mule";
    }

    protected boolean isBackgroundCommand() {
        return true;
    }

    public void doAction(String action, String[] args)
        throws PluginException {

        if (action.equals("stop")) {
            stop();
        }
        else {
            //super doesn't know if this is a program or jmx op
            super.doAction(action, args);
        }
    }

    public int start() {
        String config = getConfig(MuleServerDetector.PROP_CONFIG);

        String[] args = {
             "-config", config,
             "start"
        };

        return doCommand(args);
    }

    //arg is exit code
    public int stop() {
        return invokeMethod("stop", new String[] { "0" });
    }

    public int restart() {
        return invokeMethod("restart");
    }
}
