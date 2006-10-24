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
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformVOHelperLocal;
import org.hyperic.hq.appdef.shared.PlatformVOHelperUtil;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerVOHelperLocal;
import org.hyperic.hq.appdef.shared.ServerVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServicePK;
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
        ServerValue server = svrHelper.getServerValue(new ServerPK(id));
        checkViewPermission(subject, server.getEntityId());
        return server;
    }

    private ServiceValue getServiceValue(AuthzSubjectValue subject, Integer id)
            throws CreateException, NamingException, FinderException,
            PermissionException {
        if (svcHelper == null)
            svcHelper = ServiceVOHelperUtil.getLocalHome().create();
        ServiceValue service = svcHelper.getServiceValue(new ServicePK(id));
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
     * Find virtual platforms in a VM Process
     * @param subject
     * @param vmId
     * @return a list of virtual platform values
     * @throws PlatformNotFoundException
     * @throws PermissionException
     * @ejb:interface-method
     */
    public List findVirtualPlatformsByVM(AuthzSubjectValue subject, Integer vmId)
        throws PlatformNotFoundException, PermissionException {
        Connection conn      = null;
        PreparedStatement ps = null;
        ResultSet rs         = null;
        String sql = "SELECT id FROM " + PLATFORM_VIEW + " WHERE process_id = ?";
    
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            
            ps.setInt(1, vmId.intValue());
            rs = ps.executeQuery();
    
            ArrayList platformList = new ArrayList();
            try {
                while (rs.next()) {
                    Integer id = new Integer(rs.getInt(1));
                    try {
                        platformList.add(getPlatformValue(subject, id));
                    } catch (FinderException e) {
                        // continue
                    }
                }
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (CreateException e) {
                throw new SystemException(e);
            }
            return platformList;
        } catch (SQLException e) {
            throw new SystemException("Error looking up virtual services by " +
                                      "process id " + vmId + ":" + e, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
    }

    /**
     * Find virtual servers in a VM Process
     * @param subject
     * @param vmId
     * @return a list of virtual server values
     * @throws ServerNotFoundException
     * @throws PermissionException
     * @ejb:interface-method
     */
    public List findVirtualServersByVM(AuthzSubjectValue subject, Integer vmId)
        throws ServerNotFoundException, PermissionException {
        Connection conn      = null;
        PreparedStatement ps = null;
        ResultSet rs         = null;
        String sql = "SELECT id FROM " + SERVER_VIEW + " WHERE process_id = ?";
    
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            
            ps.setInt(1, vmId.intValue());
            rs = ps.executeQuery();
    
            ArrayList serverList = new ArrayList();
            try {
                while (rs.next()) {
                    Integer id = new Integer(rs.getInt(1));
                    try {
                        serverList.add(getServerValue(subject, id));
                    } catch (FinderException e) {
                        // continue
                    }
                }
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (CreateException e) {
                throw new SystemException(e);
            }
            return serverList;
        } catch (SQLException e) {
            throw new SystemException("Error looking up virtual services by " +
                                      "process id " + vmId + ":" + e, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
    }

    /**
     * Find virtual services in a VM Process
     * @param subject
     * @param vmId
     * @return a list of virtual service values
     * @throws ServiceNotFoundException
     * @throws PermissionException
     * @ejb:interface-method
     */
    public List findVirtualServicesByVM(AuthzSubjectValue subject, Integer vmId)
        throws ServiceNotFoundException, PermissionException {
        Connection conn      = null;
        PreparedStatement ps = null;
        ResultSet rs         = null;
        String sql = "SELECT id FROM " + SERVICE_VIEW + " WHERE process_id = ?";
    
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            
            ps.setInt(1, vmId.intValue());
            rs = ps.executeQuery();
    
            ArrayList serviceList = new ArrayList();
            try {
                while (rs.next()) {
                    Integer id = new Integer(rs.getInt(1));
                    try {
                        serviceList.add(getServiceValue(subject, id));
                    } catch (FinderException e) {
                        // continue
                    }
                }
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (CreateException e) {
                throw new SystemException(e);
            }
            return serviceList;
        } catch (SQLException e) {
            throw new SystemException("Error looking up virtual services by " +
                                      "process id " + vmId + ":" + e, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
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
    
    /**
     * Associate an array of entities to a VM
     * @throws FinderException 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void associateEntities(AuthzSubjectValue subj,
                                  Integer processId,
                                  AppdefEntityID[] aeids)
        throws FinderException {
        Connection conn      = null;
        PreparedStatement ps = null;
        
        try {
            ResourceManagerLocal resMan =
                ResourceManagerUtil.getLocalHome().create();
            
            String sql = "INSERT INTO " + VIRTUAL_TABLE +
                         " (resource_id, process_id) VALUES (?, ?)";
            
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            
            for (int i = 0; i < aeids.length; i++) {
               String typeStr =
                   AppdefUtil.appdefTypeIdToAuthzTypeStr(aeids[i].getType());
               ResourceValue res =
                   resMan.findResourceByTypeAndInstanceId(typeStr,
                                                          aeids[i].getId());
               
               ps.setInt(1, res.getId().intValue());
               ps.setInt(2, processId.intValue());
               
               ps.execute();
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (SQLException e) {
            throw new SystemException("Error associating virtual resources to "
                                      + "VM process id " + processId + ":" + e,
                                      e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, null);
        }
    }

    /**
     * Associate an array of entities to a VM
     * @throws FinderException 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void associateToPhysical(AuthzSubjectValue subj,
                                    Integer physicalId,
                                    AppdefEntityID aeid)
        throws FinderException {
        Connection conn = null;
        PreparedStatement ps = null, is = null;
        ResultSet rs = null;
        
        try {
            StringBuffer finder = new StringBuffer("SELECT resource_id FROM ");
            switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                finder.append(PLATFORM_VIEW);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                finder.append(SERVER_VIEW);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                finder.append(SERVICE_VIEW);
                break;
            default:
                throw new InvalidAppdefTypeException(
                    "Cannot associate appdefType " + aeid.getType() +
                    " to physical resource");
            }
            finder.append(" WHERE id = ?");
            
            conn = getDBConn();
            ps = conn.prepareStatement(finder.toString());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int resId = rs.getInt(1);
                
                String update = "UPDATE " + VIRTUAL_TABLE +
                                " SET physical_id = ? WHERE resource_id = ?";
                
                is = conn.prepareStatement(update);
                
                is.setInt(1, physicalId.intValue());
                is.setInt(2, resId);
                is.execute();
            }
            else {
                throw new FinderException(aeid.toString() +
                    " is not registered as a virtual resource");
            }
        } catch (SQLException e) {
            throw new SystemException("Error associating virtual resource " +
                                      aeid + " to physical id " + physicalId +
                                      ":" + e,e);
        } finally {
            DBUtil.closeJDBCObjects(log, null, ps, rs);
            DBUtil.closeJDBCObjects(log, conn, is, null);
        }
    }

    public void ejbCreate() throws CreateException { }    
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
