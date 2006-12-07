package org.hyperic.hq.plugin.vmware;

import java.util.Properties;

import org.hyperic.sigar.vmware.ConnectParams;

public class VMwareConnectParams extends ConnectParams {
    //vmware client is not thread safe
    static final Object LOCK = new Object();

    public static final String PROP_AUTHD_PORT = "authd.client.port";

    static final int DEFAULT_AUTHD_PORT_INT = 0;

    public static final String DEFAULT_AUTHD_PORT =
        String.valueOf(DEFAULT_AUTHD_PORT_INT);

    public VMwareConnectParams(Properties props) {
        super(getHost(props),
              getPort(props),
              getUser(props),
              getPass(props));
    }

    private static int getPort(Properties props) {
        String port =
            sanitize(props.getProperty(PROP_AUTHD_PORT));
        if (port == null) {
            return DEFAULT_AUTHD_PORT_INT;
        }
        return Integer.parseInt(port);
    }

    private static String sanitize(String prop) {
        if ((prop == null) || (prop.length() == 0)) {
            return null;
        }
        if ((prop.charAt(0) == '%') &&
            (prop.charAt(prop.length()-1) == '%'))
        {
            return null;
        }
        return prop;
    }

    private static String getHost(Properties props) {
        return sanitize(props.getProperty("host"));
    }

    private static String getUser(Properties props) {
        return sanitize(props.getProperty("user"));
    }

    private static String getPass(Properties props) {
        return sanitize(props.getProperty("pass"));
    }
}
