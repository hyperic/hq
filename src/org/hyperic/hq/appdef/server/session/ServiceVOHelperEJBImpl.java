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
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.hibernate.Util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;

/**
 *
 * @ejb:bean name="ServiceVOHelper" jndi-name="ejb/appdef/ServiceVOHelper"
 *           local-jndi-name="LocalServiceVOHelper" view-type="local"
 *           type="Stateless"
 * @ejb:util generate="physical"
 */
public class ServiceVOHelperEJBImpl extends AppdefSessionEJB implements
        SessionBean {

    private final String ctx = "org.hyperic.hq.appdef.server.session.ServiceVOHelperEJBImpl";

    private Log log = LogFactory.getLog(ctx);

    private final String SERVICE_SQL = "SELECT ID, SERVER_ID, SERVICE_TYPE_ID,"
            + " SVC_CLUSTER_ID, NAME, SORT_NAME, DESCRIPTION, MTIME, CTIME, "
            + "MODIFIED_BY, LOCATION, OWNER, CONFIG_RESPONSE_ID, "
            + "PARENT_SERVICE_ID, AUTODISCOVERY_ZOMBIE, SERVICE_RT, "
            + "ENDUSER_RT FROM EAM_SERVICE WHERE ID = ?";

    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceValue getServiceValue(Integer pk) throws FinderException,
            NamingException {
        return (ServiceValue) getServiceValueImpl(pk, false);
    }

    /**
     * Get the service light value object
     * 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceLightValue getServiceLightValue(Integer pk)
            throws FinderException, NamingException {
        return (ServiceLightValue) getServiceValueImpl(pk, true);
    }

    /**
     * Get the service light value object from an ejb local
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceLightValue getServiceLightValue(Service ejb)
            throws FinderException, NamingException {
        return getServiceLightValue(ejb.getId());
    }

    /**
     * Get the server value object
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceValue getServiceValue(Service ejb)
            throws NamingException {
        try {
            return getServiceValue(ejb.getId());
        } catch (FinderException e) {
            // This should never happen, as we have already gotten the local obj
            log.error("ServiceLocal primary key " + ejb.getId()
                    + " invalid: ", e);
            return null;
        }
    }

    /**
     * synchronized VO retrieval
     */
    private AppdefResourceValue getServiceValueImpl(Integer servicePK,
                                                    boolean getLightVO)
            throws NamingException {
        return getServiceValueDirectSQL(servicePK, getLightVO);
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AppdefResourceValue getServiceValueDirectSQL(Integer pk,
                                                        boolean getLight) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Util.getConnection();
            ps = conn.prepareStatement(SERVICE_SQL);
            ps.setInt(1, pk.intValue());
            rs = ps.executeQuery();
            rs.next();
            if (getLight) {
                ServiceLightValue slv = new ServiceLightValue();
                slv.setId(new Integer(rs.getInt(1)));
                slv.setServiceType(getServiceTypeValue(
                    new Integer(rs.getInt(3))));
                slv.setName(rs.getString(5));
                slv.setSortName(rs.getString(6));
                slv.setDescription(rs.getString(7));
                slv.setMTime(new Long(rs.getLong(8)));
                slv.setCTime(new Long(rs.getLong(9)));
                slv.setModifiedBy(rs.getString(10));
                slv.setLocation(rs.getString(11));
                slv.setOwner(rs.getString(12));
                slv.setConfigResponseId(new Integer(rs.getInt(13)));
                slv.setParentId(new Integer(rs.getInt(14)));
                slv.setAutodiscoveryZombie(rs.getBoolean(15));
                slv.setServiceRt(rs.getBoolean(16));
                slv.setEndUserRt(rs.getBoolean(17));
                return slv;
            } else {
                ServiceValue sv = new ServiceValue();
                sv.setId(new Integer(rs.getInt(1)));
                sv.setServiceType(getServiceTypeValue(
                    new Integer(rs.getInt(3))));
                sv.setName(rs.getString(5));
                sv.setSortName(rs.getString(6));
                sv.setDescription(rs.getString(7));
                sv.setMTime(new Long(rs.getLong(8)));
                sv.setCTime(new Long(rs.getLong(9)));
                sv.setModifiedBy(rs.getString(10));
                sv.setLocation(rs.getString(11));
                sv.setOwner(rs.getString(12));
                sv.setConfigResponseId(new Integer(rs.getInt(13)));
                sv.setParentId(new Integer(rs.getInt(14)));
                sv.setAutodiscoveryZombie(rs.getBoolean(15));
                sv.setServiceRt(rs.getBoolean(16));
                sv.setEndUserRt(rs.getBoolean(17));
                Server serv =
                    getServerDAO().findById(new Integer(rs.getInt(2)));
                sv.setServer(serv.getServerLightValue());
                return sv;
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(ctx, null, ps, rs);
            Util.endConnection();
        }
    }

    /**
     * Get the server type value object
     * 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceTypeValue getServiceTypeValue(Integer pk)
            throws FinderException, NamingException
    {
        ServiceType ejb = getServiceTypeDAO().findById(pk);
        return getServiceTypeValueImpl(ejb);
    }

    /**
     * Get the server type value object
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ServiceTypeValue getServiceTypeValue(ServiceType ejb)
            throws NamingException {
        return getServiceTypeValueImpl(ejb);
    }

    private ServiceTypeValue getServiceTypeValueImpl(ServiceType ejb)
            throws NamingException {
        return ejb.getServiceTypeValue();
    }

    public void ejbCreate() {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }
}
