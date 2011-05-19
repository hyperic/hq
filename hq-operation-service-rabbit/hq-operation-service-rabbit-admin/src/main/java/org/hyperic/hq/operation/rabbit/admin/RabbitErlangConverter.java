/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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
 */
package org.hyperic.hq.operation.rabbit.admin;

import com.ericsson.otp.erlang.*;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.admin.erlang.*;
import org.springframework.amqp.core.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Helena Edelson
 */
public class RabbitErlangConverter extends SimpleErlangConverter implements ErlangConverter {

    private static Map<Class, ControlAction> controlActions;

    private static String VIRTUAL_HOST;

    /**
     * Initializes a map of ControlActions by function.
     */
    static {
        ControlAction[] actions = new ControlAction[]{
                AddUser.create(), DeleteUser.create(), ChangePassword.create(), ListUsers.create(), NodeStatus.create(),
                ListStatus.create(), ListQueues.create(), StartBrokerApplication.create(), ListExchanges.create(),
                StopBrokerApplication.create(), StopNode.create(), ResetNode.create(), ForceResetNode.create(), ListVirtualHosts.create()
        };

        controlActions = new HashMap<Class, ControlAction>();

        for (ControlAction action : actions) {
            controlActions.put(action.getKey(), action);
        }
    }

    public RabbitErlangConverter() {
        setVirtualHost(MessageConstants.DEFAULT_VHOST);
    }

    public Object fromErlang(ControlAction action, OtpErlangObject erlangObject) throws ConversionException {
        ErlangConverter converter = getConverter(action.getKey());
        return converter != null ? converter.fromErlang(erlangObject) : super.fromErlang(erlangObject);
    }

    public void setVirtualHost(String virtualHost) {
        VIRTUAL_HOST = virtualHost;
    }

    protected ErlangConverter getConverter(Class type) {
        ControlAction action = controlActions.get(type);
        return action != null ? action.getConverter() : this;
    }

    public static Map<Class, ControlAction> getControlActions() {
        return controlActions;
    }

    public static class ListUsers extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ListUsers.class, "rabbit_auth_backend_internal", "list_users", new ListUsersConverter());
        }
    }

    public static class AddUser extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(AddUser.class, "rabbit_auth_backend_internal", "add_user");
        }
    }

    public static class DeleteUser extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(DeleteUser.class, "rabbit_auth_backend_internal", "delete_user");
        }
    }

    public static class ChangePassword extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ChangePassword.class, "rabbit_auth_backend_internal", "change_password");
        }
    }

    public static class ListVirtualHosts extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ListVirtualHosts.class, "rabbit_access_control", "list_vhosts");
        }
    }

    public static class StartBrokerApplication extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(StartBrokerApplication.class, "rabbit", "start");
        }
    }

    public static class StopBrokerApplication extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(StopBrokerApplication.class, "rabbit", "stop");
        }
    }

    public static class StopNode extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(StopNode.class, "rabbit", "stop_and_halt");
        }
    }

    public static class ResetNode extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ResetNode.class, "rabbit_mnesia", "reset");
        }
    }

    public static class ForceResetNode extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ForceResetNode.class, "rabbit_mnesia", "force_reset");
        }
    }

    public static class NodeStatus extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(NodeStatus.class, "rabbit_mnesia", "status", new NodeStatusConverter());
        }
    }

    public static class ListStatus extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ListStatus.class, "rabbit", "status", new StatusConverter());
        }
    }

    public static class ListQueues extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ListQueues.class, "rabbit_amqqueue", "info_all", new QueueConverter());
        }
    }

    public static class ListExchanges extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(ListExchanges.class, "rabbit_exchange", "list", new ExchangeConverter());
        }
    }

    /* TODO */

    public static class ListBindings extends RabbitControlAction {
        public static ControlAction create() {
            return null;
        }
    }

    public static class BrokerVersion extends RabbitControlAction {
        public static ControlAction create() {
            return new RabbitControlAction(BrokerVersion.class, "rabbit", "status", new VersionConverter());
        }
    }


    public static class VersionConverter extends SimpleErlangConverter {
        @Override
        public Object fromErlang(OtpErlangObject erlangObject) throws ConversionException {
            String version = null;
            long items = ((OtpErlangList) erlangObject).elements().length;
            if (items > 0) {
                if (erlangObject instanceof OtpErlangList) {
                    for (OtpErlangObject outerList : ((OtpErlangList) erlangObject).elements()) {
                        if (outerList instanceof OtpErlangTuple) {
                            OtpErlangTuple entry = (OtpErlangTuple) outerList;
                            String key = entry.elementAt(0).toString();
                            if (key.equals("running_applications") && entry.elementAt(1) instanceof OtpErlangList) {
                                OtpErlangList value = (OtpErlangList) entry.elementAt(1);

                                Pattern p = Pattern.compile("\"(\\d+\\.\\d+(?:\\.\\d+)?)\"}$");
                                Matcher m = p.matcher(value.elementAt(0).toString());
                                version = m.find() ? m.group(1) : null;
                            }
                        }
                    }
                }
            }

            return version;
        }
    }

    public static class ListUsersConverter extends SimpleErlangConverter {

        @Override
        public Object fromErlang(OtpErlangObject erlangObject) throws ConversionException {

            List<String> users = new ArrayList<String>();
            if (erlangObject instanceof OtpErlangList) {
                OtpErlangList erlangList = (OtpErlangList) erlangObject;
                for (OtpErlangObject obj : erlangList) {
                    String value = extractString(obj);
                    if (value != null) {
                        users.add(value);
                    }
                }
            }
            return users;
        }

        private String extractString(OtpErlangObject obj) {

            if (obj instanceof OtpErlangBinary) {
                OtpErlangBinary binary = (OtpErlangBinary) obj;
                return new String(binary.binaryValue());
            } else if (obj instanceof OtpErlangTuple) {
                OtpErlangTuple tuple = (OtpErlangTuple) obj;
                return extractString(tuple.elementAt(0));
            }
            return null;
        }
    }

    public static class NodeStatusConverter extends SimpleErlangConverter {

        @Override
        public Object fromErlang(OtpErlangObject erlangObject) throws ConversionException {
            Node node = null;
            if (erlangObject instanceof OtpErlangList) {
                OtpErlangList erlangList = (OtpErlangList) erlangObject;
                OtpErlangTuple nodeTuple = (OtpErlangTuple) erlangList.elementAt(0);
                OtpErlangTuple runningNodesTuple = (OtpErlangTuple) erlangList.elementAt(1);

                OtpErlangList nodeInfo = (OtpErlangList) nodeTuple.elementAt(1);
                OtpErlangList names = (OtpErlangList) ((OtpErlangTuple) nodeInfo.elementAt(0)).elementAt(1);
                node = new Node((names.elementAt(0)).toString());
                /* TODO run cluster and check if we can just use the one tuple */
                OtpErlangList cluster = (OtpErlangList) runningNodesTuple.elementAt(1);
                List<String> peers = new ArrayList<String>();
                for (OtpErlangObject erlangNodeName : cluster) {
                    peers.add(erlangNodeName.toString());
                }
                node.setPeersInCluster(peers);
            }

            return node;
        }
    }

    public static class StatusConverter extends SimpleErlangConverter {

        @Override
        public Object fromErlang(OtpErlangObject erlangObject) throws ConversionException {
            List<Node> nodes = new ArrayList<Node>();
            List<Node> runningNodes = new ArrayList<Node>();
            if (erlangObject instanceof OtpErlangList) {
                OtpErlangList erlangList = (OtpErlangList) erlangObject;

                OtpErlangTuple nodesTuple = (OtpErlangTuple) erlangList.elementAt(1);
                OtpErlangList nodesList = (OtpErlangList) nodesTuple.elementAt(1);
                extractNodes(nodes, nodesList);

                OtpErlangTuple runningNodesTuple = (OtpErlangTuple) erlangList.elementAt(2);
                nodesList = (OtpErlangList) runningNodesTuple.elementAt(1);
                extractNodes(runningNodes, nodesList);

            }

            return new BrokerStatus(nodes, runningNodes);
        }

        private void extractNodes(List<Node> nodes, OtpErlangList nodesList) {
            for (OtpErlangObject erlangNodeName : nodesList) {
                String nodeName = erlangNodeName.toString();
                nodes.add(new Node(nodeName));
            }
        }
    }


    public static class QueueConverter extends SimpleErlangConverter {

        @Override
        public Object fromErlang(OtpErlangObject response) throws ConversionException {
            List<org.hyperic.hq.operation.rabbit.admin.erlang.Queue> queues = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {
                queues = new ArrayList<org.hyperic.hq.operation.rabbit.admin.erlang.Queue>();
                if (response instanceof OtpErlangList) {
                    OtpErlangList erlangList = (OtpErlangList) response;
                    for (OtpErlangObject element : erlangList.elements()) {
                        org.hyperic.hq.operation.rabbit.admin.erlang.Queue queue = new org.hyperic.hq.operation.rabbit.admin.erlang.Queue();
                        OtpErlangList itemList = (OtpErlangList) element;
                        for (OtpErlangObject item : itemList.elements()) {
                            OtpErlangTuple tuple = (OtpErlangTuple) item;
                            if (tuple.arity() == 2) {
                                String key = tuple.elementAt(0).toString();
                                OtpErlangObject value = tuple.elementAt(1);
                                switch (QueueField.toQueueField(key)) {
                                    case name:
                                        queue.setName(extractNameValueFromTuple((OtpErlangTuple) value));
                                        break;
                                    case transactions:
                                        queue.setTransactions(extractLong(value));
                                        break;
                                    case acks_uncommitted:
                                        queue.setAcksUncommitted(extractLong(value));
                                        break;
                                    case consumers:
                                        queue.setConsumers(extractLong(value));
                                        break;
                                    case pid:
                                        queue.setPid(extractPid(value));
                                        break;
                                    case durable:
                                        queue.setDurable(extractAtomBoolean(value));
                                        break;
                                    case messages:
                                        queue.setMessages(extractLong(value));
                                        break;
                                    case memory:
                                        queue.setMemory(extractLong(value));
                                        break;
                                    case auto_delete:
                                        queue.setAutoDelete(extractAtomBoolean(value));
                                        break;
                                    case messages_ready:
                                        queue.setMessagesReady(extractLong(value));
                                        break;
                                    case arguments:
                                        OtpErlangList list = (OtpErlangList) value;
                                        if (list != null) {
                                            String[] args = new String[list.arity()];
                                            for (int i = 0; i < list.arity(); i++) {
                                                OtpErlangObject obj = list.elementAt(i);
                                                args[i] = obj.toString();
                                            }
                                            queue.setArguments(args);
                                        }
                                        break;
                                    case messages_unacknowledged:
                                        queue.setMessagesUnacknowledged(extractLong(value));
                                        break;
                                    case messages_uncommitted:
                                        queue.setMessageUncommitted(extractLong(value));
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        queues.add(queue);
                    }
                }
            }

            return queues;
        }

        private boolean extractAtomBoolean(OtpErlangObject value) {
            return ((OtpErlangAtom) value).booleanValue();
        }

        private String extractNameValueFromTuple(OtpErlangTuple value) {
            Object nameElement = value.elementAt(3);
            return new String(((OtpErlangBinary) nameElement).binaryValue());
        }

    }


    public static class ExchangeConverter extends SimpleErlangConverter {

        public Object fromErlang(OtpErlangObject response) throws ConversionException {
            List<Exchange> exchanges = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {

                if (response instanceof OtpErlangList) {
                    exchanges = new ArrayList<Exchange>();
                    Exchange exchange = null;
                    String exchangeName = null;
                    String type = null;

                    for (OtpErlangObject o : ((OtpErlangList) response).elements()) {
                        if (o instanceof OtpErlangTuple) {
                            OtpErlangObject[] elements = ((OtpErlangTuple) o).elements();
                            OtpErlangTuple map = (OtpErlangTuple) elements[1];
                            exchangeName = new String(((OtpErlangBinary) map.elementAt(3)).binaryValue());
                            type = getExchangeType((OtpErlangAtom) elements[2]);

                            if (exchangeName != null && type != null) {
                                exchange = doCreateExchange(exchangeName, type);
                            }
                        }

                        if (exchange != null) {
                            exchanges.add(exchange);
                        }
                    }
                }
            }
            if (exchanges != null) {
                Assert.isTrue(exchanges.size() == items);
            }

            return exchanges;
        }


        /**
         * Returns true if type if atom value matches
         * ExchangeType.topic.name().
         * Currently only works for direct or topic types.
         * @param atom
         * @return
         */
        private String getExchangeType(OtpErlangAtom atom) {
            String type = null;
            if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.TOPIC)) {
                type = TopicExchange.class.getSimpleName();
            } else if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.DIRECT)) {
                type = DirectExchange.class.getSimpleName();
            } else if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.FANOUT)) {
                type = FanoutExchange.class.getSimpleName();
            }

            return type;
        }


        private Exchange doCreateExchange(String exchangeName, String type) {
            Exchange exchange = null;
            if (type.equalsIgnoreCase(TopicExchange.class.getSimpleName())) {
                exchange = new TopicExchange(exchangeName);
            } else if (type.equalsIgnoreCase(DirectExchange.class.getSimpleName())) {
                exchange = new DirectExchange(exchangeName);
            } else if (type.equalsIgnoreCase(FanoutExchange.class.getSimpleName())) {
                exchange = new FanoutExchange(exchangeName);
            }

            return exchange;
        }
    }

    private enum QueueField {
        transactions, acks_uncommitted, consumers, pid, durable, messages, memory, auto_delete, messages_ready,
        arguments, name, messages_unacknowledged, messages_uncommitted, NOVALUE;

        public static QueueField toQueueField(String str) {
            try {
                return valueOf(str);
            } catch (Exception ex) {
                return NOVALUE;
            }
        }
    }
}
