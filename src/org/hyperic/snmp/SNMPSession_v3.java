/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.UserTarget;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * Implements the SNMPSession interface for SNMPv3 sessions by
 * extending the SNMPSession_v2c implementation.  SNMPv3 is
 * only different from v1 or v2c inthe way that a session
 * is initialized.
 */
class SNMPSession_v3 extends SNMPSession_v2c {

    static {
        USM usm =
            new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
        if ("true".equals(System.getProperty("snmpLogging"))) {
            LogFactory.setLogFactory(new Log4jLogFactory());
        }
    }

    SNMPSession_v3() {
        this.version = SnmpConstants.version3;
    }

    protected PDU newPDU() {
        ScopedPDU pdu = new ScopedPDU();
        return pdu;
    }

    private OctetString getPrivPassphrase(String defVal) {
        String val = System.getProperty("snmpPrivPassphrase", defVal);
        if (val == null) {
            return null;
        }
        return new OctetString(val);
    }

    private OID getPrivProtocol(String defVal)
        throws SNMPException {

        String val = System.getProperty("snmpPrivProtocol", defVal);
        if (val == null) {
            return null;
        }
        if (val.equals("DES")) {
            return PrivDES.ID;
        }
        else if ((val.equals("AES128")) || (val.equals("AES"))) {
            return PrivAES128.ID;
        }
        else if (val.equals("AES192")) {
            return PrivAES192.ID;
        }
        else if (val.equals("AES256")) {
            return PrivAES256.ID;
        }
        else {
            throw new SNMPException("Privacy protocol " + val + " not supported");
        }
    }

    void init(String host,
              String port,
              String transport,
              String user,
              String password,
              int authmethod)
        throws SNMPException {

        OID authProtocol =
            authmethod == SNMPClient.AUTH_SHA ? AuthSHA.ID : AuthMD5.ID;
        OctetString securityName = new OctetString(user);
        OctetString authPassphrase =
            password == null ? null : new OctetString(password);
        OctetString privPassphrase = getPrivPassphrase(null); //XXX template option
        OID privProtocol = getPrivProtocol(null); //XXX template option

        UserTarget target = new UserTarget(); 
        target.setSecurityName(securityName);
        if (authPassphrase != null) { 
            if (privPassphrase != null) { 
                target.setSecurityLevel(SecurityLevel.AUTH_PRIV); 
            }
            else { 
                target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV); 
            } 
        }
        else { 
            target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV); 
        } 
        this.target = target;

        initSession(host, port, transport);
        USM usm = this.session.getUSM();
        if (usm.getUserTable().getUser(securityName) != null) {
            return;
        }
        usm.addUser(securityName, new UsmUser(securityName,
                                              authProtocol,
                                              authPassphrase,
                                              privProtocol,
                                              privPassphrase));
    }
}
