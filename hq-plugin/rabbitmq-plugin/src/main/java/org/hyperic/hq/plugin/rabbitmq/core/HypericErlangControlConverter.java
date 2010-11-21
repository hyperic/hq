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
package org.hyperic.hq.plugin.rabbitmq.core;

import com.ericsson.otp.erlang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.erlang.support.converter.ErlangConversionException;
import org.springframework.erlang.support.converter.SimpleErlangConverter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HypericErlangConverter handles jinterface calls that are not currently in Spring AMQP
 * or are in Spring AMQP not working in the Hyperic Agent environment.
 * ToDo clean up conversions
 * @author Helena Edelson
 */
public class HypericErlangControlConverter extends SimpleErlangConverter implements HypericErlangConverter {

    private static final Log logger = LogFactory.getLog(HypericErlangControlConverter.class);

    /**
     * Convert Java Object to Erlang
     * @param o
     * @return
     * @throws ErlangConversionException
     */
    public OtpErlangObject toErlang(Object o) throws ErlangConversionException {
        return o instanceof String ? new OtpErlangBinary(((String) o).getBytes()) : null;
    }

    public Object fromErlang(OtpErlangObject otpErlangObject) throws ErlangConversionException {
        return super.fromErlang(otpErlangObject);
    }

    public Object fromErlangRpc(String s, String s1, OtpErlangObject otpErlangObject) throws ErlangConversionException {
        return super.fromErlangRpc(s, s1, otpErlangObject);
    }

    /**
     * @param response
     * @param args
     * @return
     * @throws OtpErlangException
     */
    public Object fromErlangRpc(OtpErlangObject response, ErlangArgs args) {
        Class type = args.getType();
        String virtualHost = args.getVirtualHost();

        if (type.isAssignableFrom(Exchange.class)) {
            return convertExchanges(response, virtualHost);
        }
        else if (type.isAssignableFrom(String.class)) {
            return convertVirtualHosts(response);
        }
        else if (type.isAssignableFrom(RabbitConnection.class)) {
            return convertConnections(response);
        }
        else if (type.isAssignableFrom(RabbitChannel.class)) {
            return convertChannels(response);
        }
        else if (type.isAssignableFrom(RabbitBinding.class)) {
            return convertBindings(response);
        }
        else if (type.isAssignableFrom(QueueInfo.class)) {
            return convertQueues(response);
        }
        else {
            return convertVersion(response);
        }

    }

    public Object convertQueues(OtpErlangObject response) {
        QueueConverter converter = new QueueConverter();
        return converter.fromErlang(response);
    }

    public Object convertBindings(OtpErlangObject response) {
        BindingConverter converter = new BindingConverter();
        return converter.fromErlang(response);
    }

    public Object convertChannels(OtpErlangObject response) {
        ChannelConverter converter = new ChannelConverter();
        return converter.fromErlang(response);
    }

    public Object convertConnections(OtpErlangObject response) {
        ConnectionConverter converter = new ConnectionConverter();
        return converter.fromErlang(response);
    }

    public Object convertExchanges(OtpErlangObject response, String virtualHost) {
        ExchangeConverter exchangeConverter = new ExchangeConverter();
        return exchangeConverter.fromErlang(response, virtualHost);
    }

    public Object convertVirtualHosts(OtpErlangObject response) {
        VirtualHostConverter converter = new VirtualHostConverter();
        return converter.fromErlang(response);
    }

    public Object convertVersion(OtpErlangObject response) {
        VersionConverter converter = new VersionConverter();
        return converter.fromErlang(response);
    }

    public class QueueConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {
            List<QueueInfo> queues = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {
                queues = new ArrayList<QueueInfo>();
                if (response instanceof OtpErlangList) {
                    OtpErlangList erlangList = (OtpErlangList) response;
                    for (OtpErlangObject element : erlangList.elements()) {
                        QueueInfo queue = new QueueInfo();
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

    public class BindingConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {

            List<RabbitBinding> bindings = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {
                bindings = new ArrayList<RabbitBinding>();
                if (response instanceof OtpErlangList) {
                    OtpErlangList erlangList = (OtpErlangList) response;
                    for (OtpErlangObject element : erlangList.elements()) {
                        RabbitBinding binding = new RabbitBinding();
                        OtpErlangList itemList = (OtpErlangList) element;
                        for (OtpErlangObject item : itemList.elements()) {
                            OtpErlangTuple tuple = (OtpErlangTuple) item;
                            if (tuple.arity() == 2) {
                                String key = tuple.elementAt(0).toString();
                                OtpErlangObject value = tuple.elementAt(1);
                            }
                        }
                    }
                }
            }
            return bindings;
        }
    }

    public class ChannelConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {
            List<RabbitChannel> channels = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {
                channels = new ArrayList<RabbitChannel>();
                if (response instanceof OtpErlangList) {
                    for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
                        if (outerList instanceof OtpErlangList) {
                            RabbitChannel channel = new RabbitChannel();

                            for (OtpErlangObject innerListObj : ((OtpErlangList) outerList).elements()) {
                                if (innerListObj instanceof OtpErlangTuple) {
                                    OtpErlangTuple map = (OtpErlangTuple) innerListObj;
                                    String key = map.elementAt(0).toString();
                                    OtpErlangObject value = map.elementAt(1);

                                    if (key.equals("pid") && value instanceof OtpErlangPid) {
                                        String pid = SimpleErlangConverter.extractPid(value);
                                        channel.setPid(pid.substring(5, pid.length() - 1));
                                    }
                                    if (key.equals("connection") && value instanceof OtpErlangPid) {
                                        String pid = SimpleErlangConverter.extractPid(value);
                                        channel.setConnection(pid.substring(5, pid.length() - 1));
                                    } else if (key.equals("number") && value instanceof OtpErlangLong) {
                                        channel.setNumber(SimpleErlangConverter.extractLong(value));
                                    } else if (key.equals("transactional") && value instanceof OtpErlangAtom) {
                                        channel.setTransactional(((OtpErlangAtom) value).atomValue());
                                    } else if (key.equals("consumer_count") && value instanceof OtpErlangLong) {
                                        channel.setConsumerCount(SimpleErlangConverter.extractLong(value));
                                    } else if (key.equals("messages_unacknowledged") && value instanceof OtpErlangLong) {
                                        channel.setMessagesUnacknowledged(SimpleErlangConverter.extractLong(value));
                                    } else if (key.equals("acks_uncommitted") && value instanceof OtpErlangLong) {
                                        channel.setAcksUncommitted(SimpleErlangConverter.extractLong(value));
                                    } else if (key.equals("prefetch_count") && value instanceof OtpErlangLong) {
                                        channel.setPrefetchCount(SimpleErlangConverter.extractLong(value));
                                    } else if (key.equals("user") && value instanceof OtpErlangBinary) {
                                        channel.setUser(new String(((OtpErlangBinary) value).binaryValue()));
                                    } else if (key.equals("vhost") && value instanceof OtpErlangBinary) {
                                        channel.setvHost(new String(((OtpErlangBinary) value).binaryValue()));
                                    }
                                }
                            }
                            if (channel != null) {
                                channels.add(channel);
                            }
                        }
                    }
                }
            }
            return channels;
        }
    }

    public class VersionConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {
            String version = null;
            long items = ((OtpErlangList) response).elements().length;
            if (items > 0) {
                if (response instanceof OtpErlangList) {
                    for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
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

    public class VirtualHostConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {
            List<String> virtualHosts = new ArrayList<String>();

            if (response != null && response instanceof OtpErlangList) {
                for (OtpErlangObject obj : ((OtpErlangList) response).elements()) {
                    if (obj instanceof OtpErlangBinary) {
                        if (virtualHosts == null) virtualHosts = new ArrayList<String>();
                        virtualHosts.add(new String(((OtpErlangBinary) obj).binaryValue()));
                    }
                }
            }

            return virtualHosts;
        }
    }

    public class ConnectionConverter {

        public Object fromErlang(OtpErlangObject response) throws ErlangConversionException {
            List<RabbitConnection> connections = null;

            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {
                connections = new ArrayList<RabbitConnection>();
                if (response instanceof OtpErlangList) {
                    for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
                        if (outerList instanceof OtpErlangList) {

                            RabbitConnection connection = new RabbitConnection();

                            String host = null;

                            String peerHost = null;

                            int port = 0;

                            int peerPort = 0;

                            for (OtpErlangObject innerListObj : ((OtpErlangList) outerList).elements()) {

                                if (innerListObj instanceof OtpErlangTuple) {
                                    OtpErlangTuple map = (OtpErlangTuple) innerListObj;
                                    String key = map.elementAt(0).toString();
                                    OtpErlangObject value = map.elementAt(1);

                                    if (key.equals("pid") && value instanceof OtpErlangPid) {
                                        String pid = SimpleErlangConverter.extractPid(value);
                                        connection.setPid(pid.substring(5, pid.length() - 1));
                                    } else if (key.equals("address") && value instanceof OtpErlangTuple) {
                                        host = value.toString().replace(",", ".").replace("{", "").replace("}", "");
                                    } else if (key.equals("port") && value instanceof OtpErlangLong) {
                                        port = new Long(SimpleErlangConverter.extractLong(value)).intValue();
                                    } else if (key.equals("peer_address") && value instanceof OtpErlangTuple) {
                                        peerHost = value.toString().replace(",", ".").replace("{", "").replace("}", "");
                                    } else if (key.equals("peer_port") && value instanceof OtpErlangLong) {
                                        peerPort = new Long(SimpleErlangConverter.extractLong(value)).intValue();
                                    } else if (key.equals("recv_oct") && value instanceof OtpErlangLong) {
                                        connection.setOctetsReceived(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("recv_cnt") && value instanceof OtpErlangLong) {
                                        connection.setReceiveCount(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("send_oct") && value instanceof OtpErlangLong) {
                                        connection.setOctetsSent(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("send_cnt") && value instanceof OtpErlangLong) {
                                        connection.setSendCount(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("send_pend") && value instanceof OtpErlangLong) {
                                        connection.setSendCount(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("state") && value instanceof OtpErlangAtom) {
                                        connection.setState(((OtpErlangAtom) value).atomValue());
                                    } else if (key.equals("channels") && value instanceof OtpErlangLong) {
                                        connection.setChannels(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("user") && value instanceof OtpErlangBinary) {
                                        connection.setUsername(new String(((OtpErlangBinary) value).binaryValue()));
                                    } else if (key.equals("vhost") && value instanceof OtpErlangBinary) {
                                        connection.setVhost(new String(((OtpErlangBinary) value).binaryValue()));
                                    } else if (key.equals("timeout") && value instanceof OtpErlangLong) {
                                        connection.setTimeout(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    } else if (key.equals("frame_max") && value instanceof OtpErlangLong) {
                                        connection.setFrameMax(new Long(SimpleErlangConverter.extractLong(value)).intValue());
                                    }
                                }
                            }
                            if (host != null && port > 0) connection.setAddress(host, port);

                            if (peerHost != null && peerPort > 0) connection.setPeerAddress(peerHost, peerPort);

                            if (connection != null) {
                                connections.add(connection);
                            }
                        }
                    }
                }
            }
            return connections;
        }
    }

    public class ExchangeConverter {

        public Object fromErlang(OtpErlangObject response, String virtualHost) throws ErlangConversionException {
            List<Exchange> exchanges = null;
            long items = ((OtpErlangList) response).elements().length;

            if (items > 0) {

                if (response instanceof OtpErlangList) {
                    exchanges = new ArrayList<Exchange>();
                    Exchange exchange = null;
                    String exchangeName = null;
                    Class type = null;

                    for (OtpErlangObject o : ((OtpErlangList) response).elements()) {
                        if (o instanceof OtpErlangTuple) {
                            for (OtpErlangObject entry : ((OtpErlangTuple) o).elements()) {
                                if (entry instanceof OtpErlangAtom) { 
                                    type = getExchangeType((OtpErlangAtom) entry);
                                }
                                else if (entry instanceof OtpErlangTuple) {
                                    for (OtpErlangObject item : ((OtpErlangTuple) entry).elements()) {
                                        if (item instanceof OtpErlangBinary) {
                                            exchangeName = new String(((OtpErlangBinary) item).binaryValue());

                                        }
                                    }
                                }

                                if (exchangeName != null && type != null  && !exchangeName.equalsIgnoreCase("/")) {
                                    exchange = doCreateExchange(exchangeName, type);
                                }
                            }
                        }

                        if (exchange != null) {
                            exchanges.add(exchange);
                        }
                    }
                }
            }
          /*  if (exchanges != null) {
                Assert.isTrue(exchanges.size() == items);
            }*/

            return exchanges;
        }

        /**
         * Returns true if type if atom value matches
         * ExchangeType.topic.name().
         * Currently does not handle type 'system'
         * @param atom
         * @return
         */
        private Class getExchangeType(OtpErlangAtom atom) {
            if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.TOPIC)) {
                return TopicExchange.class;
            }
            else if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.DIRECT)) {
                return DirectExchange.class;
            }
            else if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.FANOUT)) {
                return FanoutExchange.class;
            }
            else if (atom.atomValue().equalsIgnoreCase(ExchangeTypes.HEADERS)) {
                return HeadersExchange.class;
            }

            return null;
        }

        /**
         * @param exchangeName
         * @param type of Exchange
         * @return
         */
        private Exchange doCreateExchange(String exchangeName, Class type) {
            if (type.isAssignableFrom(TopicExchange.class)) {
                return new TopicExchange(exchangeName);
            }
            else if (type.isAssignableFrom(DirectExchange.class)) {
                return exchangeName.length() == 0 ? new DirectExchange(AMQPTypes.DEFAULT_EXCHANGE_NAME)
                        : new DirectExchange(exchangeName);
            }
            else if (type.isAssignableFrom(FanoutExchange.class)) {
                return new FanoutExchange(exchangeName);
            }
            else if (type.isAssignableFrom(HeadersExchange.class) && exchangeName.length() > 0) {
                return new HeadersExchange(exchangeName);
            }
            
            return null;
        }
    }

    /**
     * @author Mark Pollack
     * @author Mark Fisher
     */
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
