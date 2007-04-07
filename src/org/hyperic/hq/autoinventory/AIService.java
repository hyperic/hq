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

import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

public class AIService extends AppdefResource
{
    private String serviceTypeName;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] measurementConfig;
    private byte[] responseTimeConfig;
    private Server server;

    /**
     * default constructor
     */
    public AIService()
    {
        super();
    }

    public AIService(AIServiceValue sv)
    {
        super();
        setAIServiceValue(sv);
    }

    public AppdefEntityID getEntityId()
    {
        return new AppdefEntityID(
            AppdefEntityConstants.APPDEF_TYPE_SERVICE,
            getId().intValue());
    }

    public String getServiceTypeName()
    {
        return this.serviceTypeName;
    }

    public void setServiceTypeName(String serviceTypeName)
    {
        this.serviceTypeName = serviceTypeName;
    }

    public Integer getQueueStatus()
    {
        return this.queueStatus;
    }

    public void setQueueStatus(Integer queueStatus)
    {
        this.queueStatus = queueStatus;
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

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
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

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
    }

    public byte[] getResponseTimeConfig()
    {
        return this.responseTimeConfig;
    }

    public void setResponseTimeConfig(byte[] responseTimeConfig)
    {
        this.responseTimeConfig = responseTimeConfig;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    private AIServiceValue aIServiceValue = new AIServiceValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) AIService object instead
     * @return
     */
    public AIServiceValue getAIServiceValue()
    {
        aIServiceValue.setServerId(getServerId());
        aIServiceValue.setServiceTypeName(
            (getServiceTypeName() == null) ? "" : getServiceTypeName());
        aIServiceValue.setCustomProperties(getCustomProperties());
        aIServiceValue.setProductConfig(getProductConfig());
        aIServiceValue.setControlConfig(getControlConfig());
        aIServiceValue.setMeasurementConfig(getMeasurementConfig());
        aIServiceValue.setResponseTimeConfig(getResponseTimeConfig());
        aIServiceValue.setName(getName());
        aIServiceValue.setDescription(getDescription());
        aIServiceValue.setId(getId());
        aIServiceValue.setMTime(getMTime());
        aIServiceValue.setCTime(getCTime());
        return aIServiceValue;
    }

    public void setAIServiceValue(AIServiceValue valueHolder)
    {
        setServiceTypeName( valueHolder.getServiceTypeName() );
        setCustomProperties( valueHolder.getCustomProperties() );
        setProductConfig( valueHolder.getProductConfig() );
        setControlConfig( valueHolder.getControlConfig() );
        setMeasurementConfig( valueHolder.getMeasurementConfig() );
        setResponseTimeConfig( valueHolder.getResponseTimeConfig() );
        setName( valueHolder.getName() );
        setDescription( valueHolder.getDescription() );
    }

    public int getServerId()
    {
        return
            getServer() != null && getServer().getId() != null ?
            getServer().getId().intValue() : 0;
    }

    public void setServerId(int server)
    {
        setServer(new Server(new Integer(server)));
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AIService) || !super.equals(obj)) {
            return false;
        }
        AIService o = (AIService)obj;
        return
            ((server == o.getServer()) ||
             (server!=null && o.getServer()!=null &&
              server.equals(o.getServer())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (server != null ? server.hashCode() : 0);

        return result;
    }

    public AppdefResourceType getAppdefResourceType() {
        return null;
    }
}
