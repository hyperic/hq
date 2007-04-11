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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public class Platform extends PlatformBase
{
    private String _commentText;
    private PlatformType _platformType;
    private ConfigResponseDB _configResponse;
    private Agent _agent;
    private Collection _ips = new ArrayList();
    private Collection _servers =  new ArrayList();

    public Platform() {
        super();
    }

    public String getCommentText() {
        return _commentText;
    }

    void setCommentText(String comment) {
        _commentText = comment;
    }

    public PlatformType getPlatformType() {
        return _platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        _platformType = platformType;
    }

    public ConfigResponseDB getConfigResponse() {
        return _configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse) {
        _configResponse = configResponse;
    }

    public Agent getAgent() {
        return _agent;
    }

    public void setAgent(Agent agent) {
        _agent = agent;
    }

    public Collection getIps() {
        return _ips;
    }

    public void setIps(Collection ips) {
        _ips = ips;
    }

    public Ip addIp(String address, String netmask, String macAddress) {
        Ip ip = new Ip(address, netmask, macAddress);
        _ips.add(ip);
        ip.setPlatform(this);
        return ip;
    }

    public Ip removeIp(String address, String netmask, String macAddress) {
        for (Iterator i = _ips.iterator(); i.hasNext(); ) {
            Ip ip = (Ip)i.next();
            if (ip.getAddress().equals(address) &&
                ip.getNetmask().equals(netmask) &&
                ip.getMacAddress().equals(macAddress)) {
                i.remove();
                return ip;
            }
        }
        return null;
    }

    
    public Ip updateIp(String address, String netmask, String macAddress) {
        for (Iterator i = _ips.iterator(); i.hasNext(); ) {
            Ip ip = (Ip)i.next();
            if (ip.getAddress().equals(address)) {
                ip.setNetmask(netmask);
                ip.setMacAddress(macAddress);
                return ip;
            }
        }
        return null;
    }
    
    public Collection getServers() {
        return _servers;
    }

    public void setServers(Collection servers) {
        _servers = servers;
    }

    /**
     * Update an existing appdef platform with data from an AI platform.
     * @param aiplatform the AI platform object to use for data
     */
    public void updateWithAI(AIPlatformValue aiplatform, String owner) {
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
     */
    public Integer getConfigResponseId() {
        return _configResponse != null ? _configResponse.getId() : null;
    }

    /**
     * Compare this entity to a value object
     * (legacy code from entity bean)
     * @return true if this platform is the same as the one in the val obj
     */
    public boolean matchesValueObject(PlatformValue obj) {
        boolean matches;

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
    public Set getServerSnapshot() {
        return new LinkedHashSet(_servers);
    }

    private PlatformValue _platformValue = new PlatformValue();

    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) Platform object instead
     */
    public PlatformValue getPlatformValue()
    {
        _platformValue.setSortName(getSortName());
        _platformValue.setCommentText(getCommentText());
        _platformValue.setModifiedBy(getModifiedBy());
        _platformValue.setOwner(getOwner());
        _platformValue.setConfigResponseId(getConfigResponseId());
        _platformValue.setCertdn(getCertdn());
        _platformValue.setFqdn(getFqdn());
        _platformValue.setName(getName());
        _platformValue.setLocation(getLocation());
        _platformValue.setDescription(getDescription());
        _platformValue.setCpuCount(getCpuCount());
        _platformValue.setId(getId());
        _platformValue.setMTime(getMTime());
        _platformValue.setCTime(getCTime());
        _platformValue.removeAllIpValues();
        Iterator iIpValue = getIps().iterator();
        while (iIpValue.hasNext()){
            _platformValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
        }
        _platformValue.cleanIpValue();
        if (getPlatformType() != null)
            _platformValue.setPlatformType(
                getPlatformType().getPlatformTypeValue());
        else
            _platformValue.setPlatformType( null );
        if (getAgent() != null) {
            _platformValue.setAgent(getAgent().getAgentValue());
        }
        else
            _platformValue.setAgent(null);
        return _platformValue;
    }


    private PlatformLightValue _platformLightValue = new PlatformLightValue();
    /**
     * legacy EJB DTO patter
     * @deprecated use (this) Platform object instead
     */
    public PlatformLightValue getPlatformLightValue()
    {
        _platformLightValue.setSortName(getSortName());
        _platformLightValue.setCommentText(getCommentText());
        _platformLightValue.setModifiedBy(getModifiedBy());
        _platformLightValue.setOwner(getOwner());
        _platformLightValue.setConfigResponseId(getConfigResponseId());
        _platformLightValue.setCertdn(getCertdn());
        _platformLightValue.setFqdn(getFqdn());
        _platformLightValue.setName(getName());
        _platformLightValue.setLocation(getLocation());
        _platformLightValue.setDescription(getDescription());
        _platformLightValue.setCpuCount(getCpuCount());
        _platformLightValue.setId(getId());
        _platformLightValue.setMTime(getMTime());
        _platformLightValue.setCTime(getCTime());
        _platformLightValue.removeAllIpValues();
        Iterator iIpValue = getIps().iterator();
        while (iIpValue.hasNext()){
            _platformLightValue.addIpValue(((Ip)iIpValue.next()).getIpValue());
        }
        _platformLightValue.cleanIpValue();
        if ( getPlatformType() != null )
            _platformLightValue.setPlatformType(
                getPlatformType().getPlatformTypeValue() );
        else
            _platformLightValue.setPlatformType( null );
        return _platformLightValue;
    }

    /**
     * convenience method for copying simple values from
     * legacy Platform Value Object.
     */
    public void setPlatformValue(PlatformValue pv) {
        setDescription(pv.getDescription());
        setCommentText(pv.getCommentText());
        setModifiedBy(pv.getModifiedBy());
        setOwner(pv.getOwner());
        setLocation(pv.getLocation());
        setCpuCount(pv.getCpuCount());
        setCertdn(pv.getCertdn());
        setFqdn(pv.getFqdn());
        setName(pv.getName());
    }

    /**
     * Get a snapshot of the IPLocals associated with this platform
     * @deprecated use getIps()
     */
    public Set getIpSnapshot() {
        return new LinkedHashSet(getIps());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Platform) && super.equals(obj);
    }

    public AppdefResourceType getAppdefResourceType() {
        return _platformType;
    }
}
