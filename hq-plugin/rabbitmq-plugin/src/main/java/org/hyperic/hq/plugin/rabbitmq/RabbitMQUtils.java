/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.objs.Application;
import org.hyperic.hq.plugin.rabbitmq.objs.Queue;
import org.hyperic.hq.plugin.rabbitmq.objs.Status;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public class RabbitMQUtils {

    private static Log log = LogFactory.getLog(RabbitMQUtils.class);

    public static List getQueues(String server, String vHost) throws PluginException {
        OtpErlangObject[] args = new OtpErlangObject[]{new OtpErlangBinary(vHost.getBytes())};
        OtpErlangList queues = (OtpErlangList) executeCommand(server, "rabbit_amqqueue", "info_all", new OtpErlangList(args));
        List res = new ArrayList();
        for (int n = 0; n < queues.arity(); n++) {
            res.add(new Queue((OtpErlangList) queues.elementAt(n)));
        }
        if (log.isDebugEnabled()) {
            log.debug("[getQueues] res=" + res);
        }
        return res;
    }

    public static List getVHost(String server) throws PluginException {
        List res = new ArrayList();
        OtpErlangList vhosts = (OtpErlangList) executeCommand(server, "rabbit_access_control", "list_vhosts", new OtpErlangList());
        for (int n = 0; n < vhosts.arity(); n++) {
            OtpErlangBinary vhost = (OtpErlangBinary) vhosts.elementAt(n);
            res.add(new String(vhost.binaryValue()));
        }
        if (log.isDebugEnabled()) {
            log.debug("[getVHost] res=" + res);
        }
        return res;
    }

    public static String getServerVersion(String server) throws PluginException {
        String serverName = server.split(",")[0]; // XXX test with all server names...
        OtpErlangObject received = executeCommand(serverName, "rabbit", "status", new OtpErlangList());
        Status status = parseStatusObject((OtpErlangList) received);
        Application app = status.getApplication("rabbit");
        return app.getVersion();
    }
    static OtpConnection connection = null;

    private static OtpErlangObject executeCommand(String server, String mod, String fun, OtpErlangList args) throws PluginException {
        if (log.isDebugEnabled()) {
            log.debug("[executeCommand] server=" + server + ", mod=" + mod + ", fun=" + fun + ", args=" + args);
        }

        server=server.split(",")[0];
        
        OtpErlangObject received;
        try {
            if (connection == null) {
                OtpSelf self = new OtpSelf("guest");
                OtpPeer other = new OtpPeer(server);
                connection = self.connect(other);
            }
            connection.sendRPC(mod, fun, args);
            received = connection.receiveRPC();
            if (received instanceof OtpErlangTuple) {
                if (((OtpErlangTuple) received).elementAt(0).toString().equals("badrpc")) {
                    throw new PluginException(received.toString());
                }
            }
        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[executeCommand] received=" + received + " (" + received.getClass() + ")");
        }
        return received;
    }

    private static Status parseStatusObject(OtpErlangList obj) {
        Status res = new Status();
        for (int n = 0; n < obj.arity(); n++) {
            OtpErlangTuple ele = (OtpErlangTuple) obj.elementAt(n);
            String key = ((OtpErlangAtom) ele.elementAt(0)).atomValue();
            if (key.equalsIgnoreCase("running_applications")) {
                OtpErlangList apps = (OtpErlangList) ele.elementAt(1);
                for (int i = 0; i < apps.arity(); i++) {
                    OtpErlangTuple appvals = (OtpErlangTuple) apps.elementAt(i);
                    Application app = new Application(appvals.elementAt(0), appvals.elementAt(1), appvals.elementAt(2));
                    res.addApplication(app);
                }
            } else if (key.equalsIgnoreCase("nodes")) {
                OtpErlangList nodes = (OtpErlangList) ele.elementAt(1);
                for (int i = 0; i < nodes.arity(); i++) {
                    OtpErlangAtom node = (OtpErlangAtom) nodes.elementAt(i);
                    res.addNode(node.atomValue());
                }
            } else if (key.equalsIgnoreCase("running_nodes")) {
                OtpErlangList nodes = (OtpErlangList) ele.elementAt(1);
                for (int i = 0; i < nodes.arity(); i++) {
                    OtpErlangAtom node = (OtpErlangAtom) nodes.elementAt(i);
                    res.addRunningModes(node.atomValue());
                }
            }
        }
        log.debug("[parseStatusObject] res=" + res);
        return res;
    }

    public static Map tupleToMap(OtpErlangTuple list) {
        Map res = new HashMap();
        int n = 0;
        do {
            res.put(list.elementAt(n++).toString(), list.elementAt(n++));
        } while (n < list.arity());
        return res;
    }

    public static Map tupleListToMap(OtpErlangList list) {
        Map res = new HashMap();
        int n = 0;
        for (int i = 0; i < list.arity(); i++) {
            res.putAll(tupleToMap((OtpErlangTuple) list.elementAt(i)));
        }
        return res;
    }

    public static String toString(OtpErlangBinary bin) {
        return new String(bin.binaryValue());
    }
}
