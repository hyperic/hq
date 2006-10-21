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

import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformTypePK;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Pojo for hibernate hbm mapping file
 */
public class PlatformType extends AppdefResourceType
    
{
    private String os;
    private String osVersion;
    private String arch;
    private String plugin;
    private Collection serverTypes;
    private Collection platforms;

    /**
     * default constructor
     */
    public PlatformType()
    {
        super();
    }

    // Property accessors
    public String getOs()
    {
        return this.os;
    }

    public void setOs(String os)
    {
        this.os = os;
    }

    public String getOsVersion()
    {
        return this.osVersion;
    }

    public void setOsVersion(String osVersion)
    {
        this.osVersion = osVersion;
    }

    public String getArch()
    {
        return this.arch;
    }

    public void setArch(String arch)
    {
        this.arch = arch;
    }

    public String getPlugin()
    {
        return this.plugin;
    }

    public void setPlugin(String plugin)
    {
        this.plugin = plugin;
    }

    public Collection getServerTypes()
    {
        return this.serverTypes;
    }

    public void setServerTypes(Collection servers)
    {
        this.serverTypes = servers;
    }

    public Collection getPlatforms()
    {
        return this.platforms;
    }

    public void setPlatforms(Collection platforms)
    {
        this.platforms = platforms;
    }

    public Platform createPlatform(PlatformValue platform, AgentPK agent)
    {
        throw new UnsupportedOperationException(
            "use PlatformDAO.createPlatform()"
        );
    }

    public Platform createPlatform(AIPlatformValue aiplatform,
                                   String initialOwner)
    {
        throw new UnsupportedOperationException(
            "use PlatformDAO.createPlatform()"
        );
    }

    private PlatformTypeValue platformTypeValue = new PlatformTypeValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) PlatformType object instead
     * @return
     */
    public PlatformTypeValue getPlatformTypeValue()
    {
        platformTypeValue.setSortName(getSortName());
        platformTypeValue.setName(getName());
        platformTypeValue.setDescription(getDescription());
        platformTypeValue.setPlugin(getPlugin());
        platformTypeValue.setId(getId());
        platformTypeValue.setMTime(getMTime());
        platformTypeValue.setCTime(getCTime());
        platformTypeValue.removeAllServerTypeValues();
        if (getServerTypes() != null) {
            Iterator isv = getServerTypes().iterator();
            while (isv.hasNext()){
                platformTypeValue.addServerTypeValue(
                    ((ServerType)isv.next()).getServerTypeValue());
            }
        }
        return platformTypeValue;
    }

    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) PlatformType object instead
     * @return
     */
    public PlatformTypeValue getPlatformTypeValueObject()
    {
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

    public Set getServerTypeSnapshot()
    {
        if (getServerTypes() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getServerTypes());
    }

    private PlatformTypePK pkey = new PlatformTypePK();
    /**
     * @deprecated use getId()
     * @return
     */
    public PlatformTypePK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    public boolean equals(Object obj)
    {
        if (!super.equals(obj) || !(obj instanceof PlatformType)) {
            return false;
        }
        return true;
    }
}
