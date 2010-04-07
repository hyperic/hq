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

import java.util.List;
import java.util.Map;

import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;

class SNMPSession_v2c
    extends SNMPSession_v1
{
    SNMPSession_v2c() {
        this.version = SnmpConstants.version2c;
    }

    public List getBulk(String name) throws SNMPException {
        // Optimize using snmp4j v2 specific stuff...
        return super.getBulk(name);
    }

    public Map getTable(String name, int index) throws SNMPException {
        // Optimize using snmp4j v2 specific stuff...
        return super.getTable(name, index);
    }
    
    protected void validateResponsePDU(String name, PDU response)
        throws SNMPException {

        // No specific SNMPv2c logic currently
        super.validateResponsePDU(name, response);
    }
}
