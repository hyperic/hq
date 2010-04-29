/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2008], Hyperic, Inc. 
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
import java.util.Iterator;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class PlatformType extends AppdefResourceType {
    private String            _os;
    private String            _osVersion;
    private String            _arch;
    private String            _plugin;
    private Collection        _serverTypes = new ArrayList();
    private Collection        _platforms = new ArrayList();
    private PlatformTypeValue _platformTypeValue = new PlatformTypeValue();

    protected PlatformType() {
    }

    public PlatformType(String name, String plugin) {
        setName(name);
        setPlugin(plugin);
    }

    public int getAuthzType() {
        return AuthzConstants.authzPlatformProto.intValue();
    }

    public String getOs() {
        return _os;
    }

    protected void setOs(String os) {
        _os = os;
    }

    public String getOsVersion() {
        return _osVersion;
    }

    protected void setOsVersion(String osVersion) {
        _osVersion = osVersion;
    }

    public String getArch() {
        return _arch;
    }

    protected void setArch(String arch) {
        _arch = arch;
    }

    public String getPlugin() {
        return _plugin;
    }

    protected void setPlugin(String plugin) {
        _plugin = plugin;
    }

    public Collection<ServerType> getServerTypes() {
        return Collections.unmodifiableCollection(_serverTypes);
    }
    
    protected Collection getServerTypesBag() {
        return _serverTypes;
    }

    protected void setServerTypesBag(Collection servers) {
        _serverTypes = servers;
    }

    public Collection<Platform> getPlatforms() {
        return Collections.unmodifiableCollection(_platforms);
    }
    
    protected Collection getPlatformBag() {
        return _platforms;
    }

    protected void setPlatformBag(Collection platforms) {
        _platforms = platforms;
    }

    private void registerNewPlatform(Platform p) {
        _platforms.add(p);
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    }
    /**
     * Create a new platform based on the AI platform value.
     */
    protected Platform create(AIPlatformValue aip, String initialOwner,
                              ConfigResponseDB config, Agent agent) 
    {
        Platform p = copyAIPlatformValue(aip);
        p.setPlatformType(this);
        p.setConfigResponse(config);
        p.setModifiedBy(initialOwner);
        p.setAgent(agent);
        registerNewPlatform(p);
        return p;
    }

    protected Platform create(PlatformValue pv, Agent agent, 
                              ConfigResponseDB config) 
    {
        Platform p = new Platform();
        
        p.setName(pv.getName());
        p.setDescription(pv.getDescription());
        p.setCertdn(pv.getCertdn());
        p.setCommentText(pv.getCommentText());
        p.setCpuCount(pv.getCpuCount());
        p.setFqdn(pv.getFqdn());
        p.setLocation(pv.getLocation());
        p.setModifiedBy(pv.getModifiedBy());
        p.setPlatformType(this);
        p.setAgent(agent);
        p.setConfigResponse(config);
        
        for (Iterator i=pv.getAddedIpValues().iterator(); i.hasNext();) {
            IpValue ipv = (IpValue)i.next();
            p.addIp(ipv.getAddress(), ipv.getNetmask(), ipv.getMACAddress());
        }
        registerNewPlatform(p);
        return p;
    }

    private Platform copyAIPlatformValue(AIPlatformValue aip) {
        Platform p = new Platform();

        p.setCertdn(aip.getCertdn());
        p.setFqdn(aip.getFqdn());
        p.setName(aip.getName());
        p.setDescription(aip.getDescription());
        p.setCommentText("");
        p.setLocation("");
        p.setCpuCount(aip.getCpuCount());
        fixName(p);
        return p;
    }

    private void fixName(Platform p) {
        // if name is not set then set it to fqdn (assuming it is set of course)
        String name = p.getName();
        if (name == null || "".equals(name.trim())) {
            p.setName(p.getFqdn());
        }
    }
    
    /**
     * legacy DTO pattern
     * @deprecated use (this) PlatformType object instead
     */
    public PlatformTypeValue getPlatformTypeValue() {
        _platformTypeValue.setSortName(getSortName());
        _platformTypeValue.setName(getName());
        _platformTypeValue.setDescription(getDescription());
        _platformTypeValue.setPlugin(getPlugin());
        _platformTypeValue.setId(getId());
        _platformTypeValue.setMTime(getMTime());
        _platformTypeValue.setCTime(getCTime());
        return _platformTypeValue;
    }

    public boolean equals(Object obj) {
        if (obj instanceof PlatformType == false)
            return false;
        
        return ((PlatformType)obj).getName().equals(getName());
    }
    
    public int hashCode() {
        return getName().hashCode();
    }

    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return getPlatformTypeValue();
    }
}
