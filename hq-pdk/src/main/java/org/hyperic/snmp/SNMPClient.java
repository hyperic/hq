/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.snmp;

import java.io.IOException;
import java.io.File;
import java.util.Properties;

import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;

public class SNMPClient {
    private static final int VERSION_1 = 1;
    private static final int VERSION_2C = 2;
    private static final int VERSION_3 = 3;

    static final int AUTH_MD5 = 0;
    static final int AUTH_SHA = 1;

    public static final String DEFAULT_IP = "127.0.0.1";

    public static final int DEFAULT_PORT = 161;

    public static final String DEFAULT_TRANSPORT = "udp";
    public static final String DEFAULT_USERNAME = "username";
    public static final String DEFAULT_PASSWORD = "";
    public static final String DEFAULT_AUTHTYPE = "none";
    public static final String DEFAULT_PRIV_TYPE = "none";

    public static final String DEFAULT_PORT_STRING = String.valueOf(DEFAULT_PORT);
    public static final String DEFAULT_COMMUNITY = System.getProperty("snmp.defaultCommunity", "public");
    
    public static final String DEFAULT_RETRIES = "1";
    public static final String DEFAULT_TIMEOUT = "1500";

    public static final String[] VALID_VERSIONS = { "v1", "v2c", "v3" };

    public static final String PROP_IP = "snmpIp";
    public static final String PROP_PORT = "snmpPort";
    public static final String PROP_TRANSPORT = "snmpTransport";
    public static final String PROP_VERSION = "snmpVersion";
    public static final String PROP_COMMUNITY = "snmpCommunity";
    public static final String PROP_USER = "snmpUser";
    public static final String PROP_PASSWORD = "snmpPassword";
    public static final String PROP_AUTHTYPE = "snmpAuthType";
    public static final String PROP_PRIV_TYPE = "snmpPrivacyType";
    public static final String PROP_PRIV_PASSPHRASE = "snmpPrivacyPassPhrase";
    public static final String PROP_SECURITY_CONTEXT = "snmpSecurityContext";
    public static final String PROP_RETRIES = "snmpRetries";
    public static final String PROP_TIMEOUT = "snmpTimeout";

    private static Log log = LogFactory.getLog(SNMPClient.class);

    // Cache should be configurable by subclasses...
    private static int CACHE_EXPIRE_DEFAULT = 60 * 5 * 1000; // 5 minutes

    private static IntHashMap sessionCache = null;
    private static MIBTree mibTree;

    private int sessionCacheExpire = CACHE_EXPIRE_DEFAULT;

    private static int parseVersion(String version) {
        if (version == null) {
            throw new IllegalArgumentException("version is null");
        }

        if (version.equalsIgnoreCase("v1")) {
            return VERSION_1;
        } else if (version.equalsIgnoreCase("v2c")) {
            return VERSION_2C;
        } else if (version.equalsIgnoreCase("v3")) {
            return VERSION_3;
        }

        throw new IllegalArgumentException("unknown version: " + version);
    }

    public SNMPClient() {
    }

    public void addMIBs(String jar, String[] accept) {
        try {
            mibTree.parse(new File(jar), accept);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void addMIBs(String[] mibs) {
        for (int i = 0; i < mibs.length; i++) {
            File file = new File(mibs[i]);

            if (file.exists()) {
                try {
                    mibTree.parse(file);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.debug("MIB '" + file + "' does not exist");
            }
        }
    }

    static synchronized OID getMibOID(String mibName) throws MIBLookupException {
        int[] oid = mibTree.getOID(mibName);

        if (oid == null) {
            String msg = "Failed to lookup MIB for name=" + mibName;

            String unfound = mibTree.getLastLookupFailure();

            if (!mibName.equals(unfound)) {
                msg += " (last lookup failure=" + unfound + ")";
            }

            throw new MIBLookupException(msg);
        }

        return new OID(oid);
    }

    public static String getOID(String mibName) {
        try {
            return getMibOID(mibName).toString();
        } catch (MIBLookupException e) {
            return null;
        }
    }

    /*
     * Begins a "session" with an SNMP agent.
     * @param version The version of SNMP to use. Can be one of the following
     * values: VERSION_1, VERSION_2C, VERSION_3
     * @return A SNMPSession object to be used in all future communications with
     * the SNMP agent.
     */
    static SNMPSession startSession(int version) throws SNMPException {
        switch (version) {
        case VERSION_1:

            return new SNMPSession_v1();

        case VERSION_2C:

            return new SNMPSession_v2c();

        case VERSION_3:

            return new SNMPSession_v3();

        default:

            throw new SNMPException("Invalid SNMP Version: " + version);
        }
    }

    public boolean init(Properties props) throws SNMPException {
        // Mainly for debugging...
        final String prop = "snmp.sessionCacheExpire";

        String expire = props.getProperty(prop);

        if (expire != null) {
            this.sessionCacheExpire = Integer.parseInt(expire) * 1000;
        }

        if (mibTree == null) {
            mibTree = MIBTree.getInstance();

            sessionCache = new IntHashMap();
        }

        return true;
    }

    public SNMPSession getSession(ConfigResponse config) throws SNMPException {
        return getSession(config.toProperties());
    }

    public SNMPSession getSession(Properties props) throws SNMPException {
        String address = props.getProperty(PROP_IP, DEFAULT_IP);
        String port = props.getProperty(PROP_PORT, DEFAULT_PORT_STRING);
        String version = props.getProperty(PROP_VERSION, VALID_VERSIONS[1]);
        String community = props.getProperty(PROP_COMMUNITY, DEFAULT_COMMUNITY);
        String transport = props.getProperty(PROP_TRANSPORT, DEFAULT_TRANSPORT);
        String user = props.getProperty(PROP_USER, DEFAULT_USERNAME);
        String authtype = props.getProperty(PROP_AUTHTYPE, DEFAULT_AUTHTYPE);
        String authpass = props.getProperty(PROP_PASSWORD, DEFAULT_PASSWORD);
        String privtype = props.getProperty(PROP_PRIV_TYPE, DEFAULT_PRIV_TYPE);
        String privpass = props.getProperty(PROP_PRIV_PASSPHRASE, DEFAULT_PASSWORD);
        String retries = props.getProperty(PROP_RETRIES, DEFAULT_RETRIES);
        String timeout = props.getProperty(PROP_TIMEOUT, DEFAULT_TIMEOUT);

        SNMPSession session = null;

        int id = address.hashCode() ^ port.hashCode() ^ 
                 version.hashCode() ^ community.hashCode() ^
                 transport.hashCode() ^ user.hashCode() ^
                 authtype.hashCode() ^ authpass.hashCode() ^
                 privtype.hashCode() ^ privpass.hashCode() ^
                 retries.hashCode() ^ timeout.hashCode();

        synchronized (sessionCache) {
            session = (SNMPSession) sessionCache.get(id);
        }

        if (session != null) {
            return session;
        }

        int snmpVersion = parseVersion(version);

        try {
            session = startSession(snmpVersion);

            switch (snmpVersion) {
            case SNMPClient.VERSION_1:

            case SNMPClient.VERSION_2C:

                ((SNMPSession_v1) session).init(address, port, community, transport, retries, timeout);

                break;

            case SNMPClient.VERSION_3:

                ((SNMPSession_v3) session).init(address, port, transport, user, 
                                                authtype, authpass, privtype, privpass, retries, timeout);

                break;

            default:

                throw new SNMPException("Unsupported SNMP version: " + snmpVersion);
            }
        } catch (SNMPException e) {
            String msg = "Failed to initialize SNMP session: " + e.getMessage();

            throw new SNMPException(msg, e);
        }

        session = SNMPSessionCache.newInstance(session, this.sessionCacheExpire);

        synchronized (sessionCache) {
            sessionCache.put(id, session);
        }

        return session;
    }
}
