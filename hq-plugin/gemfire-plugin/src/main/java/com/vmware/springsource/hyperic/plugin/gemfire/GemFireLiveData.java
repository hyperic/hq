package com.vmware.springsource.hyperic.plugin.gemfire;

import com.vmware.springsource.hyperic.plugin.gemfire.collectors.MemberCollector;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class GemFireLiveData extends LiveDataPlugin {

    private static final Log log = LogFactory.getLog(GemFireLiveData.class);
    private static final String[] cmds = {"getMembers", "getDetails", "connectToSystem"};

    public Object getData(String command, ConfigResponse config) throws PluginException {
        Object res = null;

        log.info("command='" + command + "' config='" + config + "'");
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(config.toProperties());
            log.info("mServer="+mServer);
            if ("getDetails".equals(command)) {
                res = getDetails(mServer);
            } else if ("getMembers".equals(command)) {
                res = getMembers(mServer);
            } else if ("connectToSystem".equals(command)) {
                res = connectToSystem(mServer);
            } else {
                throw new PluginException("command '" + command + "' not found");
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
        return res;
    }

    public String[] getCommands() {
        return cmds;
    }

    private static String connectToSystem(MBeanServerConnection mServer) throws Exception {
        Object[] args = new Object[0];
        String[] def = new String[0];
        ObjectName sys = (ObjectName) mServer.invoke(new ObjectName("GemFire:type=Agent"), "connectToSystem", args, def);
        log.debug("[connectToSystem] sys=" + sys);
        return sys.getKeyProperty("id");
    }

    private static String[] getMembers(MBeanServerConnection mServer) throws Exception {
        Object[] args = new Object[0];
        String[] def = new String[0];
        String[] members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);
        return members;
    }

    private static Map getDetails(MBeanServerConnection mServer) throws Exception {
        Object[] args = new Object[0];
        String[] def = new String[0];
        String[] members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);
        Map data = new HashMap();
        for (String member : members) {
            data.put(member, getMemberDetails(mServer, member));
        }
        return data;
    }

    private static Map getMemberDetails(MBeanServerConnection mServer, String member) throws Exception {
        Map details = new HashMap();
        details.putAll(MemberCollector.getMetrics(member, mServer, true));
        return details;
    }
}
