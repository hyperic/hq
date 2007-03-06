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

package org.hyperic.hq.autoinventory;

import org.hyperic.hq.appdef.IpBase;
import org.hyperic.hq.appdef.shared.AIIpValue;

public class AIIp extends IpBase
{
    private AIPlatform _aIPlatform;
    private Integer _queueStatus;
    private long _diff;
    private boolean _ignored;

    public AIIp() {
        super();
    }

    public AIIp(AIIpValue ipv) {
        super();
        setAIIpValue(ipv);
    }

    public AIPlatform getAIPlatform() {
        return _aIPlatform;
    }

    public void setAIPlatform(AIPlatform aIPlatform) {
        _aIPlatform = aIPlatform;
    }

    public int getQueueStatus() {
        return _queueStatus != null ? _queueStatus.intValue() : 0;
    }

    public void setQueueStatus(Integer queueStatus) {
        _queueStatus = queueStatus;
    }

    /**
     * @deprecated use setQueueStatus(Integer)
     */
    public void setQueueStatus(int queueStatus) {
        setQueueStatus(new Integer(queueStatus));
    }

    public long getDiff() {
        return _diff;
    }

    public void setDiff(long diff) {
        _diff = diff;
    }

    public boolean isIgnored() {
        return _ignored;
    }

    /**
     * @deprecated use isIgnored()
     */
    public boolean getIgnored() {
        return isIgnored();
    }

    public void setIgnored(boolean ignored) {
        _ignored = ignored;
    }

    private AIIpValue aIIpValue = new AIIpValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) AIIp object instead
     */
    public AIIpValue getAIIpValue()
    {
        aIIpValue.setQueueStatus(getQueueStatus());
        aIIpValue.setDiff(getDiff());
        aIIpValue.setIgnored(getIgnored());
        aIIpValue.setAddress(getAddress());
        aIIpValue.setMACAddress(getMacAddress());
        aIIpValue.setNetmask(getNetmask());
        aIIpValue.setId(getId());
        aIIpValue.setMTime(getMTime());
        aIIpValue.setCTime(getCTime());
        return aIIpValue;
    }

    /**
     * @deprecated
     */
    public void setAIIpValue(AIIpValue valueHolder) {
        setQueueStatus(valueHolder.getQueueStatus() );
        setDiff(valueHolder.getDiff());
        setIgnored(valueHolder.getIgnored());
        setAddress(valueHolder.getAddress());
        setMacAddress(valueHolder.getMACAddress());
        setNetmask(valueHolder.getNetmask());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AIIp) || !super.equals(obj)) {
            return false;
        }
        AIIp o = (AIIp) obj;
        return ((_aIPlatform == o.getAIPlatform()) ||
                (_aIPlatform != null && o.getAIPlatform() != null &&
                 _aIPlatform.equals(o.getAIPlatform())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (_aIPlatform != null ? _aIPlatform.hashCode() : 0);

        return result;
    }
}
