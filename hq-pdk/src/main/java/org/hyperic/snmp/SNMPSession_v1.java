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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

class SNMPSession_v1 implements SNMPSession {
    protected int version;
    protected AbstractTarget target;
    protected Snmp session;
    private Address address;
    private static Snmp sessionInstance = null;
    protected static Log log = LogFactory.getLog("SNMPSession");

    private Snmp getSessionInstance() throws IOException {
        if (sessionInstance == null) {
            String listen = "0.0.0.0/0";

            AbstractTransportMapping transport;

            if (this.address instanceof TcpAddress) {
                transport = new DefaultTcpTransportMapping(new TcpAddress(listen));
            } else {
                transport = new DefaultUdpTransportMapping(new UdpAddress(listen));
            }

            sessionInstance = new Snmp(transport);
            sessionInstance.listen();
        }

        return sessionInstance;
    }

    SNMPSession_v1() {
        this.version = SnmpConstants.version1;
    }

    protected void initSession(String address, String port, String transport, String retries, String timeout) throws SNMPException {
        if (address == null) {
            address = SNMPClient.DEFAULT_IP;
        }

        if (port == null) {
            port = SNMPClient.DEFAULT_PORT_STRING;
        }

        this.address = GenericAddress.parse(transport + ":" + address + "/" + port);

        this.target.setAddress(this.address);
        this.target.setVersion(this.version);
        this.target.setRetries(Integer.parseInt(retries));
        this.target.setTimeout(Integer.parseInt(timeout));

        try {
            this.session = getSessionInstance();
        } catch (IOException e) {
            throw new SNMPException(e.getMessage(), e);
        }
    }

    void init(String address, String port, String community, String transport, String retries, String timeout) throws SNMPException {
        CommunityTarget target = new CommunityTarget();

        if (community == null) {
            community = SNMPClient.DEFAULT_COMMUNITY;
        }

        target.setCommunity(new OctetString(community));

        this.target = target;

        initSession(address, port, transport, retries, timeout);
    }

    protected static OID getOID(String name) throws MIBLookupException {
        MIBTree mibTree = MIBTree.getInstance();

        int[] oid = mibTree.getOID(name);

        if (oid == null) {
            String msg = "Failed to lookup OID for name=" + name;

            String unfound = mibTree.getLastLookupFailure();

            if (!name.equals(unfound)) {
                msg += " (last lookup failure=" + unfound + ")";
            }

            throw new MIBLookupException(msg);
        }

        return new OID(oid);
    }

    protected PDU newPDU() {
        return new PDU();
    }

    protected PDU getPDU(String oid, int type) throws MIBLookupException {
        return getPDU(getOID(oid), type);
    }

    protected PDU getPDU(OID oid, int type) {
        PDU pdu = newPDU();

        pdu.setType(type);

        if (type == PDU.GETBULK) {
            pdu.setMaxRepetitions(10);
            pdu.setNonRepeaters(0);
        }

        pdu.add(new VariableBinding(oid));

        return pdu;
    }

    private boolean walk(OID rootOID, List values) throws IOException {
        int requests = 0;
        int vars = 0;

        boolean isError = false;

        TreeUtils treeUtils = new TreeUtils(this.session, new DefaultPDUFactory());

        List events = treeUtils.getSubtree(this.target, rootOID);

        for (int i = 0; i < events.size(); i++) {
            TreeEvent e = (TreeEvent) events.get(i);

            requests++;

            if (e.isError()) {
                isError = true;

                log.debug(rootOID + " walk: " + e.getErrorMessage(), e.getException());
            }

            VariableBinding[] vb = e.getVariableBindings();

            if (vb != null) {
                vars += vb.length;

                for (int j = 0; j < vb.length; j++) {
                    values.add(new SNMPValue(vb[j]));
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(rootOID + " walk: " + requests + " requests, " + vars + " vars, avg=" + vars / requests);
        }

        return !isError;
    }

    private SNMPValue getValue(String name, int type) throws SNMPException {
        PDU request = getPDU(name, type);
        PDU response;

        ResponseEvent event = null;

        try {
            event = this.session.send(request, this.target);
        } catch (IOException e) {
            throw new SNMPException("Failed to get " + name, e);
        }

        if (event == null) {
            throw new SNMPException("No response for " + name);
        }

        response = event.getResponse();

        validateResponsePDU(name, response);

        VariableBinding var = response.get(0);

        if (var.isException()) {
            throw new MIBLookupException(name + ": " + var.getVariable().toString()); // e.g.
                                                                                      // noSuchObject
        }

        return new SNMPValue(var);
    }
    
    protected void validateResponsePDU(String name, PDU response)
        throws SNMPException {

        if (response == null) {
            throw new SNMPException("No response PDU for " + name);
        }
    }

    public SNMPValue getSingleValue(String name) throws SNMPException {
        return getValue(name, PDU.GET);
    }

    public SNMPValue getNextValue(String name) throws SNMPException {
        return getValue(name, PDU.GETNEXT);
    }

    public List getColumn(String name) throws SNMPException {
        List values = new ArrayList();

        try {
            if (!walk(getOID(name), values)) {
                throw new SNMPException("No response for " + name);
            }
        } catch (IOException e) {
            throw new SNMPException(e.getMessage(), e);
        }

        return values;
    }

    private StringBuffer getSubId(OID oid1, int oid1Len, OID oid2) {
        int oid2Len = oid2.getValue().length;

        StringBuffer sb = new StringBuffer();

        for (int x = oid1Len; x < oid2Len; x++) {
            sb.append(oid2.get(x));

            if (x < oid2Len - 1) {
                sb.append('.');
            }
        }

        return sb;
    }

    public Map getTable(String name, int index) throws SNMPException {
        OID oid = (OID) getOID(name).clone();

        oid.append(index);

        HashMap map = new HashMap();

        List column = getColumn(name);

        for (int i = 0; i < column.size(); i++) {
            SNMPValue value = (SNMPValue) column.get(i);

            StringBuffer sb = getSubId(oid, oid.getValue().length, value.oid);

            map.put(sb.toString(), new SNMPValue(value.oid, value.var));
        }

        return map;
    }

    public SNMPValue getTableValue(String name, int index, String leaf) throws SNMPException {
        OID oid = (OID) getOID(name).clone();

        oid.append(index);
        oid.append(leaf);

        PDU request = getPDU(oid, PDU.GET);

        PDU response;

        ResponseEvent event = null;

        try {
            event = this.session.send(request, this.target);
        } catch (IOException e) {
            throw new SNMPException("Failed to get " + name, e);
        }

        if (event == null) {
            throw new SNMPException("No response for " + name);
        }

        response = event.getResponse();

        if (response == null) {
            throw new SNMPException("No response for " + name);
        }

        return new SNMPValue(response.get(0));
    }

    public List getBulk(String name) throws SNMPException {
        return getColumn(name);
    }
}
