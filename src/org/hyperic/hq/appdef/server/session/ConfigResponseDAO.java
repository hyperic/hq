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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.dao.HibernateDAO;
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

    void save(ConfigResponseDB entity) {
        super.save(entity);
    }

    public void remove(ConfigResponseDB entity) {
        super.remove(entity);
    }

    /**
     * @return newly instantiated config response object
     */
    ConfigResponseDB create() {
        ConfigResponseDB newConfig = new ConfigResponseDB();
        save(newConfig);
        return newConfig;
    }

    /**
     * Initialize the config response for a new platform
     */
    ConfigResponseDB createPlatform() {
        ConfigResponseDB cLocal = new ConfigResponseDB();
        try {
            ConfigResponse empty = new ConfigResponse();
            cLocal.setProductResponse(empty.encode());
            cLocal.setMeasurementResponse(empty.encode());
            save(cLocal);
        } catch (EncodingException e) {
            // will never happen, we're setting up an empty response
		}
        return cLocal;
    }

    ConfigResponseDB findByPlatformId(Integer id) {
        PlatformDAO dao = new PlatformDAO(getFactory());
        Platform plat = dao.get(id);
        return (plat == null ? null : plat.getConfigResponse());
    }

    public ConfigResponseDB findByServerId(Integer id) {
        ServerDAO dao = new ServerDAO(getFactory());
        Server server = dao.get(id);
        return (server == null ? null : server.getConfigResponse());
    }

    public ConfigResponseDB findByServiceId(Integer id) {
        ServiceDAO dao = new ServiceDAO(getFactory());
        Service service = dao.get(id);
        return (service == null ? null : service.getConfigResponse());
    }
    
    /**
     * ValidationError setter so that the version isn't incremented.  The issue
     * is that when measurements are being scheduled during a resource
     * creation process, it's possible that an error will be registering
     * at the same time that the ConfigResponseDB object is being used somewhere
     * else.
     */
    void setValidationError(ConfigResponseDB resp, String error) {
        String sql = "update ConfigResponseDB set validationError = ? " +
        		     "where id = ?";
        getSession().createQuery(sql)
            .setString(0, error)
            .setParameter(1, resp.getId())
            .executeUpdate();
    }
}
