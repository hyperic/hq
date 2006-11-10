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

package org.hyperic.hq.measurement.data;

import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MetricValue;

public class MeasurementReport 
    implements java.io.Serializable 
{
    private String    agentToken;
    private DSNList[] clientIdList;
    private SRN[]     srnList;
    
    public MeasurementReport(){
        this.clientIdList = null;
    }
    
    public String getAgentToken() {
        return agentToken;
    }
    
    public void setAgentToken(String agentIpPort) {
        this.agentToken = agentIpPort;
    }
    
    public void setClientIdList(DSNList[] clientIds){
        this.clientIdList = clientIds;
    }

    public DSNList[] getClientIdList(){
        return this.clientIdList;
    }

    public void setSRNList(SRN[] srns){
        this.srnList = srns;
    }

    public SRN[] getSRNList(){
        return this.srnList;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer output = new StringBuffer("Measurement Report:\n");
        
        for (int i = 0; i < this.clientIdList.length; i++) {
            DSNList cid = this.clientIdList[i];
            
            ValueList[] dsns = cid.getDsns();
            for (int j = 0; j < dsns.length; j++) {
                MetricValue[] values = dsns[j].getValues();
                for (int k = 0; k < values.length; k++) {
                    output.append(
                        "Data point for CID=" + cid.getClientId() +
                        " DSN ID=" + dsns[j].getDsnId() + 
                        " Value=" + values[k].getValue() +
                        " tStamp=" + values[k].getTimestamp() + "\n");
                }
            }
        }
        
        return output.toString();
    }

}
