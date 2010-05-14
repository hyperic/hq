/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing appdef objects in EE
 * 
 */
@org.springframework.stereotype.Service
@Transactional
public class AppdefManagerImpl implements AppdefManager {

    private final Log log = LogFactory.getLog(AppdefManagerImpl.class.getName());

    private PlatformDAO platformDAO;

    private PlatformTypeDAO platformTypeDAO;

    private ServerDAO serverDAO;

    private ServerTypeDAO serverTypeDAO;

    private ServiceDAO serviceDAO;

    private ServiceTypeDAO serviceTypeDAO;

    private PermissionManager permissionManager;

    private ResourceManager resourceManager;

    private static final String OPERABLE_SQL =
    /* ex. "SELECT DISTINCT(server_type_id) FROM eam_server " + */
    " s, EAM_CONFIG_RESPONSE c, EAM_RESOURCE r, EAM_OPERATION o, " + "EAM_RESOURCE_TYPE t, EAM_ROLE_OPERATION_MAP ro, "
        + "EAM_ROLE_RESOURCE_GROUP_MAP g, " + "EAM_RES_GRP_RES_MAP rg "
        + "WHERE t.name = ? AND o.resource_type_id = t.id AND o.name = ? AND "
        + "operation_id = o.id AND ro.role_id = g.role_id AND " + "g.resource_group_id = rg.resource_group_id AND "
        + "rg.resource_id = r.id AND r.resource_type_id = t.id AND "
        + "r.instance_id = s.id AND s.config_response_id = c.id AND " + "c.control_response is not null AND "
        + "(r.subject_id = ? OR EXISTS " + "(SELECT * FROM EAM_SUBJECT_ROLE_MAP sr "
        + "WHERE sr.role_id = g.role_id AND subject_id = ?))";

    @Autowired
    public AppdefManagerImpl(PlatformDAO platformDAO, PlatformTypeDAO platformTypeDAO, ServerDAO serverDAO,
                             ServerTypeDAO serverTypeDAO, ServiceDAO serviceDAO, ServiceTypeDAO serviceTypeDAO,
                             PermissionManager permissionManager, ResourceManager resourceManager) {

        this.platformDAO = platformDAO;
        this.platformTypeDAO = platformTypeDAO;
        this.serverDAO = serverDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceDAO = serviceDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
    }

    private List<Integer> findOperableResourceColumn(AuthzSubject subj, String resourceTable, String resourceColumn,
                                                     String resType, String operation, String addCond) {
        List<Integer> resTypeIds = new ArrayList<Integer>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = Util.getConnection();

            StringBuffer sql = new StringBuffer("SELECT DISTINCT(s.").append(resourceColumn).append(") FROM ").append(
                resourceTable).append(OPERABLE_SQL);

            if (addCond != null) {
                sql.append(" AND s.").append(addCond);
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
            throw new SystemException("SQL Error getting scope: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(AppdefManagerImpl.class, null, stmt, rs);
        }
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllablePlatformTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceColumn(subject, "EAM_PLATFORM", "platform_type_id",
            AuthzConstants.platformResType, AuthzConstants.platformOpControlPlatform, null);

        TreeMap<String, AppdefEntityID> platformTypes = new TreeMap<String, AppdefEntityID>();
        for (Integer typeId : typeIds) {
            try {
                PlatformType pt = platformTypeDAO.findById(typeId);
                platformTypes.put(pt.getName(), AppdefEntityTypeID.newPlatformID(typeId));
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
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllablePlatformNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceColumn(subject, "EAM_PLATFORM", "id", AuthzConstants.platformResType,
            AuthzConstants.platformOpControlPlatform, "platform_type_id=" + tid);

        TreeMap<String, AppdefEntityID> platformNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Platform plat = platformDAO.findById(id);
                platformNames.put(plat.getName(), AppdefEntityID.newPlatformID(id));
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
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> getControllableServerTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceColumn(subject, "EAM_SERVER", "server_type_id",
            AuthzConstants.serverResType, AuthzConstants.serverOpControlServer, null);

        TreeMap<String, AppdefEntityTypeID> serverTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServerType st = serverTypeDAO.findById(typeId);
                if (!st.isVirtual())
                    serverTypes.put(st.getName(), new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                        typeId));
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
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllableServerNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceColumn(subject, "EAM_SERVER", "id", AuthzConstants.serverResType,
            AuthzConstants.serverOpControlServer, "server_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serverNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Server svr = serverDAO.findById(id);
                serverNames.put(svr.getName(), AppdefEntityID.newServerID(id));
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
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> getControllableServiceTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceColumn(subject, "EAM_SERVICE", "service_type_id",
            AuthzConstants.serviceResType, AuthzConstants.serviceOpControlService, null);

        TreeMap<String, AppdefEntityTypeID> serviceTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServiceType st = serviceTypeDAO.findById(typeId);
                serviceTypes.put(st.getName(),
                    new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, typeId));
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
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllableServiceNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceColumn(subject, "EAM_SERVICE", "id", AuthzConstants.serviceResType,
            AuthzConstants.serviceOpControlService, "service_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serviceNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Service svc = serviceDAO.findById(id);
                serviceNames.put(svc.getName(), AppdefEntityID.newServiceID(id));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }

        return serviceNames;
    }

    /**
     * Change appdef entity owner
     * 
     * 
     */
    public void changeOwner(AuthzSubject who, AppdefResource res, AuthzSubject newOwner) throws PermissionException,
        ServerNotFoundException {
        // check if the caller can modify this server
        permissionManager.checkModifyPermission(who, res.getEntityId());
        // now get its authz resource
        Resource authzRes = res.getResource();
        // change the authz owner
        resourceManager.setResourceOwner(who, authzRes, newOwner);
        // update the modified field in the appdef table -- YUCK
        res.setModifiedBy(who.getName());
    }
}
