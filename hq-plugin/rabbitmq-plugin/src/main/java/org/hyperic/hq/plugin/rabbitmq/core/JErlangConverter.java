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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.ConnectionCallback;
import org.springframework.erlang.core.ErlangTemplate;
import org.springframework.erlang.support.converter.ErlangConversionException;
import org.springframework.erlang.support.converter.SimpleErlangConverter;
import org.springframework.util.Assert;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JErlangConverter
 * @author Helena Edelson
 */
public class JErlangConverter implements ErlangConverter {

    private static final Log logger = LogFactory.getLog(JErlangConverter.class);

    @Autowired
    private ErlangTemplate erlangTemplate;

    public OtpErlangObject toErlang(Object o) throws ErlangConversionException {
        if (o instanceof String) {
            /** can be binary or atom but we have no use case for atom */
            return new OtpErlangBinary(((String)o).getBytes());
        }
        return null;
    }

    public Object fromErlangRpc(final String module, final String function, String vHost, Class type) throws ErlangConversionException {
        final OtpErlangObject[] args = vHost == null ? new OtpErlangObject[]{} : new OtpErlangObject[]{toErlang(vHost)};

        OtpErlangObject response = (OtpErlangObject) erlangTemplate.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(org.springframework.erlang.connection.Connection connection) throws Exception {
                connection.sendRPC(module, function, new OtpErlangList(args));
                return connection.receiveRPC();
            }
        });

        if (response.toString().startsWith("{badrpc")) {
            throw new ErlangBadRpcException(response.toString());
        }

        return fromErlang(response, vHost, type);
    }

    /**
     * @param response
     * @param virtualHost
     * @param type
     * @return
     * @throws OtpErlangException
     */
    public Object fromErlang(OtpErlangObject response, String virtualHost, Class type) {
        try {
            if (type.isAssignableFrom(Exchange.class)) {
                return convertExchanges(response, virtualHost);
            }
            else if (type.isAssignableFrom(String.class)) {
                return convertVirtualHosts(response);
            }
            else if (type.isAssignableFrom(HypericConnection.class)) {
                return convertConnections(response);
            }
            else if (type.isAssignableFrom(HypericChannel.class) && virtualHost == null) {
                return convertChannels(response);
            }
            else {
                return convertVersion(response);
            }
        }
        catch (OtpErlangException e) {
            throw new ErlangBadRpcException(response.toString());
        }
    }

    /**
     * Get a list of Channels
     * @param response
     * @return
     */
    private List<HypericChannel> convertChannels(OtpErlangObject response) {
        List<HypericChannel> channels = null;

        long items = ((OtpErlangList) response).elements().length;

        if (items > 0) {
            channels = new ArrayList<HypericChannel>();

            if (response instanceof OtpErlangList) {

                for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
                    if (outerList instanceof OtpErlangList) {
                        HypericChannel channel = new HypericChannel();

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

        if (channels != null) {
            logger.debug("Found " + channels.size() + " channels");
        }

        return channels;

    }

    /**
     * Get version
     * @param response
     * @return
     */
    private String convertVersion(OtpErlangObject response) {
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

    /**
     * Convert response object to List of virtual hosts.
     * @param response
     * @return List of String representations of virtual hosts
     */
    private List<String> convertVirtualHosts(OtpErlangObject response) {
        List<String> virtualHosts = null;

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

    /**
     * Get a list of ErlangConnection objects from the response.
     * @param response
     * @return
     * @throws OtpErlangException
     */
    private List<HypericConnection> convertConnections(OtpErlangObject response) throws OtpErlangException {
        List<HypericConnection> connections = null;

        long items = ((OtpErlangList) response).elements().length;

        if (items > 0) {
            connections = new ArrayList<HypericConnection>();

            if (response instanceof OtpErlangList) {

                for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
                    if (outerList instanceof OtpErlangList) {

                        HypericConnection connection = new HypericConnection();

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
                            logger.debug(connection);
                            connections.add(connection);
                        }
                    }
                }
            }
        }

        if (connections != null) {
            logger.debug("Found " + connections.size() + " connections");
        }
        return connections;
    }

    /**
     * rough method, converting to above
     * @param response
     * @param virtualHost
     * @return
     * @throws OtpErlangException
     */
    private List<Exchange> convertExchanges(OtpErlangObject response, String virtualHost) throws OtpErlangException {
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

                        for (OtpErlangObject entry : ((OtpErlangTuple) o).elements()) {

                            if (entry instanceof OtpErlangAtom) {
                                OtpErlangAtom atom = ((OtpErlangAtom) entry);
                                String tmp = getExchangeType(atom);
                                if (tmp != null) {
                                    type = getExchangeType(atom);
                                }
                            } else if (entry instanceof OtpErlangTuple) {

                                for (OtpErlangObject innerObj : ((OtpErlangTuple) entry).elements()) {
                                    if (innerObj instanceof OtpErlangBinary) {
                                        OtpErlangBinary b = (OtpErlangBinary) innerObj;
                                        exchangeName = new String(b.binaryValue());
                                    }
                                }

                            }
                            if (exchangeName != null && !exchangeName.startsWith(virtualHost) && type != null) {
                                exchange = doCreateExchange(exchangeName, virtualHost, type);
                            }
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

    /**
     * ToDo change to isAssignableFrom()
     * @param exchangeName
     * @param vHost
     * @param type
     * @return
     */
    private Exchange doCreateExchange(String exchangeName, String vHost, String type) {
        Exchange exchange = null;
        if (!exchangeName.startsWith(vHost)) {
            if (type.equalsIgnoreCase(TopicExchange.class.getSimpleName())) {
                exchange = new TopicExchange(exchangeName);
            } else if (type.equalsIgnoreCase(DirectExchange.class.getSimpleName())) {
                exchange = new DirectExchange(exchangeName);
            } else if (type.equalsIgnoreCase(FanoutExchange.class.getSimpleName())) {
                exchange = new FanoutExchange(exchangeName);
            }
        }

        return exchange;
    }


}
