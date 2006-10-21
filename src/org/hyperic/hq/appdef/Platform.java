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

package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Pojo for hibernate hbm mapping file
 */
public class Platform extends AppdefResource
{
    private String fqdn;
    private String certdn;
    private String commentText;
    private Integer cpuCount;
    private PlatformType platformType;
    private ConfigResponseDB configResponse;
    private Agent agent;
    private Collection ips;
    private Collection servers;

    /**
     * default constructor
     */
    public Platform()
    {
        super();
    }

    public String getFqdn()
    {
        return this.fqdn;
    }

    public void setFqdn(String fqDN)
    {
        this.fqdn = fqDN;
    }

    public String getCertdn()
    {
        return this.certdn;
    }

    public void setCertdn(String certDN)
    {
        this.certdn = certDN;
    }

    public String getCommentText()
    {
        return this.commentText;
    }

    public void setCommentText(String comment)
    {
        this.commentText = comment;
    }

    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public PlatformType getPlatformType()
    {
        return this.platformType;
    }

    public void setPlatformType(PlatformType platformType)
    {
        this.platformType = platformType;
    }

    public ConfigResponseDB getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse)
    {
        this.configResponse = configResponse;
    }

    public Agent getAgent()
    {
        return this.agent;
    }

    public void setAgent(Agent agent)
    {
        this.agent = agent;
    }

    public Collection getIps()
    {
        return this.ips;
    }

    public void setIps(Collection ips)
    {
        this.ips = ips;
    }

    public Collection getServers()
    {
        return this.servers;
    }

    public void setServers(Collection servers)
    {
        this.servers = servers;
    }

    private PlatformPK pkey = new PlatformPK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public PlatformPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    public Server createServer(ServerValue sv)
    {
        throw new UnsupportedOperationException(
            "use ServerDAO.createServer()");
    }

    public void updatePlatform(PlatformValue existing)
    {
        throw new UnsupportedOperationException(
            "use PlatformDAO.updatePlatform()");
    }

    /**
     * Update an existing appdef platform with data from an AI platform.
     * @param aiplatform the AI platform object to use for data
     */
    public void updateWithAI(AIPlatformValue aiplatform, String owner)
    {
        setFqdn(aiplatform.getFqdn());
        setCertdn(aiplatform.getCertdn());
        if (aiplatform.getName() != null) {
            setName(aiplatform.getName());
        }
        setModifiedBy(owner);
        // setLocation("");
        setOwner(owner);
        setCpuCount(aiplatform.getCpuCount());
        setDescription(aiplatform.getDescription());
    }

    /**
     * legacy EJB accessor method
     * @deprecated use getConfigResponse()
     * @return
     */
    public Integer getConfigResponseId()
    {
        return configResponse != null ?configResponse.getId() : null;
    }
    /**
     * legacy EJB accessor method
     * @deprecated use setConfigResponse()
     * @return
     */
    public void setConfigResponseId(Integer crif)
    {
        ConfigResponseDB c = new ConfigResponseDB();
        c.setId(crif);
        setConfigResponse(c);
    }

    /**
     * Compare this entity to a value object
     * (legacy code from entity bean)
     * @return true if this platform is the same as the one in the val obj
     */
    public boolean matchesValueObject(PlatformValue obj)
    {
        boolean matches = true;

        matches = super.matchesValueObject(obj) &&
            (this.getName() != null ? this.getName().equals(obj.getName())
                : (obj.getName() == null)) &&
            (this.getDescription() != null ?
                this.getDescription().equals(obj.getDescription())
                : (obj.getDescription() == null)) &&
            (this.getCertdn() != null ? this.getCertdn().equals(obj.getCertdn())
                : (obj.getCertdn() == null)) &&
            (this.getCommentText() != null ?
                this.getCommentText().equals(obj.getCommentText())
                : (obj.getCommentText() == null)) &&
            (this.getCpuCount() != null ?
                this.getCpuCount().equals(obj.getCpuCount())
                : (obj.getCpuCount() == null)) &&
            (this.getFqdn() != null ? this.getFqdn().equals(obj.getFqdn())
                : (obj.getFqdn() == null)) &&
            (this.getLocation() != null ?
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) &&
            (this.getOwner() != null ? this.getOwner().equals(obj.getOwner())
                : (obj.getOwner() == null)) &&
        // now for the IP's
        // if there's any in the addedIp's collection, it was messed with
        // which means the match fails
            (obj.getAddedIpValues().size() == 0) &&
            (obj.getRemovedIpValues().size() == 0) &&
        // check to see if we have changed the agent
            (this.getAgent() != null ? this.getAgent().equals(obj.getAgent())
                : (obj.getAgent() == null));
        return matches;
    }

    /**
     * Get a snapshot of the ServerLocals associated with this platform
     * @deprecated use getServers()
     */
    public Set getServerSnapshot()
    {
        Collection servers = getServers();
        if(servers == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(servers);
    }

    /**
     * legacy EJB DTO patter
     * @deprecated use (this) Platform object instead
     * @return
     */
    public PlatformValue getPlatformValueObject()
    {
        PlatformValue pv = new PlatformValue();
        pv.setSortName(getSortName());
        pv.setDescription(getDescription());
        pv.setCommentText(getCommentText());
        pv.setModifiedBy(getModifiedBy());
        pv.setOwner(getOwner());
        pv.setCertdn(getCertdn());
        pv.setFqdn(getFqdn());
        pv.setName(getName());
        pv.setLocation(getLocation());
        pv.setCpuCount(getCpuCount());
        pv.setId(this.getId());
        pv.setMTime(getMTime());
        pv.setCTime(getCTime());
        pv.setConfigResponseId(getConfigResponseId());
        PlatformType ptype = getPlatformType();
        if ( ptype != null )
            pv.setPlatformType(ptype.getPlatformTypeValue());
        else
           pv.setPlatformType( null );
        Agent agent = getAgent();
        if ( agent != null )
            pv.setAgent( agent.getAgentValue() );
        else
            pv.setAgent( null );
        return pv;
    }

    private PlatformValue platformValue = new PlatformValue();
    /**
     * legacy EJB DTO patter
     * @deprecated use (this) Platform object instead
     * @return
     */
    public PlatformValue getPlatformValue()
    {
        platformValue.setSortName(getSortName());
        platformValue.setCommentText(getCommentText());
        platformValue.setModifiedBy(getModifiedBy());
        platformValue.setOwner(getOwner());
        platformValue.setConfigResponseId(getConfigResponseId());
        platformValue.setCertdn(getCertdn());
        platformValue.setFqdn(getFqdn());
        platformValue.setName(getName());
        platformValue.setLocation(getLocation());
        platformValue.setDescription(getDescription());
        platformValue.setCpuCount(getCpuCount());
        platformValue.setId(getId());
        platformValue.setMTime(getMTime());
        platformValue.setCTime(getCTime());
        platformValue.removeAllIpValues();
        Collection ips = getIps();
        if (ips != null) {
            Iterator iIpValue = ips.iterator();
            while (iIpValue.hasNext()){
                platformValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
            }
        }
        platformValue.cleanIpValue();
        platformValue.removeAllServerValues();
        Collection servers = getServers();
        if (servers != null) {
            Iterator iServerValue = servers.iterator();
            while (iServerValue.hasNext()){
                platformValue.addServerValue(
                    ((Server)iServerValue.next()).getServerLightValue());
            }
        }
        platformValue.cleanServerValue();
        if ( getPlatformType() != null )
            platformValue.setPlatformType(
                getPlatformType().getPlatformTypeValue());
        else
            platformValue.setPlatformType( null );
        if ( getAgent() != null ) {
            platformValue.setAgent( getAgent().getAgentValue() );
        }
        else
            platformValue.setAgent( null );
        return platformValue;
    }


    private PlatformLightValue platformLightValue = new PlatformLightValue();
    /**
     * legacy EJB DTO patter
     * @deprecated use (this) Platform object instead
     * @return
     */
    public PlatformLightValue getPlatformLightValue()
    {
        platformLightValue.setSortName(getSortName());
        platformLightValue.setCommentText(getCommentText());
        platformLightValue.setModifiedBy(getModifiedBy());
        platformLightValue.setOwner(getOwner());
        platformLightValue.setConfigResponseId(getConfigResponseId());
        platformLightValue.setCertdn(getCertdn());
        platformLightValue.setFqdn(getFqdn());
        platformLightValue.setName(getName());
        platformLightValue.setLocation(getLocation());
        platformLightValue.setDescription(getDescription());
        platformLightValue.setCpuCount(getCpuCount());
        platformLightValue.setId(getId());
        platformLightValue.setMTime(getMTime());
        platformLightValue.setCTime(getCTime());
        platformLightValue.removeAllIpValues();
        Collection ips = getIps();
        if (ips != null) {
            Iterator iIpValue = ips.iterator();
            while (iIpValue.hasNext()){
                platformLightValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
            }
        }
        platformLightValue.cleanIpValue();
        if ( getPlatformType() != null )
            platformLightValue.setPlatformType(
                getPlatformType().getPlatformTypeValue() );
        else
            platformLightValue.setPlatformType( null );
        return platformLightValue;
    }

    /**
     * convenience method for copying simple values from
     * legacy Platform Value Object.
     * @param pv
     */
    public void setPlatformValue(PlatformValue pv)
    {
        setDescription(pv.getDescription());
        setCommentText( pv.getCommentText() );
        setModifiedBy( pv.getModifiedBy() );
        setOwner( pv.getOwner() );
        setLocation( pv.getLocation() );
        setCpuCount( pv.getCpuCount() );
        setCertdn( pv.getCertdn() );
        setFqdn( pv.getFqdn() );
        setName( pv.getName() );
    }

    /**
     * Get a snapshot of the IPLocals associated with this platform
     * @deprecated use getIps()
     */
    public Set getIpSnapshot()
    {
        if (getIps() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getIps());
    }

    public boolean equals(Object obj)
    {
        if (!super.equals(obj) || !(obj instanceof Platform)) {
            return false;
        }
        return true;
    }

}
