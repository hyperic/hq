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

import org.hyperic.hq.appdef.ServerBase;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;

public class AIServer extends ServerBase
{
    private AIPlatform aIPlatform;
    private Character active;
    private String serverTypeName;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] responseTime_Config;
    private byte[] measurementConfig;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;

    /**
     * default constructor
     */
    public AIServer()
    {
        super();
    }

    public AIServer(AIServerValue sv)
    {
        super();
        setAIServerValue(sv);
    }

    public AIPlatform getAIPlatform()
    {
        return this.aIPlatform;
    }

    public void setAIPlatform(AIPlatform aIPlatform)
    {
        this.aIPlatform = aIPlatform;
    }

    public Character getActive()
    {
        return this.active;
    }

    public void setActive(Character active)
    {
        this.active = active;
    }

    public String getServerTypeName()
    {
        return this.serverTypeName;
    }

    public void setServerTypeName(String serverTypeName)
    {
        this.serverTypeName = serverTypeName;
    }

    public byte[] getCustomProperties()
    {
        return this.customProperties;
    }

    public void setCustomProperties(byte[] customProperties)
    {
        this.customProperties = customProperties;
    }

    public byte[] getProductConfig()
    {
        return this.productConfig;
    }

    public void setProductConfig(byte[] productConfig)
    {
        this.productConfig = productConfig;
    }

    public byte[] getControlConfig()
    {
        return this.controlConfig;
    }

    public void setControlConfig(byte[] controlConfig)
    {
        this.controlConfig = controlConfig;
    }

    public byte[] getResponseTimeConfig()
    {
        return this.responseTime_Config;
    }

    public void setResponseTimeConfig(byte[] responseTime_Config)
    {
        this.responseTime_Config = responseTime_Config;
    }

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
    }

    public int getQueueStatus()
    {
        return queueStatus != null ? queueStatus.intValue() : 0;
    }

    public void setQueueStatus(Integer queueStatus)
    {
        this.queueStatus = queueStatus;
    }

    /**
     * @deprecated use setQueueStatus(Integer)
     * @param queueStatus
     */
    public void setQueueStatus(int queueStatus)
    {
        setQueueStatus(new Integer(queueStatus));
    }

    public long getDiff()
    {
        return this.diff;
    }

    public void setDiff(long diff)
    {
        this.diff = diff;
    }

    public boolean isIgnored()
    {
        return this.ignored;
    }

    /**
     * @deprecated use isIgnored()
     * @return
     */
    public boolean getIgnored()
    {
        return isIgnored();
    }

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
    }

    private AIServerValue aIServerValue = new AIServerValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) AIServer object instead
     * @return
     */
    public AIServerValue getAIServerValue()
    {
        aIServerValue.setQueueStatus(getQueueStatus());
        aIServerValue.setCustomProperties(getCustomProperties());
        aIServerValue.setProductConfig(getProductConfig());
        aIServerValue.setControlConfig(getControlConfig());
        aIServerValue.setMeasurementConfig(getMeasurementConfig());
        aIServerValue.setResponseTimeConfig(getResponseTimeConfig());
        aIServerValue.setDiff(getDiff());
        aIServerValue.setIgnored(isIgnored());
        aIServerValue.setServerTypeName(
            (getServerTypeName() == null) ? "" : getServerTypeName());
        aIServerValue.setName(getName());
        aIServerValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        aIServerValue.setInstallPath(getInstallPath());
        aIServerValue.setDescription(getDescription());
        aIServerValue.setServicesAutomanaged(isServicesAutomanaged());
        aIServerValue.setId(getId());
        aIServerValue.setMTime(getMTime());
        aIServerValue.setCTime(getCTime());
        return aIServerValue;
    }

    public void setAIServerValue(AIServerValue valueHolder)
    {
        setQueueStatus( valueHolder.getQueueStatus() );
        setCustomProperties( valueHolder.getCustomProperties() );
        setProductConfig( valueHolder.getProductConfig() );
        setControlConfig( valueHolder.getControlConfig() );
        setMeasurementConfig( valueHolder.getMeasurementConfig() );
        setResponseTimeConfig( valueHolder.getResponseTimeConfig() );
        setDiff( valueHolder.getDiff() );
        setIgnored( valueHolder.getIgnored() );
        setServerTypeName( valueHolder.getServerTypeName() );
        setName( valueHolder.getName() );
        setAutoinventoryIdentifier( valueHolder.getAutoinventoryIdentifier() );
        setInstallPath( valueHolder.getInstallPath() );
        setDescription( valueHolder.getDescription() );
        setServicesAutomanaged( valueHolder.getServicesAutomanaged() );
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AIServer) || !super.equals(obj)) {
            return false;
        }
        AIServer o = (AIServer)obj;
        return
            ((autoinventoryIdentifier == o.getAutoinventoryIdentifier()) ||
             (autoinventoryIdentifier!=null && o.getAutoinventoryIdentifier()!=null &&
              autoinventoryIdentifier.equals(o.getAutoinventoryIdentifier())))
            &&
            ((aIPlatform == o.getAIPlatform()) ||
             (aIPlatform!=null && o.getAIPlatform()!=null &&
              aIPlatform.equals(o.getAIPlatform())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result +(autoinventoryIdentifier != null ?
                             autoinventoryIdentifier.hashCode() : 0);
        result = 37*result + (aIPlatform != null ? aIPlatform.hashCode() : 0);

        return result;
    }

    /**
     * For compatibility
     */
    public AppdefResourceType getAppdefResourceType() {
        return null;
    }

    /**
     * For compatibility
     */
   public AppdefResourceValue getAppdefResourceValue() {
        return null;
    }
   
   protected String _getAuthzOp(String op) {
       throw new IllegalArgumentException("No supported operations");
   }
   
}

