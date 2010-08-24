package com.vmware.springsource.hyperic.plugin.gemfire;

import antlr.collections.List;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class GemFireLiveData extends LiveDataPlugin {

    private static final Log log = LogFactory.getLog(GemFireLiveData.class);
    private static final String[] cmds = {"getMembers", "getDetails", "connectToSystem"};
    private static final int prefixLength = "gemfire.member.".length();

    public Object getData(String command, ConfigResponse config) throws PluginException {
        Object res = null;
        JMXConnector connector = null;

        log.info("command='" + command + "' config='" + config + "'");
        try {
            connector = MxUtil.getMBeanConnector(config.toProperties());
            MBeanServerConnection mServer = connector.getMBeanServerConnection();
            if ("getDetails".equals(command)) {
                res = getDetails(mServer);
            } else if ("getMembers".equals(command)) {
                res = getMembers(mServer);
            } else if ("connectToSystem".equals(command)) {
                res = connectToSystem(mServer);
            } else {
                throw new PluginException("command '" + command + "' not found");
            }
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);

        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
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
        Map details;
        Map<String, Object> memberDetails;
        for (String menber : members) {
            details = new HashMap();
            data.put(menber, details);
            Object[] args2 = {menber};
            String[] def2 = {String.class.getName()};
            memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            for (String k : memberDetails.keySet()) {
                details.put(k.substring(prefixLength, k.lastIndexOf('.')), memberDetails.get(k));
            }
        }
        return data;
    }
}
