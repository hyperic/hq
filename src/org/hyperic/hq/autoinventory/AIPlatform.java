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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.PlatformBase;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.product.PlatformDetector;

public class AIPlatform extends PlatformBase
{
    private String platformTypeName;
    private String osversion;
    private String arch;
    private String agentToken;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;
    private Long lastApproved;
    private Integer cpuSpeed;
    private Integer ram;
    private String gateway;
    private String dhcpServer;
    private String dnsServer;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] measurementConfig;
    private Collection aiips = new ArrayList();
    private Collection aiservers =  new ArrayList();

    public AIPlatform()
    {
        super();
    }

    public AIPlatform(AIPlatformValue apv)
    {
        super();
        setFqdn(apv.getFqdn());
        setCertdn(apv.getCertdn());
        setQueueStatus(apv.getQueueStatus());
        setDescription(apv.getDescription());
        setDiff(apv.getDiff());
        setPlatformTypeName(apv.getPlatformTypeName());
        setLastApproved(new Long(0));
        setIgnored(false);
        setName(apv.getName());
        setAgentToken (apv.getAgentToken());
        setCpuCount   (apv.getCpuCount());
        setCustomProperties(apv.getCustomProperties());
        setProductConfig(apv.getProductConfig());
        setMeasurementConfig(apv.getMeasurementConfig());
        setControlConfig(apv.getControlConfig());
    }

    public String getPlatformTypeName()
    {
        return this.platformTypeName;
    }

    public void setPlatformTypeName(String platformTypeName)
    {
        this.platformTypeName = platformTypeName;
    }

    public String getOsversion()
    {
        return this.osversion;
    }

    public void setOsversion(String osversion)
    {
        this.osversion = osversion;
    }

    public String getArch()
    {
        return this.arch;
    }

    public void setArch(String arch)
    {
        this.arch = arch;
    }

    public String getAgentToken()
    {
        return this.agentToken;
    }

    public void setAgentToken(String agentToken)
    {
        this.agentToken = agentToken;
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
     * @depreated use setQueueStatus(Integer)
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

    public void setDiff(Long diff)
    {
        this.diff = diff != null ? diff.longValue() : 0L;
    }

    public boolean isIgnored()
    {
        return this.ignored;
    }

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
    }

    public long getLastApproved()
    {
        return lastApproved != null ? lastApproved.longValue() : 0L;
    }

    public void setLastApproved(Long lastApproved)
    {
        this.lastApproved = lastApproved;
    }

    public Integer getCpuSpeed()
    {
        return this.cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed)
    {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getRam()
    {
        return this.ram;
    }

    public void setRam(Integer ram)
    {
        this.ram = ram;
    }

    public String getGateway()
    {
        return this.gateway;
    }

    public void setGateway(String gateway)
    {
        this.gateway = gateway;
    }

    public String getDhcpServer()
    {
        return this.dhcpServer;
    }

    public void setDhcpServer(String dhcpServer)
    {
        this.dhcpServer = dhcpServer;
    }

    public String getDnsServer()
    {
        return this.dnsServer;
    }

    public void setDnsServer(String dnsServer)
    {
        this.dnsServer = dnsServer;
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

    public Collection getAIIps()
    {
        return this.aiips;
    }

    public void setAIIps(Collection aiips)
    {
        this.aiips = aiips;
    }

    public Collection getAIServers()
    {
        return this.aiservers;
    }

    public void setAIServers(Collection aiservers)
    {
        this.aiservers = aiservers;
    }

    public void addAIServer(AIServer server)
    {
        if (server != null) {
            aiservers.add(server);
            server.setAIPlatform(this);
        }
    }

    public boolean isPlatformDevice() {
        return !PlatformDetector.isSupportedPlatform(getPlatformTypeName());
    }

    private AIPlatformValue aipValue = new AIPlatformValue();
    /**
     * @deprecated use (this) AIPlatformValue object
     * @return
     */
    public AIPlatformValue getAIPlatformValue()
    {
        aipValue.setAgentToken(
            (getAgentToken() == null) ? "" : getAgentToken());
        aipValue.setQueueStatus(getQueueStatus());
        aipValue.setCustomProperties(getCustomProperties());
        aipValue.setProductConfig(getProductConfig());
        aipValue.setControlConfig(getControlConfig());
        aipValue.setMeasurementConfig(getMeasurementConfig());
        aipValue.setDiff(getDiff());
        aipValue.setIgnored(isIgnored());
        aipValue.setPlatformTypeName(
            (getPlatformTypeName() == null) ? "" : getPlatformTypeName());
        aipValue.setLastApproved(new Long(getLastApproved()));
        aipValue.setCertdn(getCertdn());
        aipValue.setFqdn(getFqdn());
        aipValue.setName(getName());
        aipValue.setLocation(getLocation());
        aipValue.setDescription(getDescription());
        aipValue.setCpuCount(getCpuCount());
        aipValue.setId(getId());
        aipValue.setMTime(getMTime());
        aipValue.setCTime(getCTime());
        aipValue.removeAllAIIpValues();
        Iterator iAIIpValue = getAIIps().iterator();
        while (iAIIpValue.hasNext()){
            aipValue.addAIIpValue(
                ((AIIp)iAIIpValue.next()).getAIIpValue() );
        }
        aipValue.cleanAIIpValue();
        aipValue.removeAllAIServerValues();
        Iterator iAIServerValue = getAIServers().iterator();
        while (iAIServerValue.hasNext()){
            aipValue.addAIServerValue(
                ((AIServer)iAIServerValue.next()).getAIServerValue() );
        }
        aipValue.cleanAIServerValue();
        return aipValue;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof AIPlatform) && super.equals(obj);
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