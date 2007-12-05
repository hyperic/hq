package org.hyperic.hq.plugin.netdevice;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.snmp.SNMPClient;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

public class SNMPTrapReceiver
    implements CommandResponder {

    static final String PROP_LISTEN_ADDRESS = "snmpTrapReceiver.listenAddress";
    static final String DEFAULT_LISTEN_ADDRESS = "udp:0.0.0.0/162";

    private static SNMPTrapReceiver instance = null;
    private static final Log log =
        LogFactory.getLog(SNMPTrapReceiver.class.getName());

    private MultiThreadedMessageDispatcher _dispatcher;
    private Snmp _snmp;
    private Address _listenAddress;
    private ThreadPool _threadPool;
    private Map _plugins = new HashMap();

    private int _received = 0;

    public static SNMPTrapReceiver getInstance(Properties props)
        throws IOException {
        
        if (instance == null) {
            instance = new SNMPTrapReceiver();
            instance.init(props);
        }

        return instance;
    }

    public static void start(Properties props)
        throws IOException {

        getInstance(props);
    }

    public static void shutdown() throws IOException {
        if (instance != null) {
            log.debug("Shutdown instance");
            instance._threadPool.cancel();
            instance._snmp.close();
            instance = null;
        }
    }

    private static String getPluginKey(String address, String community) {
        return address + "-" + community;        
    }

    private static String getConfig(LogTrackPlugin plugin,
                                   String key, String def) {

        String value = plugin.getConfig(key);
        if (value == null) {
            //commandline-testing
            return plugin.getManager().getProperty(key, def);
        }
        else {
            return value;
        }
    }

    private static String getPluginKey(LogTrackPlugin plugin) {
        String address =
            getConfig(plugin,
                      SNMPClient.PROP_IP,
                      SNMPClient.DEFAULT_IP);

        String community = 
            getConfig(plugin,
                      SNMPClient.PROP_COMMUNITY,
                      SNMPClient.DEFAULT_COMMUNITY);

        return getPluginKey(address, community);
    }

    private LogTrackPlugin getPlugin(String address, String community) {
        String key = getPluginKey(address, community);
        return (LogTrackPlugin)_plugins.get(key);
    }

    public void add(LogTrackPlugin plugin)
        throws IOException {

        String key = getPluginKey(plugin);
        log.debug("Add " + plugin.getName() + " for " + key);
        _plugins.put(key, plugin);
    }

    public static void remove(LogTrackPlugin plugin) {
        if (instance == null) {
            return;
        }
        String key = getPluginKey(plugin);
        log.debug("Remove " + plugin.getName() + " for " + key);
        instance._plugins.remove(key);
    }

    private SNMPTrapReceiver () {}

    private void init(Properties props)
        throws IOException {

        String address =
            props.getProperty(PROP_LISTEN_ADDRESS,
                              DEFAULT_LISTEN_ADDRESS).trim();

        String numThreads =
            props.getProperty("snmpTrapReceiver.numThreads", "1");

        _threadPool =
            ThreadPool.create("SNMPTrapReceiver",
                              Integer.parseInt(numThreads));

        _dispatcher =
            new MultiThreadedMessageDispatcher(_threadPool,
                                               new MessageDispatcherImpl());

        try {
            _listenAddress = GenericAddress.parse(address);
            if (!_listenAddress.isValid()) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IOException("Invalid " +
                                  PROP_LISTEN_ADDRESS + "=" + address);
        }

        log.debug(PROP_LISTEN_ADDRESS + "=" + address);

        TransportMapping transport;
        if (_listenAddress instanceof UdpAddress) {
            transport =
                new DefaultUdpTransportMapping((UdpAddress)_listenAddress);
        }
        else {
            transport =
                new DefaultTcpTransportMapping((TcpAddress)_listenAddress);
        }

        _snmp = new Snmp(_dispatcher, transport);
        _snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
        _snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
        _snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3());

        USM usm =
            new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        _snmp.addCommandResponder(this);
        _snmp.listen();
    }

    public int getTrapsReceived() {
        return _received;
    }

    private String getMessage(CommandResponderEvent event) {
        StringBuffer msg = new StringBuffer();

        Vector vars = event.getPDU().getVariableBindings();
        int size = vars.size();
        for (int i=0; i<size; i++) {
            VariableBinding var = (VariableBinding)vars.get(i);
            msg.append(var.getVariable().toString().trim());
            if (i < size-1) {
                msg.append(", ");
            }
        }        

        return msg.toString();
    }

    public void processPdu(CommandResponderEvent event) {
        _received++;

        Address peer = event.getPeerAddress();
        InetAddress peerAddress;
        if (peer instanceof UdpAddress) {
            peerAddress = ((UdpAddress)peer).getInetAddress();
        }
        else if (peer instanceof TcpAddress) {
            peerAddress = ((TcpAddress)peer).getInetAddress();
        }
        else {
            log.debug("Unsupported transport: " +
                      peer.getClass().getName());
            return;
        }

        String address =
            peerAddress.getHostAddress();

        String community =
            new OctetString(event.getSecurityName()).toString();

        LogTrackPlugin plugin = getPlugin(address, community);

        if (plugin == null) {
            if (log.isDebugEnabled()) {
                log.debug("No plugin for " + address +
                          ", msg=" + getMessage(event));
            }
        }
        else {
            String msg = getMessage(event);
            if (log.isDebugEnabled()) {
                log.debug("Msg=" + msg);
            }
            plugin.reportEvent(System.currentTimeMillis(),
                               LogTrackPlugin.LOGLEVEL_ERROR,
                               address,
                               msg);
        }
    }

    public static void main(String[] args) throws Exception {
        SNMPTrapReceiver.start(System.getProperties());

        System.out.println("Ready");
        System.in.read();
        System.out.print("Shutting down...");
        SNMPTrapReceiver.shutdown();
        System.out.println("done");
    }
}

