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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformVOHelperLocal;
import org.hyperic.hq.appdef.shared.PlatformVOHelperUtil;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerVOHelperLocal;
import org.hyperic.hq.appdef.shared.ServerVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceVOHelperLocal;
import org.hyperic.hq.appdef.shared.ServiceVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 * @ejb:bean name="VirtualManager"
 *      jndi-name="ejb/appdef/VirtualManager"
 *      local-jndi-name="LocalVirtualManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="SUPPORTS"
 */
public class VirtualManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {

    private Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl");

    private final String VIRTUAL_TABLE = "EAM_VIRTUAL";
    private final String PLATFORM_VIEW = "EAM_VIRT_PLAT_VIEW";
    private final String SERVER_VIEW   = "EAM_VIRT_SVR_VIEW";
    private final String SERVICE_VIEW  = "EAM_VIRT_SVC_VIEW";
    
    private ServerVOHelperLocal   svrHelper  = null;
    private ServiceVOHelperLocal  svcHelper  = null;
    
    private PlatformValue getPlatformValue(AuthzSubjectValue subject, Integer id)
        throws CreateException, NamingException, FinderException,
               PermissionException 
    {
        Platform p = DAOFactory.getDAOFactory().getPlatformDAO().findById(id);
        
        PlatformValue platform = p.getPlatformValue();
        checkViewPermission(subject, platform.getEntityId());
        return platform;
    }

    private ServerValue getServerValue(AuthzSubjectValue subject, Integer id)
        throws CreateException, NamingException, FinderException,
               PermissionException {
        if (svrHelper == null)
            svrHelper = ServerVOHelperUtil.getLocalHome().create();
        ServerValue server = svrHelper.getServerValue(id);
        checkViewPermission(subject, server.getEntityId());
        return server;
    }

    private ServiceValue getServiceValue(AuthzSubjectValue subject, Integer id)
            throws CreateException, NamingException, FinderException,
            PermissionException {
        if (svcHelper == null)
            svcHelper = ServiceVOHelperUtil.getLocalHome().create();
        ServiceValue service = svcHelper.getServiceValue(id);
        checkViewPermission(subject, service.getEntityId());
        return service;
    }

    private Connection getDBConn() throws SQLException {
        try {
            return DBUtil.getConnByContext(this.getInitialContext(), 
                                            HQConstants.DATASOURCE);
        } catch(NamingException exc){
            throw new SystemException("Unable to get database context: " +
                                         exc.getMessage(), exc);
        }
    }

    /**
     * Find virtual resources whose parent is the given physical ID
     * @param subject
     * @param aeid
     * @return list of virtual resource values
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @ejb:interface-method
     */
    public List findVirtualResourcesByPhysical(AuthzSubjectValue subject,
                                               AppdefEntityID aeid)
        throws AppdefEntityNotFoundException, PermissionException {
        Connection conn      = null;
        PreparedStatement ps = null;
        ResultSet rs         = null;
        StringBuffer sql = new StringBuffer("SELECT id FROM ");
        
        switch (aeid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            sql.append(PLATFORM_VIEW);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            sql.append(SERVER_VIEW);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            sql.append(SERVICE_VIEW);
            break;
        default:
            throw new InvalidAppdefTypeException(
                "Appdef Entity Type: " + aeid.getType() +
                " does not support virtual resources");
        }
        
        sql.append(" WHERE physical_id = ?");

        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql.toString());
            
            ps.setInt(1, aeid.getID());
            rs = ps.executeQuery();

            ArrayList resourcesList = new ArrayList();
            while (rs.next()) {
                Integer id = new Integer(rs.getInt(1));
                
                try {
                    switch (aeid.getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                        resourcesList.add(getPlatformValue(subject, id));
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        resourcesList.add(getServerValue(subject, id));
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        resourcesList.add(getServiceValue(subject, id));
                        break;
                    }
                } catch (NamingException e) {
                    throw new SystemException(e);
                } catch (CreateException e) {
                    throw new SystemException(e);
                } catch (FinderException e) {
                    // continue
                }
            }
            return resourcesList;
        } catch (SQLException e) {
            throw new SystemException("Error looking up virtual resources by " +
                                      "physical id " + aeid + ":" + e, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
    }
    
    public void ejbCreate() throws CreateException { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
