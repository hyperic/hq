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

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class SNMPValue {

    private static Log log = LogFactory.getLog(SNMPValue.class);
    
    public static final int TYPE_LONG = 0;
    public static final int TYPE_STRING = 1;
    public static final int TYPE_LONG_CONVERTABLE = 2;

    OID oid;
    Variable var;

    private SNMPValue() { }

    SNMPValue(VariableBinding vb) {
        this(vb.getOid(), vb.getVariable());
    }
    
    SNMPValue(OID oid, Variable var) {
        this.oid = oid;
        this.var = var;
    }

    private boolean isOctetString() {
        return
            this.var.getSyntax() ==
            SMIConstants.SYNTAX_OCTET_STRING;
    }

    public byte[] getBytes() {
        return ((OctetString)this.var).getValue();
    }

    public String toString() {
        if (isOctetString()) {
            //avoid OctetString.toString() hex encoding
            //if bytes contain any ISO control chars
            return new String(getBytes());
        }
        else {
            return this.var.toString();
        }
    }

    private String toHex(int val) {
        return Integer.toHexString(val & 0xff);
    }

    //from SNMPv2-TC:
    //PhysAddress ::= TEXTUAL-CONVENTION
    //DISPLAY-HINT "1x:"
    //STATUS       current
    //DESCRIPTION
    //        "Represents media- or physical-level addresses."
    //SYNTAX       OCTET STRING
    public String toPhysAddressString() {
        byte[] data = getBytes();

        if (data.length == 0) {
            return "0:0:0:0:0:0"; //e.g. loopback
        }

        StringBuffer buffer = new StringBuffer();
        
        buffer.append(toHex(data[0]));
                    
        for (int i=1; i<data.length; i++) {
            buffer.append(':').append(toHex(data[i]));
        }
        
        return buffer.toString();
    }

    public String getOID() {
        return this.oid.toString();
    }

    public int getType() {
        switch (this.var.getSyntax()) {
          case SMIConstants.SYNTAX_INTEGER32:
          case SMIConstants.SYNTAX_COUNTER32:
          case SMIConstants.SYNTAX_COUNTER64:
          case SMIConstants.SYNTAX_TIMETICKS:
          case SMIConstants.SYNTAX_GAUGE32:
            return TYPE_LONG;
          case SMIConstants.SYNTAX_OCTET_STRING:
            //XXX while we are able to convert long
            //does not mean we should. treat as a string
            //for now.
            //return TYPE_LONG_CONVERTABLE;
            return TYPE_STRING;
          default:
            return TYPE_STRING;
        }
    }

    // XXX A bit of a hack - if it is an OctetString, treat
    // it like a DateAndTime (from the SNMPv2-TC MIB)
    private long convertDateAndTimeToLong()
        throws SNMPException {

        byte[] bytes = getBytes();

        if (bytes.length < 8) {
            String msg =
                "OctetString is not in DateAndTime syntax";
            throw new SNMPException(msg);
        }

        Calendar cal = Calendar.getInstance();

        int ix = 0;
        int year = (bytes[ix] > 0) ?
            bytes[ix] : (256 + bytes[ix]);

        year <<= 8;
        ix++;
        year += (bytes[ix] > 0) ?
            bytes[ix] : (256 + bytes[ix]);

        ix++;

        int month = bytes[ix++];
        int day = bytes[ix++];
        int hour = bytes[ix++];
        int minutes = bytes[ix++];
        int seconds = bytes[ix++];
        int deciseconds = bytes[ix++];

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, (month-1));
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, (100*deciseconds));
        cal.set(Calendar.ZONE_OFFSET, 0);
        cal.set(Calendar.DST_OFFSET, 0);

        if (log.isDebugEnabled()) {
            log.debug("converted to DateAndTime: millis=" +
                      cal.getTimeInMillis() + ", date=" +
                      cal.getTime());
        }

        return cal.getTimeInMillis();
    }

    public long toLong() throws SNMPException {
        if (isOctetString()) {
            return convertDateAndTimeToLong();
        }
        else {
            try {
                return this.var.toLong();
            } catch (UnsupportedOperationException e) {
                String msg =
                    "Cannot convert " +
                    this.var.getSyntaxString() +
                    " to long";
                throw new SNMPException(msg);
            }
        }
    }
}
