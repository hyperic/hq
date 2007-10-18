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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefManagerUtil;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.jdbc.DBUtil;

/**
 * This class is responsible for managing appdef objects in EE
 * @ejb:bean name="AppdefManager"
 *      jndi-name="ejb/appdef/AppdefManager"
 *      local-jndi-name="LocalAppdefManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="SUPPORTS"
 */

public class AppdefManagerEJBImpl
    extends AppdefSessionEJB implements SessionBean {
    
    public static AppdefManagerLocal getOne() {
        try {
            return AppdefManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    

    private static final String OPERABLE_SQL =
    /* ex. "SELECT DISTINCT(server_type_id) FROM eam_server " + */
    " s, EAM_CONFIG_RESPONSE c, EAM_RESOURCE r, EAM_OPERATION o, " +
        "EAM_RESOURCE_TYPE t, EAM_ROLE_OPERATION_MAP ro, " +
        "EAM_ROLE_RESOURCE_GROUP_MAP g, " +
        "EAM_RES_GRP_RES_MAP rg " +
    "WHERE t.name = ? AND o.resource_type_id = t.id AND o.name = ? AND " +
          "operation_id = o.id AND ro.role_id = g.role_id AND " +
          "g.resource_group_id = rg.resource_group_id AND " +
          "rg.resource_id = r.id AND r.resource_type_id = t.id AND " +
          "r.instance_id = s.id AND s.config_response_id = c.id AND " +
          "c.control_response is not null AND " +
          "(r.subject_id = ? OR EXISTS " +
          "(SELECT * FROM EAM_SUBJECT_ROLE_MAP sr " +
           "WHERE sr.role_id = g.role_id AND subject_id = ?))";

    private List findOperableResourceColumn(AuthzSubjectValue subj,
                                            String resourceTable,
                                            String resourceColumn,
                                            String resType,
                                            String operation,
                                            String addCond) {
        List resTypeIds = new ArrayList();
        PreparedStatement stmt = null;
        ResultSet rs = null;        
        try {
            Connection conn = getPlatformDAO().getSession().connection();
            
            StringBuffer sql = new StringBuffer("SELECT DISTINCT(s.")
                .append(resourceColumn)
                .append(") FROM ")
                .append(resourceTable)
                .append(OPERABLE_SQL);
            
            if (addCond != null) {
                sql.append(" AND s.")
                   .append(addCond);
            }
            
            stmt = conn.prepareStatement(sql.toString());
            int i = 1;
            stmt.setString(i++, resType);
            stmt.setString(i++, operation);
            stmt.setInt(i++, subj.getId().intValue());
            stmt.setInt(i++, subj.getId().intValue());
            log.debug("Operable SQL: " + sql);
    
            rs = stmt.executeQuery();
    
            // now build the list
            for (i = 1; rs.next(); i++) {
                resTypeIds.add(new Integer(rs.getInt(1)));
            }
            return resTypeIds;
        } catch (SQLException e) {
            log.error("Error getting scope by SQL", e);
            throw new SystemException("SQL Error getting scope: " +
                                      e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getSessionContext(), null, stmt, rs);
            getPlatformDAO().getSession().disconnect();
        }
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllablePlatformTypes(AuthzSubjectValue subject)
        throws PermissionException {
        List typeIds =
            findOperableResourceColumn(subject,
                                       "EAM_PLATFORM",
                                       "platform_type_id",
                                       AuthzConstants.platformResType,
                                       AuthzConstants.platformOpControlPlatform,
                                       null);
    
        TreeMap platformTypes = new TreeMap();
        for (Iterator it = typeIds.iterator(); it.hasNext(); ) {
            Integer typeId = (Integer) it.next();
            try {
                PlatformType pt = getPlatformTypeDAO().findById(typeId);
                platformTypes.put(pt.getName(), new AppdefEntityTypeID(
                    AppdefEntityConstants.APPDEF_TYPE_PLATFORM, typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return platformTypes;
    }
    
    /**
     * Get controllable platform types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllablePlatformNames(AuthzSubjectValue subject, int tid)
        throws PermissionException {
        List ids =
            findOperableResourceColumn(subject,
                                       "EAM_PLATFORM", "id",
                                       AuthzConstants.platformResType,
                                       AuthzConstants.platformOpControlPlatform,
                                       "platform_type_id=" + tid);
        
        TreeMap platformNames = new TreeMap();
        for (Iterator it = ids.iterator(); it.hasNext(); ) {
            Integer id = (Integer) it.next();
            try {
                Platform plat = getPlatformDAO().findById(id);
                platformNames.put(plat.getName(),
                                  AppdefEntityID.newPlatformID(id.intValue()));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return platformNames;
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllableServerTypes(AuthzSubjectValue subject)
        throws PermissionException {
        List typeIds =
            findOperableResourceColumn(subject,
                                       "EAM_SERVER",
                                       "server_type_id",
                                       AuthzConstants.serverResType,
                                       AuthzConstants.serverOpControlServer,
                                       null);
    
        TreeMap serverTypes = new TreeMap();
        for (Iterator it = typeIds.iterator(); it.hasNext(); ) {
            Integer typeId = (Integer) it.next();
            try {
                ServerType st = getServerTypeDAO().findById(typeId);
                if (!st.getVirtual())
                    serverTypes.put(st.getName(), new AppdefEntityTypeID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVER, typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return serverTypes;
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllableServerNames(AuthzSubjectValue subject, int tid)
        throws PermissionException {
        List ids =
            findOperableResourceColumn(subject, "EAM_SERVER", "id",
                                       AuthzConstants.serverResType,
                                       AuthzConstants.serverOpControlServer,
                                       "server_type_id=" + tid);
    
        TreeMap serverNames = new TreeMap();
        for (Iterator it = ids.iterator(); it.hasNext(); ) {
            Integer id = (Integer) it.next();
            try {
                Server svr = getServerDAO().findById(id);
                serverNames.put(svr.getName(),
                                AppdefEntityID.newServerID(id.intValue()));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return serverNames;
    }

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllableServiceTypes(AuthzSubjectValue subject)
        throws PermissionException {
        List typeIds =
            findOperableResourceColumn(subject,
                                       "EAM_SERVICE",
                                       "service_type_id",
                                       AuthzConstants.serviceResType,
                                       AuthzConstants.serviceOpControlService,
                                       null);
    
        TreeMap serviceTypes = new TreeMap();
        for (Iterator it = typeIds.iterator(); it.hasNext(); ) {
            Integer typeId = (Integer) it.next();
            try {
                ServiceType st = getServiceTypeDAO().findById(typeId);
                serviceTypes.put(st.getName(), new AppdefEntityTypeID(
                    AppdefEntityConstants.APPDEF_TYPE_SERVICE, typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return serviceTypes;
    }

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map getControllableServiceNames(AuthzSubjectValue subject, int tid)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List ids = 
            findOperableResourceColumn(subject, "EAM_SERVICE", "id",
                                       AuthzConstants.serviceResType,
                                       AuthzConstants.serviceOpControlService,
                                       "service_type_id=" + tid);
    
        TreeMap serviceNames = new TreeMap();
        for (Iterator it = ids.iterator(); it.hasNext(); ) {
            Integer id = (Integer) it.next();
            try {
                Service svc = getServiceDAO().findById(id);
                serviceNames.put(svc.getName(),
                                 AppdefEntityID.newServiceID(id.intValue()));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
        
        return serviceNames;
    }

    /** @ejb:create-method */
    public void ejbCreate() throws CreateException {}
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
