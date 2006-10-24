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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;

public class PlatformType extends AppdefResourceType {
    private String            _os;
    private String            _osVersion;
    private String            _arch;
    private String            _plugin;
    private Collection        _serverTypes = new ArrayList();
    private Collection        _platforms = new ArrayList();
    private PlatformTypePK    _pkey = new PlatformTypePK();
    private PlatformTypeValue _platformTypeValue = new PlatformTypeValue();

    protected PlatformType() {
    }

    public PlatformType(PlatformTypeValue ptv) {
        setName(ptv.getName());
        setPlugin(ptv.getPlugin());
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

    public Collection getServerTypes() {
        return Collections.unmodifiableCollection(_serverTypes);
    }

    protected void setServerTypes(Collection servers) {
        _serverTypes = servers;
    }

    public Collection getPlatforms() {
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
        p.setOwner(initialOwner);
        p.setAgent(agent);
        registerNewPlatform(p);
        return p;
    }

    protected Platform create(PlatformValue pv, Agent agent, 
                              ConfigResponseDB config) 
    {
        return newPlatform(pv, config, agent);
    }

    /*
    protected Platform create(PlatformType ptype, AIPlatformValue aip, 
                              AgentPK agent)
    {
        AgentDAO aDAO = DAOFactory.getDAOFactory().getAgentDAO();
        Platform p = findByName(aip.getName());

        if (p != null) {
            throwDupPlatform(p.getId(), aip.getName());
        }
        p = copyAIPlatformValue(aip);
        p.setPlatformType(ptype);
        p.setAgent(aDAO.findById(agent.getId()));
        registerNewPlatform(p);
        return p;
    }

    public Platform create(PlatformType ptype, PlatformValue pv, 
                           AgentPK agent)
    {
        AgentDAO aDAO = DAOFactory.getDAOFactory().getAgentDAO();
        Platform p = findByName(pv.getName());

        if (p != null) {
            throwDupPlatform(p.getId(), pv.getName());
        }
        p = newPlatform(pv);
        p.setPlatformType(this);

        p.setAgent(aDAO.findById(agent.getId()));
        return p;
    }
    */

    private Platform copyAIPlatformValue(AIPlatformValue aip) {
        Platform p = new Platform();

        p.setCertdn(aip.getCertdn());
        p.setFqdn(aip.getFqdn());
        p.setName(aip.getName());
        p.setDescription(aip.getDescription());
        p.setCommentText("");
        p.setLocation("");
        p.setCpuCount(aip.getCpuCount());
        return p;
    }
    
    private Platform newPlatform(PlatformValue pv, ConfigResponseDB config,
                                 Agent agent) 
    {
        Platform p = new Platform();

        p.setName(pv.getName());
        p.setCertdn(pv.getCertdn());
        p.setCommentText(pv.getCommentText());
        p.setCpuCount(pv.getCpuCount());
        p.setFqdn(pv.getFqdn());
        p.setLocation(pv.getLocation());
        p.setModifiedBy(pv.getModifiedBy());
        p.setOwner(pv.getOwner());
        p.setPlatformType(this);
        p.setAgent(agent);
        p.setIps(pv.getAddedIpValues());

        registerNewPlatform(p);
        return p;
    }

    /**
     * legacy EJB DTO pattern
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
        _platformTypeValue.removeAllServerTypeValues();
        if (getServerTypes() != null) {
            Iterator isv = getServerTypes().iterator();
            while (isv.hasNext()){
                _platformTypeValue.addServerTypeValue(
                    ((ServerType)isv.next()).getServerTypeValue());
            }
        }
        return _platformTypeValue;
    }

    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) PlatformType object instead
     */
    public PlatformTypeValue getPlatformTypeValueObject() {
        PlatformTypeValue vo = new PlatformTypeValue();
        vo.setSortName(getSortName());
        vo.setName(getName());
        vo.setDescription(getDescription());
        vo.setPlugin(getPlugin());
        vo.setId(getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        return vo;
    }

    public Set getServerTypeSnapshot() {
        if (getServerTypes() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getServerTypes());
    }

    /**
     * @deprecated use getId()
     */
    public PlatformTypePK getPrimaryKey() {
        _pkey.setId(getId());
        return _pkey;
    }

    public boolean equals(Object obj) {
        return (obj instanceof PlatformType) && super.equals(obj);
    }
}
