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

package org.hyperic.hq.dao;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class ConfigResponseDAO extends HibernateDAO
{ 
    public ConfigResponseDAO(DAOFactory f) {
        super(ConfigResponseDB.class, f);
    }

    public ConfigResponseDB findById(Integer id) {
        return (ConfigResponseDB)super.findById(id);
    }

    public ConfigResponseDB get(Integer id) {
        return (ConfigResponseDB)super.get(id);
    }

    public void save(ConfigResponseDB entity) {
        super.save(entity);
    }

    public void remove(ConfigResponseDB entity) {
        super.remove(entity);
    }

    /**
     * @return newly instantiated config response object
     */
    public ConfigResponseDB create() {
        ConfigResponseDB newConfig = new ConfigResponseDB();
        save(newConfig);
        return newConfig;
    }

    /**
     * Initialize the config response for a new platform
     */
    public ConfigResponseDB createPlatform() {
        ConfigResponseDB cLocal = new ConfigResponseDB();
        try {
            ConfigResponse metricCfg = new ConfigResponse();
            ConfigResponse productCfg = new ConfigResponse();
            cLocal.setProductResponse(productCfg.encode());
            cLocal.setMeasurementResponse(metricCfg.encode());
            save(cLocal);
        } catch (EncodingException e) {
            // will never happen, we're setting up an empty response
		}
        return cLocal;
    }

    public ConfigResponseDB findByPlatformId(Integer id) {
        String sql = "select c from ConfigResponseDB c, Platform p where " +
                     "c.id = p.configResponse.id and " +
                     "p.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .setCacheable(true)
            .setCacheRegion("ConfigReponseDB.findByPlatformId")
            .uniqueResult();
    }

    public ConfigResponseDB findByServerId(Integer id) {
        String sql = "select c from ConfigResponseDB c, Server s where " +
                     "c.id = s.configResponse.id and " +
                     "s.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .setCacheable(true)
            .setCacheRegion("ConfigReponseDB.findByServerId")
            .uniqueResult();
    }

    public ConfigResponseDB findByServiceId(Integer id) {
        String sql = "select c from ConfigResponseDB c, Service s where " +
                     "c.id = s.configResponse.id and " +
                     "s.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .setCacheable(true)
            .setCacheRegion("ConfigReponseDB.findByServiceId")
            .uniqueResult();
    }
}
