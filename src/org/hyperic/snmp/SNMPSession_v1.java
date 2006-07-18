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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

class SNMPSession_v1 implements SNMPSession {

    protected int version;
    protected CommunityTarget target;
    protected Snmp session;
    private Address address;
    private static Snmp sessionInstance = null;
    protected static Log log = LogFactory.getLog("SNMPSession");

    private Snmp getSessionInstance()
        throws IOException {

        if (sessionInstance == null) {
            sessionInstance = new Snmp(new DefaultUdpTransportMapping());
            sessionInstance.listen();
        }

        return sessionInstance;
    }
    
    SNMPSession_v1() {
        this.version = SnmpConstants.version1;
    }

    void init(String address,
              String port,
              String community)
        throws SNMPException {

        if (address == null) {
            address = SNMPClient.DEFAULT_IP;
        }

        if (port == null) {
            port = SNMPClient.DEFAULT_PORT_STRING;
        }

        if (community == null) {
            community = SNMPClient.DEFAULT_COMMUNITY;
        }
        
        this.address =
            GenericAddress.parse("udp:" + address + "/" + port);

        this.target =
            new CommunityTarget();

        this.target.setAddress(this.address);
        this.target.setCommunity(new OctetString(community));
        this.target.setVersion(this.version);
        this.target.setRetries(1);
        this.target.setTimeout(500);

        try {
            this.session = getSessionInstance();
        } catch (IOException e) {
            throw new SNMPException(e.getMessage(), e);
        }
    }

    protected static OID getOID(String name)
        throws MIBLookupException {

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
    
    protected PDU getPDU(String oid, int type)
        throws MIBLookupException {

        PDU pdu = new PDU();
        pdu.setType(type);

        if (type == PDU.GETBULK) {
            pdu.setMaxRepetitions(16);
            pdu.setNonRepeaters(0);
        }

        pdu.add(new VariableBinding(getOID(oid)));

        return pdu;
    }

    private PDU walk(PDU request, List values) throws IOException {
        OID rootOID = request.get(0).getOid();
        PDU response = null;
        int requests = 0;
        boolean isDebug = log.isDebugEnabled();

        if (isDebug) {
            log.debug("Walking: " + rootOID);
        }
        
        do {
            ResponseEvent responseEvent =
                this.session.send(request, target);
            if (responseEvent == null) {
                return null;
            }
            requests++;
            response = responseEvent.getResponse();
            if (response == null) {
                return null;
            }
            if (isDebug) {
                log.debug("request# " + requests + ", #vars " +
                          response.size());
            }
        } while (walk(response, request, rootOID, values));

        return response;
    }

    private boolean walk(PDU response, PDU request,
                         OID rootOID, List values) {

        if ((response == null) || (response.getErrorStatus() != 0)) {
            return false;
        }

        boolean finished = false;
        OID lastOID = request.get(0).getOid();
        int size = response.size();

        for (int i=0; (!finished) && (i<size); i++) {
            VariableBinding vb = response.get(i);

            if ((vb.getOid() == null) ||
                (vb.getOid().size() < rootOID.size()) ||
                (rootOID.leftMostCompare(rootOID.size(), vb.getOid()) != 0))
            {
                finished = true;
            }
            else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
                finished = true;
            }
            else if (vb.getOid().compareTo(lastOID) <= 0) {
                finished = true;
            }
            else {
                values.add(new SNMPValue(vb));
                lastOID = vb.getOid();
            }
        }
        if (size == 0) {
            finished = true;
        }
        if (!finished) {
            VariableBinding next = response.get(size-1);
            next.setVariable(new Null());
            request.set(0, next);
            request.setRequestID(new Integer32(0));
        }
        return !finished;
    }

    private SNMPValue getValue(String name, int type)
        throws SNMPException {

        PDU request = getPDU(name, type);
        PDU response;
        ResponseEvent event = null;
        try {
            event =
                this.session.send(request, this.target);
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
    
    public SNMPValue getSingleValue(String name)
        throws SNMPException {

        return getValue(name, PDU.GET);
    }

    public SNMPValue getNextValue(String name)
        throws SNMPException {

        return getValue(name, PDU.GETNEXT);
    }

    public List getColumn(String name)
        throws SNMPException {

        List values = new ArrayList();
        
        try {
            PDU response =
                walk(getPDU(name, PDU.GETBULK), values);
            if (response == null) {
                throw new SNMPException("No response for " + name);
            }
        } catch (IOException e) {
            throw new SNMPException(e.getMessage(), e);
        }
        
        return values;
    }

    private StringBuffer getSubId(OID oid1,
                                  int oid1Len,
                                  OID oid2) {
        int oid2Len = oid2.getValue().length;

        StringBuffer sb = new StringBuffer();

        for (int x=oid1Len; 
             x<oid2Len;
             x++)
        {
            sb.append(oid2.get(x));
            if (x<oid2Len-1) {
                sb.append('.');
            }
        }

        return sb;
    }

    public Map getTable(String name, int index)
        throws SNMPException {

        OID oid = (OID)SNMPClient.getMibOID(name).clone();
        oid.append(index);

        HashMap map = new HashMap();
        List column = getColumn(name);

        for (int i=0; i<column.size(); i++) {
            SNMPValue value = (SNMPValue)column.get(i);
            StringBuffer sb =
                getSubId(oid, oid.getValue().length, value.oid);
            map.put(sb.toString(),
                    new SNMPValue(value.oid, value.var));
        }

        return map;
    }

    public List getBulk(String name)
        throws SNMPException {

        return getColumn(name);
    }
}
