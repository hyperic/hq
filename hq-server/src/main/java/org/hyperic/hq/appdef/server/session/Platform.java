/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2009], Hyperic, Inc. 
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.ObjectUtils;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.HasAuthzOperations;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class Platform extends PlatformBase
    implements HasAuthzOperations
{
    // Map 'simple' names onto Authz operations
    private static final Map _authOps;
    static {
        _authOps = new HashMap();
        _authOps.put("create",        AuthzConstants.platformOpCreatePlatform);
        _authOps.put("modify",        AuthzConstants.platformOpModifyPlatform);
        _authOps.put("remove",        AuthzConstants.platformOpRemovePlatform);
        _authOps.put("addServer",     AuthzConstants.platformOpAddServer);
        _authOps.put("view",          AuthzConstants.platformOpViewPlatform);
        _authOps.put("monitor",       AuthzConstants.platformOpMonitorPlatform);
        _authOps.put("control",       AuthzConstants.platformOpControlPlatform);
        _authOps.put("modifyAlerts",  AuthzConstants.platformOpManageAlerts);
    }
    
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
    @Override
    public void setName(String name) {
        super.setName(name);
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

    void setConfigResponse(ConfigResponseDB configResponse) {
        _configResponse = configResponse;
    }

    public Agent getAgent() {
        return _agent;
    }

    public void setAgent(Agent agent) {
        _agent = agent;
    }

    public Collection<Ip> getIps() {
        return _ips;
    }

    void setIps(Collection ips) {
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
    
    /**
     * @return the readonly collection of servers
     */
    public Collection<Server> getServers() {
        return Collections.unmodifiableCollection(_servers);
    }

    /**
     * @return the persisted hibernate Bag of servers
     */
    public Collection<Server> getServersBag() {
        return _servers;
    }
    
    void setServersBag(Collection servers) {
        _servers = servers;
    }

    /**
     * Update an existing appdef platform with data from an AI platform.
     * @param aiplatform the AI platform object to use for data
     */
    Resource updateWithAI(AIPlatformValue aiplatform, String owner,
                      Resource resource) {
        Resource changedResource = null;
        setResource(resource);
        if (aiplatform.getName() != null
            && !aiplatform.getName().equals(getName())) {
            setName(aiplatform.getName());
        // if the fqdn and the name are currently equal 
        // but only fqdn is changing,
        // then the name should change as well
        } else if (!getFqdn().equals(aiplatform.getFqdn())
                   && getName().equals(getFqdn())) {
            setName(aiplatform.getFqdn());
            resource.setName(aiplatform.getFqdn());
            changedResource = resource;
        }
        setCertdn(aiplatform.getCertdn());
        setFqdn(aiplatform.getFqdn());
        setModifiedBy(owner);
        // setLocation("");
        setCpuCount(aiplatform.getCpuCount());
        setDescription(aiplatform.getDescription());
        return changedResource;
    }

    /**
     * Compare this entity to a value object
     * (legacy code from entity bean)
     *
     * @return changedProps The changed properties or an empty map if this platform is the same as the one in the val
     * obj
     */
    @Override
    public Map<String, String> changedProperties(AppdefResourceValue appdefResourceValue) {

        PlatformValue platformValue = (PlatformValue) appdefResourceValue;
        Map<String, String> changedProps = super.changedProperties(platformValue);

        if (!ObjectUtils.equals(getName(), platformValue.getName())) {
            changedProps.put("Name", platformValue.getName());
        }
        if (!ObjectUtils.equals(getDescription(), platformValue.getDescription())) {
            changedProps.put("Description", platformValue.getDescription());
        }
        if (!ObjectUtils.equals(getCertdn(), platformValue.getCertdn())) {
            changedProps.put("Certdn", platformValue.getCertdn());
        }
        if (!ObjectUtils.equals(getCommentText(), platformValue.getCommentText())) {
            changedProps.put("CommentText", platformValue.getCommentText());
        }
        if (!ObjectUtils.equals(getCpuCount(), platformValue.getCpuCount())) {
            changedProps.put("CpuCount", String.valueOf(platformValue.getCpuCount()));
        }
        if (!ObjectUtils.equals(getFqdn(), platformValue.getFqdn())) {
            changedProps.put("Fqdn", platformValue.getFqdn());
        }
        if (!ObjectUtils.equals(getLocation(), platformValue.getLocation())) {
            changedProps.put("Location", platformValue.getLocation());
        }
        if (!platformValue.getUpdatedIpValues().isEmpty()) {
            changedProps.put("IpValues", platformValue.getUpdatedIpValues().toString());
        }
        if (!platformValue.getRemovedIpValues().isEmpty()) {
            changedProps.put("IpValues:Removed", platformValue.getRemovedIpValues().toString());
        }
        if (!ObjectUtils.equals(getAgent(), platformValue.getAgent())) {
            changedProps.put("Agent", String.valueOf((platformValue.getAgent())));
        }
        return changedProps;
    }

    public Map<String, String> changedProperties(AIPlatformValue aiPlatformValue) {

       Map<String, String> changedProps = new TreeMap<String, String>();

        if (!ObjectUtils.equals(getName(), aiPlatformValue.getName())) {
            changedProps.put("Name", aiPlatformValue.getName());
        }
        if (!ObjectUtils.equals(getDescription(), aiPlatformValue.getDescription())) {
            changedProps.put("Description", aiPlatformValue.getDescription());
        }
        if (!ObjectUtils.equals(getCertdn(), aiPlatformValue.getCertdn())) {
            changedProps.put("Certdn", aiPlatformValue.getCertdn());
        }
        if (!ObjectUtils.equals(getCpuCount(), aiPlatformValue.getCpuCount())) {
            changedProps.put("CpuCount", String.valueOf(aiPlatformValue.getCpuCount()));
        }
        if (!ObjectUtils.equals(getFqdn(), aiPlatformValue.getFqdn())) {
            changedProps.put("Fqdn", aiPlatformValue.getFqdn());
        }
        if (!aiPlatformValue.getAddedAIIpValues().isEmpty()) {
            changedProps.put("IpValues:Added", aiPlatformValue.getAddedAIIpValues().toString());
        }
        if (!aiPlatformValue.getUpdatedAIIpValues().isEmpty()) {
            changedProps.put("IpValues:Changed", aiPlatformValue.getUpdatedAIIpValues().toString());
        }
        if (!aiPlatformValue.getRemovedAIIpValues().isEmpty()) {
            changedProps.put("IpValues:Removed", aiPlatformValue.getRemovedAIIpValues().toString());
        }
        return changedProps;
    }

    private PlatformValue _platformValue = new PlatformValue();

    /**
     * legacy DTO pattern
     * @deprecated use (this) Platform object instead
     */
    public PlatformValue getPlatformValue()
    {
        _platformValue.setSortName(getSortName());
        _platformValue.setCommentText(getCommentText());
        _platformValue.setModifiedBy(getModifiedBy());
        _platformValue.setOwner(getOwner());
        _platformValue.setConfigResponseId(getConfigResponse().getId());
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
            // Make sure that the agent is loaded
            getAgent().getAddress();
            _platformValue.setAgent(getAgent());
        }
        else
            _platformValue.setAgent(null);
        return _platformValue;
    }

    private String getOwner() {
        return getResource() != null && getResource().getOwner() != null ?
                getResource().getOwner().getName() : "";
    }

    /**
     * convenience method for copying simple values from
     * legacy Platform Value Object.
     */
    void setPlatformValue(PlatformValue pv) {
        setDescription(pv.getDescription());
        setCommentText(pv.getCommentText());
        setModifiedBy(pv.getModifiedBy());
        setLocation(pv.getLocation());
        setCpuCount(pv.getCpuCount());
        setCertdn(pv.getCertdn());
        setFqdn(pv.getFqdn());
        setName(pv.getName());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Platform) && super.equals(obj);
    }

    public AppdefResourceType getAppdefResourceType() {
        return _platformType;
    }

    public AppdefResourceValue getAppdefResourceValue() {
        return getPlatformValue();
    }

    protected String _getAuthzOp(String op) {
        return (String)_authOps.get(op);
    }
}
