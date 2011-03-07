/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.amqp.admin;

import org.hyperic.hq.amqp.admin.erlang.*;
import org.hyperic.hq.amqp.util.MessageConstants;

import org.hyperic.hq.amqp.admin.RabbitErlangConverter.*;

import org.hyperic.hq.product.PlatformDetector;
import org.springframework.amqp.core.Exchange;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Helena Edelson
 */
public class RabbitAdminTemplate {

    private final Map<Class, ControlAction> controlActions;

    private ErlangTemplate erlangTemplate = new RabbitErlangTemplate();

    public RabbitAdminTemplate() {
        this.controlActions = RabbitErlangConverter.getControlActions();
    }

    @SuppressWarnings("unchecked")
    public Node getNodeStatus() {
        return (Node) erlangTemplate.executeAndConvertRpc(getControlAction(NodeStatus.class));
    }

    /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() {
        return (List<String>) erlangTemplate.executeAndConvertRpc(getControlAction(ListVirtualHosts.class));
    }

    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges() {
        return getExchanges(MessageConstants.DEFAULT_VHOST);
    }

    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges(String virtualHost) {
        return (List<Exchange>) erlangTemplate.executeAndConvertRpc(getControlAction(ListExchanges.class), getBytes(virtualHost));
    }

    @SuppressWarnings("unchecked")
    public List<Queue> getQueues() {
        return getQueues(MessageConstants.DEFAULT_VHOST);
    }

    @SuppressWarnings("unchecked")
    public List<Queue> getQueues(String virtualHost) {
        return (List<Queue>) erlangTemplate.executeAndConvertRpc(getControlAction(ListQueues.class), getBytes(virtualHost));
    }

    public void addUser(String username, String password) {
        erlangTemplate.executeAndConvertRpc(getControlAction(AddUser.class), getBytes(username), getBytes(password));
    }

    public void deleteUser(String username) {
        erlangTemplate.executeAndConvertRpc(getControlAction(DeleteUser.class), getBytes(username));
    }

    public void changeUserPassword(String username, String newPassword) {
        erlangTemplate.executeAndConvertRpc(getControlAction(ChangePassword.class), getBytes(username), getBytes(newPassword));
    }

    @SuppressWarnings("unchecked")
    public List<String> listUsers() {
        return (List<String>) erlangTemplate.executeAndConvertRpc(getControlAction(ListUsers.class));
    }

    public BrokerStatus getBrokerStatus() {
        try {
            return (BrokerStatus) erlangTemplate.executeAndConvertRpc(getControlAction(ListStatus.class));
        } catch (RpcException e) {
            return new BrokerStatus(Collections.<Node>emptyList(), Collections.<Node>emptyList());
        }
    }

    /**
     * Returns the version of the broker based on running nodes.
     * @return the broker version
     */
    public String getVersion() {
        return (String) erlangTemplate.executeAndConvertRpc(BrokerVersion.create());
    }

    /**
     * Returns whether the broker is alive.
     * @return true if alive, false if not
     */
    public boolean isBrokerAlive() {
        return getBrokerStatus().isAlive();
    }

    /**
     * Returns whether the broker is running.
     * @return
     */
    public boolean isBrokerRunning() {
        return getBrokerStatus().isRunning();
    }

    public boolean getRunningNode(String name) {
        List<Node> runningNodes = getBrokerStatus().getRunningNodes();
        if (runningNodes.size() > 0) {
            for (Node node : runningNodes) {
                if (node.getName().contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the ControlAction by class key.
     * @param type the ControlAction type
     * @return the org.springframework.erlang.support.ControlAction
     */
    protected ControlAction getControlAction(Class type) {
        return controlActions.get(type);
    }

    /**
     * Safely convert a string to its bytes using the encoding provided.
     * @param string the value to convert
     * @return the bytes from the string using the encoding provided
     * @throws IllegalStateException if the encoding is ont supported
     */
    private byte[] getBytes(String string) {
        try {
            return string.getBytes(MessageConstants.ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding: " + MessageConstants.ENCODING);
        }
    }

    /**
     * Returns the prefix (eg. rabbit_2 or rabbit_3) of the node name.
     * @param hostname     The host name that the Rabbit node is running on.
     * @param peerNodeName
     * @return
     */
    public static String validatePeerNodeName(String hostname, String peerNodeName) {
        if (PlatformDetector.IS_WIN32) {
            Pattern p = Pattern.compile("([^@]+)@");
            Matcher m = p.matcher(peerNodeName);
            String prefix = m.find() ? m.group(1) : null;
            return prefix + hostname.toUpperCase();
        } else {
            return peerNodeName;
        }
    }

     /**
       * Returns the host name for a given node name.
       * @param nodeName
       * @return
       */
      public static String getHostFromNode(String nodeName) {
          if (nodeName != null && nodeName.length() > 0) {
              Pattern p = Pattern.compile("@([^\\s.]+)");
              Matcher m = p.matcher(nodeName);
              return (m.find()) ? m.group(1) : null;
          }
          return null;
      }

}
