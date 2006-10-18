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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerTypeLocal;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerTypeUtil;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerUtil;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServiceLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceVOHelperLocal;
import org.hyperic.hq.appdef.shared.ServiceVOHelperUtil;
import org.hyperic.hq.appdef.ServiceType;
import org.hyperic.hq.appdef.Server;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @ejb:bean name="ServerVOHelper"
 *      jndi-name="ejb/appdef/ServerVOHelper"
 *      local-jndi-name="LocalServerVOHelper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class ServerVOHelperEJBImpl extends AppdefSessionEJB 
    implements SessionBean {

    private Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.session.ServerVOHelperEJBImpl");
    
    private final String SERVER_SQL = "SELECT ID, PLATFORM_ID, SERVER_TYPE_ID," 
        + " NAME, SORT_NAME, DESCRIPTION, MTIME, CTIME, " 
        + " MODIFIED_BY, LOCATION, OWNER, CONFIG_RESPONSE_ID, RUNTIMEAUTODISCOVERY, " 
        + " AUTODISCOVERY_ZOMBIE, SERVICESAUTOMANAGED, INSTALLPATH, AUTOINVENTORYIDENTIFIER " 
        + " FROM EAM_SERVER WHERE ID = ?";
    
    /**
     * Get the server value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */    
    public ServerValue getServerValue(ServerPK pk) 
        throws FinderException, NamingException {
        ServerValue vo = VOCache.getInstance().getServer(pk.getId());
        if(vo != null) {
            return vo;
        }
        Server ejb = getServerDAO().findByPrimaryKey(pk);
        return getServerValue(ejb);
    }
            
    /**
     * Get the server value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     */    
    public ServerValue getServerValue(ServerLocal ejb) 
        throws NamingException {
        ServerValue vo = VOCache.getInstance().getServer(
            ((ServerPK) ejb.getPrimaryKey()).getId());
        if(vo != null) {
            log.debug("Returning cached instance for Server: " + vo.getId());
            return vo;            
        }
        return (ServerValue)getServerValueImpl(ejb);
    }

    /**
     * Get the server value object
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServerValue getServerValue(Server ejb)
        throws NamingException {
        ServerValue vo = VOCache.getInstance().getServer(ejb.getId());
        if(vo != null) {
            log.debug("Returning cached instance for Server: " + vo.getId());
            return vo;
        }
        return (ServerValue)getServerValueImpl(ejb);
    }

    /**
     * Get the server light value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     */    
    public ServerLightValue getServerLightValue(ServerPK pk) 
        throws FinderException, NamingException {
        ServerLightValue vo = VOCache.getInstance().getServerLight(pk.getId());
        if(vo != null) {
            return vo;
        }
        return (ServerLightValue)getServerLightValueImpl(pk);
    }
            
    /**
     * Get the server light value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     */    
    public ServerLightValue getServerLightValue(ServerLocal ejb) 
        throws NamingException {
        try {
            return this.getServerLightValue((ServerPK) ejb.getPrimaryKey());
        } catch (FinderException e) {
            // Should never happen
            log.error("EJB passed for non-existent server entity: " +
                    ((ServerPK) ejb.getPrimaryKey()).getId());
            return null;
        }
    }    
    
    /**
     * Synchronized VO retrieval 
     */
    private AppdefResourceValue getServerValueImpl(ServerLocal ejb)
        throws NamingException {
        VOCache cache = VOCache.getInstance();
        AppdefResourceValue vo;
        synchronized(cache.getServerLock()) {
            vo = ejb.getServerValue();
            cache.put(vo.getId(), vo);
        }
        return vo;
    }

    /**
     * Synchronized VO retrieval
     */
    private AppdefResourceValue getServerValueImpl(Server ejb)
        throws NamingException {
        VOCache cache = VOCache.getInstance();
        AppdefResourceValue vo;
        synchronized(cache.getServerLock()) {
            vo = ejb.getServerValue();
            cache.put(vo.getId(), vo);
        }
        return vo;
    }
    
    /**
     * Synchronized VO retrieval 
     */
    private AppdefResourceValue getServerLightValueImpl(ServerPK spk)
        throws NamingException {
        VOCache cache = VOCache.getInstance();
        AppdefResourceValue vo;
        synchronized(cache.getServerLock()) {
            vo       = (ServerLightValue) getServerValueDirectSQL(spk, true);
            cache.put(vo.getId(), vo);
        }
        return vo;
    } 
    
    private AppdefResourceValue getServerValueDirectSQL(ServerPK pk, boolean getLight) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            log.debug("VOCache Miss! Retrieving ServerValue from database.");
            conn = DBUtil.getConnByContext(getInitialContext(), HQConstants.DATASOURCE);
            ps = conn.prepareStatement(SERVER_SQL);
            ps.setInt(1, pk.getId().intValue());
            rs = ps.executeQuery();
            rs.next();
            if(getLight) {
                ServerLightValue slv = new ServerLightValue();
                slv.setId(new Integer(rs.getInt(1)));
                slv.setServerType(getServerTypeValue(new ServerTypePK(new Integer(rs.getInt(3)))));
                slv.setName(rs.getString(4));
                slv.setSortName(rs.getString(5));
                slv.setDescription(rs.getString(6));
                slv.setMTime(new Long(rs.getLong(7)));
                slv.setCTime(new Long(rs.getLong(8)));
                slv.setModifiedBy(rs.getString(9));
                slv.setLocation(rs.getString(10));
                slv.setOwner(rs.getString(11));
                slv.setConfigResponseId(new Integer(rs.getInt(12)));
                slv.setRuntimeAutodiscovery((rs.getBoolean(13)));
                slv.setAutodiscoveryZombie(rs.getBoolean(14));
                slv.setServicesAutomanaged(rs.getBoolean(15));
                slv.setInstallPath(rs.getString(16));
                slv.setAutoinventoryIdentifier(rs.getString(17));
                return slv;
            } else {
                ServerValue sv = new ServerValue();
                sv.setId(new Integer(rs.getInt(1)));
                PlatformLightValue platLight = PlatformVOHelperUtil.getLocalHome().create()
                    .getPlatformLightValue(new PlatformPK(new Integer(rs.getInt(2))));
                sv.setPlatform(platLight);
                sv.setServerType(getServerTypeValue(new ServerTypePK(new Integer(rs.getInt(3)))));
                sv.setName(rs.getString(4));
                sv.setSortName(rs.getString(5));
                sv.setDescription(rs.getString(6));
                sv.setMTime(new Long(rs.getLong(7)));
                sv.setCTime(new Long(rs.getLong(8)));
                sv.setModifiedBy(rs.getString(9));
                sv.setLocation(rs.getString(10));
                sv.setOwner(rs.getString(11));
                sv.setConfigResponseId(new Integer(rs.getInt(12)));
                sv.setRuntimeAutodiscovery((rs.getBoolean(13)));
                sv.setAutodiscoveryZombie(rs.getBoolean(14));
                sv.setServicesAutomanaged(rs.getBoolean(15));
                sv.setInstallPath(rs.getString(16));
                sv.setAutoinventoryIdentifier(rs.getString(17));
                return sv;
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
    }

    /**
     * Get the server type value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ServerTypeValue getServerTypeValue(ServerTypePK pk)
        throws FinderException, NamingException {
            ServerTypeValue vo = VOCache.getInstance().getServerType(pk.getId());
            if(vo != null) {
                return vo;
            }
            ServerTypeLocal ejb = ServerTypeUtil.getLocalHome().findByPrimaryKey(pk);
            return getServerTypeValue(ejb);
    }
                        
    /**
     * Get the server type value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ServerTypeValue getServerTypeValue(ServerTypeLocal ejb)
        throws NamingException {
        ServerTypeValue vo = VOCache.getInstance()
            .getServerType(((ServerTypePK)ejb.getPrimaryKey()).getId());
        if(vo != null) {
            log.debug("Returning cached instance of ServerType: " + vo.getId());    
            return vo;
        }
        return getServerTypeValueImpl(ejb);
    }

    /**
     * Synchronized VO retrieval
     */
    private ServerTypeValue getServerTypeValueImpl(ServerTypeLocal ejb)
        throws NamingException {
        VOCache cache = VOCache.getInstance();
        ServerTypeValue vo;
        synchronized(cache.getServerTypeLock()) {
            vo = cache
                .getServerType(((ServerTypePK)ejb.getPrimaryKey()).getId());
            if(vo != null) {
                log.debug("Returning cached instance of ServerType: " + vo.getId());
                return vo;
            }
            vo = ejb.getServerTypeValueObject();
            Iterator serviceIt = ejb.getServiceTypeSnapshot().iterator();
            while(serviceIt.hasNext()) {
                vo.addServiceTypeValue(((ServiceType)serviceIt.next()).getServiceTypeValue());
            }
            cache.put(vo.getId(), vo);
        }
        return vo;
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    
}
