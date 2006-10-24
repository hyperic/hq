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

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AgentTypeValue;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerVOHelperLocal;
import org.hyperic.hq.appdef.shared.ServerVOHelperUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="PlatformVOHelper"
 *      jndi-name="ejb/appdef/PlatformVOHelper"
 *      local-jndi-name="LocalPlatformVOHelper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class PlatformVOHelperEJBImpl extends AppdefSessionEJB 
    implements SessionBean {
    
    private Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.session.PlatformVOHelperEJBImpl");
    
    private final String PLATFORM_SQL = "SELECT ID, PLATFORM_TYPE_ID," 
        + " NAME, SORT_NAME, CERTDN, FQDN, DESCRIPTION, MTIME, CTIME, AGENT_ID, " 
        + " MODIFIED_BY, LOCATION, OWNER, CONFIG_RESPONSE_ID, CPU_COUNT " 
        + " FROM EAM_PLATFORM WHERE ID = ?";
    
    private final String IP_SQL = "SELECT ID, ADDRESS, NETMASK, MAC_ADDRESS, "
        + " CTIME, MTIME FROM EAM_IP WHERE PLATFORM_ID = ? ";
    
    private final String AGENT_SQL = "SELECT A.ID, A.ADDRESS, A.PORT, A.AUTHTOKEN, A.AGENTTOKEN, "
        + " A.VERSION, A.CTIME, A.MTIME, T.NAME, T.SORT_NAME, T.ID, T.MTIME, T.CTIME "
        + " FROM EAM_AGENT A, EAM_AGENT_TYPE T WHERE A.ID = ? AND A.AGENT_TYPE_ID = T.ID ";
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PlatformLightValue getPlatformLightValue(PlatformPK ppk) throws FinderException,
        NamingException {
            PlatformLightValue vo = VOCache.getInstance().getPlatformLight(ppk.getId());
            if(vo != null) {
                return vo;
            }
            Platform ejb = getPlatformDAO().findByPrimaryKey(ppk);
            return getPlatformLightValue(ejb);
    }
                
    /**
     * Get a value object for this platform
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PlatformLightValue getPlatformLightValue(Platform ejb) throws
        NamingException {
        // first see if its in the cache
        PlatformLightValue pv = VOCache.getInstance()
            .getPlatformLight(ejb.getId());
        if (pv != null) {
            log.debug("Returning cached instance for platform: " + pv.getId());
            return pv;
        }
        return (PlatformLightValue)getPlatformValueImpl(ejb, true);
    }    
    
    /** 
     * Synchronized VO retrieval
     */
    private AppdefResourceValue getPlatformValueImpl(Platform ejb, boolean getLight) throws
        NamingException {
        VOCache cache = VOCache.getInstance();
        PlatformPK ppk = ejb.getPrimaryKey();
        
        synchronized(cache.getPlatformLock()) {
            
            if (getLight) {
                PlatformLightValue plv = cache.getPlatformLight(ppk.getId());
                if(plv != null) {
                    log.debug("Returning cached instance for platform: " + plv.getId());
                    return plv;
                }
                plv = (PlatformLightValue)this.getPlatformValueDirectSQL(ppk, true);
                // add it to the cache
                cache.put(plv.getId(), plv);
                return plv;
            }
            else {
                // Check the cache again
                PlatformValue pv = cache.getPlatform(ppk.getId());
                if (pv != null) {
                    log.debug("Returning cached instance for platform: " +
                              pv.getId());
                    return pv;
                }
                pv = (PlatformValue)this.getPlatformValueDirectSQL(ppk, false);
                Iterator serverIt = ejb.getServerSnapshot().iterator();
                while (serverIt.hasNext()){
                    try {
                        ServerLightValue slv = ((Server)
                            serverIt.next()).getServerLightValue();
                        pv.addServerValue(slv);
                        cache.put(slv.getId(), slv);
                    } catch (NoSuchObjectLocalException e) {
                        // the server was removed during our iteration
                        // not a problem
                    }
                }
                
                // add it to the cache
                cache.put(pv.getId(), pv);
                return pv;
            }
            
        }
    }
    
    /**
     * Get platform type value object with full CMR graph
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     *
     */
    public PlatformTypeValue getPlatformTypeValue(PlatformTypePK pk) 
    {
        PlatformType ejb = getPlatformTypeDAO().findByPrimaryKey(pk);
        return getPlatformTypeValue(ejb);
    }
    
    /**
     * Get the platform tyep value object with full CMR graph
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     *
     */
    public PlatformTypeValue getPlatformTypeValue(PlatformType ejb)
    {
        // first see if its in the cache
        PlatformTypeValue vo = VOCache.getInstance()
            .getPlatformType(ejb.getId());
        if (vo != null) {
            log.debug("Returning cached instance for platform type: " +
                      vo.getId());
            return vo;
        }
        return getPlatformTypeValueImpl(ejb);
    }

    /**
     * Synchronized VO retrieval
     */
    private PlatformTypeValue getPlatformTypeValueImpl(PlatformType ejb)
    {
        VOCache cache = VOCache.getInstance();
        PlatformTypeValue vo;
        synchronized(cache.getPlatformTypeLock()) {
            // check the cache again
            // first see if its in the cache
            vo = cache.getPlatformType(ejb.getId());
            if (vo != null) {
                log.debug("Returning cached instance for platform type: " +
                    vo.getId());
                return vo;
            }
            vo = ejb.getPlatformTypeValueObject();
            Iterator serverIt = ejb.getServerTypeSnapshot().iterator();
            try {
                ServerVOHelperLocal svo =
                    ServerVOHelperUtil.getLocalHome().create();
                while (serverIt.hasNext()) {
                    try {
                        ServerTypePK stpk =
                            ((ServerType)serverIt.next()).getPrimaryKey();
                        ServerTypeValue stv = svo.getServerTypeValue(stpk);
                        vo.addServerTypeValue(stv);
                    } catch (NoSuchObjectLocalException e) {
                        // no problem... server type was removed while iterating
                    } catch (FinderException e) {
                        // same here
                    }
                }
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
            cache.put(vo.getId(), vo);
        }
        return vo;
    }
    
    private void setIpValuesDirectSQL(PlatformValue pv, Connection conn) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(IP_SQL);
            ps.setInt(1, pv.getId().intValue());
            rs = ps.executeQuery();
            while(rs.next()) {
                IpValue anIp = new IpValue();
                anIp.setId(new Integer(rs.getInt(1)));
                anIp.setAddress(rs.getString(2));
                anIp.setNetmask(rs.getString(3)); 
                anIp.setMACAddress(rs.getString(4));
                anIp.setCTime(new Long(rs.getLong(5)));
                anIp.setMTime(new Long(rs.getLong(6)));
                pv.addIpValue(anIp);
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, null, ps, rs);
        }
    }
    
    private AgentValue getAgentValueDirectSQL(Integer agentId, Connection conn) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(AGENT_SQL);
            ps.setInt(1, agentId.intValue());
            rs = ps.executeQuery();
            rs.next();
            AgentValue av = new AgentValue();
            av.setId(new Integer(rs.getInt(1)));
            av.setAddress(rs.getString(2));
            av.setPort(rs.getInt(3));
            av.setAuthToken(rs.getString(4));
            av.setAgentToken(rs.getString(5));
            av.setVersion(rs.getString(6));
            av.setCTime(new Long(rs.getLong(7)));
            av.setMTime(new Long(rs.getLong(8)));
            av.setAgentType(new AgentTypeValue(rs.getString(9),
                                               rs.getString(10),
                                               new Integer(rs.getInt(11)),
                                               new Long(rs.getLong(12)),
                                               new Long(rs.getLong(13))));
            return av;
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, null, ps, rs);
        }
    }
    
    /**
     * DirectSQL VO retrieval
     *
     */
    private AppdefResourceValue getPlatformValueDirectSQL(PlatformPK pk, boolean getLight) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            log.debug("VOCache Miss! Retrieving PlatformValue from database.");
            conn = DBUtil.getConnByContext(getInitialContext(), HQConstants.DATASOURCE);
            ps = conn.prepareStatement(PLATFORM_SQL);
            ps.setInt(1, pk.getId().intValue());
            rs = ps.executeQuery();
            rs.next();
            if(getLight) {
                PlatformLightValue pv = new PlatformLightValue();
                pv.setId(new Integer(rs.getInt(1)));
                pv.setPlatformType(
                        this.getPlatformTypeValue(new PlatformTypePK(new Integer(rs.getInt(2)))));
                pv.setName(rs.getString(3));
                pv.setSortName(rs.getString(4));
                pv.setCertdn(rs.getString(5));
                pv.setFqdn(rs.getString(6));
                pv.setDescription(rs.getString(7));
                pv.setMTime(new Long(rs.getLong(8)));
                pv.setCTime(new Long(rs.getLong(9)));
                pv.setModifiedBy(rs.getString(11));
                pv.setLocation(rs.getString(12));
                pv.setOwner(rs.getString(13));
                pv.setConfigResponseId(new Integer(rs.getInt(14)));
                pv.setCpuCount(new Integer(rs.getInt(15)));
                return pv;
            } else {
                PlatformValue pv = new PlatformValue();
                pv.setId(new Integer(rs.getInt(1)));
                pv.setPlatformType(
                        this.getPlatformTypeValue(new PlatformTypePK(new Integer(rs.getInt(2)))));
                pv.setName(rs.getString(3));
                pv.setSortName(rs.getString(4));
                pv.setCertdn(rs.getString(5));
                pv.setFqdn(rs.getString(6));
                pv.setDescription(rs.getString(7));
                pv.setMTime(new Long(rs.getLong(8)));
                pv.setCTime(new Long(rs.getLong(9)));
                // AGENT
                pv.setAgent(this.getAgentValueDirectSQL(new Integer(rs.getInt(10)), conn));
                pv.setModifiedBy(rs.getString(11));
                pv.setLocation(rs.getString(12));
                pv.setOwner(rs.getString(13));
                // config response
                pv.setConfigResponseId(new Integer(rs.getInt(14)));
                pv.setCpuCount(new Integer(rs.getInt(15)));
                // ips
                this.setIpValuesDirectSQL(pv, conn);
                return pv;
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
