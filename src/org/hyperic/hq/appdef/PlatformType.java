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

import org.hibernate.NonUniqueObjectException;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.AgentDAO;
import org.hyperic.hibernate.dao.ConfigResponseDAO;
import org.hyperic.hibernate.dao.PlatformTypeDAO;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformTypePK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class PlatformType extends AppdefResourceType {
    private String            _os;
    private String            _osVersion;
    private String            _arch;
    private String            _plugin;
    private Collection        _serverTypes = new ArrayList();
    private Collection        _platforms = new ArrayList();
    private PlatformTypePK    _pkey = new PlatformTypePK();
    private PlatformTypeValue _platformTypeValue = new PlatformTypeValue();

    public PlatformType() {
    }

    public String getOs() {
        return _os;
    }

    public void setOs(String os) {
        _os = os;
    }

    public String getOsVersion() {
        return _osVersion;
    }

    public void setOsVersion(String osVersion) {
        _osVersion = osVersion;
    }

    public String getArch() {
        return _arch;
    }

    public void setArch(String arch) {
        _arch = arch;
    }

    public String getPlugin() {
        return _plugin;
    }

    public void setPlugin(String plugin) {
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

    protected void setPlatforms(Collection platforms) {
        _platforms = platforms;
    }

    private Platform findByName(String name) {
        return DAOFactory.getDAOFactory().getPlatformDAO().findByName(name);
    }
    
    private ConfigResponseDAO getConfigDAO() {
        return DAOFactory.getDAOFactory().getConfigResponseDAO();
    }

    private AgentDAO getAgentDAO() {
        return DAOFactory.getDAOFactory().getAgentDAO();
    }
    
    /**
     * Create a new platform based on the AI platform value.
     */
    public Platform create(AIPlatformValue aip, String initialOwner) {
        Platform p = findByName(aip.getName());

        if (p != null) {
            throwDupPlatform(p.getId(), aip.getName());
        }

        ConfigResponseDB config = getConfigDAO().createPlatform();

        p = copyAIPlatformValue(aip);
        p.setPlatformType(this);
        p.setConfigResponse(config);
        p.setModifiedBy(initialOwner);
        p.setOwner(initialOwner);
        Agent agent = getAgentDAO().findByAgentToken(aip.getAgentToken());
        
        if (agent == null) {
            throw new ObjectNotFoundException(aip.getId(),
                                              "Unable to find agent: " +
                                              aip.getAgentToken());
                                              
        }

        p.setAgent(agent);
        _platforms.add(p);
        return p;
    }

    public Platform create(PlatformValue pv, AgentPK agent) {
        Platform p = findByName(pv.getName());

        if (p != null) {
            throwDupPlatform(p.getId(), pv.getName());
        }

        p = newPlatform(pv);
        if (agent != null) {
            p.setAgent(getAgentDAO().findById(agent.getId()));
        }
        return p;
    }

    public Platform create(PlatformType ptype, AIPlatformValue aip, 
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
        _platforms.add(p);
        return p;
    }

    /*
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

    private void throwDupPlatform(Serializable id, String platName) {
        throw new NonUniqueObjectException(id, "duplicate platform found " + 
                                           "with name: " + platName);
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
        return p;
    }
    
    private Platform newPlatform(PlatformValue pv) {
        ConfigResponseDAO crDAO = getConfigDAO(); 
        Platform p = new Platform();

        p.setName(pv.getName());
        p.setCertdn(pv.getCertdn());
        p.setCommentText(pv.getCommentText());
        p.setCpuCount(pv.getCpuCount());
        p.setFqdn(pv.getFqdn());
        p.setLocation(pv.getLocation());
        p.setModifiedBy(pv.getModifiedBy());
        p.setOwner(pv.getOwner());

        // If these fks are invalid, Hibernate will throw
        // exception
        if (pv.getConfigResponseId() != null) {
            p.setConfigResponse(crDAO.findById(pv.getConfigResponseId()));
        } else {
            p.setConfigResponse(crDAO.createPlatform());
        }
        
        p.setPlatformType(this);
        _platforms.add(p);
        
        if (pv.getAgent() != null) {
            AgentDAO aDAO = DAOFactory.getDAOFactory().getAgentDAO();
            p.setAgent(aDAO.findById(pv.getAgent().getId()));
        }
        p.setIps(pv.getAddedIpValues());
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
