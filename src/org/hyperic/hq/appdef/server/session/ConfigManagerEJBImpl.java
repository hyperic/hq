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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Service;
import org.hyperic.hq.appdef.Server;
import org.hyperic.hq.appdef.shared.AIConversionUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigResponseValue;
import org.hyperic.hq.appdef.shared.MiniResourceValue;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocalHome;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServiceLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.math.MathUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an entity bean which is intended to abstract the
 * tables for appdef entities, for fetching configuration information.
 * @ejb:bean name="ConfigManager"
 *      jndi-name="ejb/appdef/ConfigManager"
 *      local-jndi-name="LocalConfigManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class ConfigManagerEJBImpl
    extends AppdefSessionEJB
    implements SessionBean
{
    private static final int MAX_VALIDATION_ERR_LEN = 512;
    private final static String logCtx = ConfigManagerEJBImpl.class.getName();
    protected Log log = LogFactory.getLog(logCtx);

    private static final PermissionManager pm = 
        PermissionManagerFactory.getInstance();

    private PlatformManagerLocalHome platformManagerLocalHome;
    private ServerManagerLocalHome   serverManagerLocalHome;
    private ServiceManagerLocalHome  serviceManagerLocalHome;
    private ResourceValue _bogus; // workaround for Intellij import optimization

    private PlatformManagerLocal getPlatformManagerLocal(){
        try {
            if(this.platformManagerLocalHome == null)
                this.platformManagerLocalHome = 
                    PlatformManagerUtil.getLocalHome();
            return this.platformManagerLocalHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    private ServerManagerLocal getServerManagerLocal(){
        try {
            if(this.serverManagerLocalHome == null)
                this.serverManagerLocalHome = ServerManagerUtil.getLocalHome();
            return this.serverManagerLocalHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    private ServiceManagerLocal getServiceManagerLocal(){
        try {
            if(this.serviceManagerLocalHome == null)
                this.serviceManagerLocalHome = 
                    ServiceManagerUtil.getLocalHome();

            return this.serviceManagerLocalHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    public static final String SQL_CONFIGRESPONSE_PLATFORM
        = "SELECT c.ID, c.PRODUCT_RESPONSE, c.CONTROL_RESPONSE, "
        + "c.MEASUREMENT_RESPONSE, c.AUTOINVENTORY_RESPONSE, "
        + "c.RESPONSE_TIME_RESPONSE, "
        + "c.USERMANAGED, c.VALIDATIONERR "
        + "FROM EAM_CONFIG_RESPONSE c, EAM_PLATFORM p "
        + "WHERE p.CONFIG_RESPONSE_ID = c.ID "
        + "AND p.ID = ?";
    public static final String SQL_CONFIGRESPONSE_SERVER
        = "SELECT c.ID, c.PRODUCT_RESPONSE, c.CONTROL_RESPONSE, "
        + "c.MEASUREMENT_RESPONSE, c.AUTOINVENTORY_RESPONSE, "
        + "c.RESPONSE_TIME_RESPONSE, "
        + "c.USERMANAGED, c.VALIDATIONERR "
        + "FROM EAM_CONFIG_RESPONSE c, EAM_SERVER s "
        + "WHERE s.CONFIG_RESPONSE_ID = c.ID "
        + "AND s.ID = ?";
    public static final String SQL_CONFIGRESPONSE_SERVICE
        = "SELECT c.ID, c.PRODUCT_RESPONSE, c.CONTROL_RESPONSE, "
        + "c.MEASUREMENT_RESPONSE, c.AUTOINVENTORY_RESPONSE, "
        + "c.RESPONSE_TIME_RESPONSE, "
        + "c.USERMANAGED, c.VALIDATIONERR "
        + "FROM EAM_CONFIG_RESPONSE c, EAM_SERVICE s "
        + "WHERE s.CONFIG_RESPONSE_ID = c.ID "
        + "AND s.ID = ?";
    /**
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ConfigResponseValue getConfigResponseValue (AppdefEntityID id)
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = null;

        switch(id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            sql = SQL_CONFIGRESPONSE_PLATFORM;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            sql = SQL_CONFIGRESPONSE_SERVER;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            sql = SQL_CONFIGRESPONSE_SERVICE;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
        default:
            throw new IllegalArgumentException("The passed entity type " +
                                               "does not support config " +
                                               "responses");
        }
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.getId().intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw AppdefEntityNotFoundException.build(id);
            }
            int i=1;
            return new ConfigResponseValue(new Integer(rs.getInt(i++)),
                                           DBUtil.getBlobColumn(rs, i++),
                                           DBUtil.getBlobColumn(rs, i++),
                                           DBUtil.getBlobColumn(rs, i++),
                                           DBUtil.getBlobColumn(rs, i++),
                                           DBUtil.getBlobColumn(rs, i++),
                                           rs.getBoolean(i++),
                                           rs.getString(i++));

        } catch (SQLException e) {
            throw new SystemException("Error looking up config response "
                                         + "for entity: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    // A Map of entityType->entityId->pluginName
    private static Map typeCache = new HashMap();
    static {
        typeCache.put(new Integer(AppdefEntityConstants.APPDEF_TYPE_PLATFORM),
                      new HashMap());
        typeCache.put(new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVER),
                      new HashMap());
        typeCache.put(new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVICE),
                      new HashMap());
        typeCache.put(new Integer(AppdefEntityConstants.APPDEF_TYPE_APPLICATION),
                      new HashMap());
    }
    /**
     * @ejb:interface-method
     */
    public String getPluginName(AppdefEntityID id)
        throws AppdefEntityNotFoundException
    {
        Integer intID = id.getId();
        String pname;
        Map nameMap = (Map) typeCache.get(new Integer(id.getType()));

        synchronized (nameMap) {
            pname = (String) nameMap.get(intID);
            if (pname != null) return pname;
        }

        try {
            switch(id.getType()){
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                PlatformLocal plat = 
                    this.findPlatformByPK(new PlatformPK(intID));
                pname = plat.getPlatformType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server serv = 
                    this.findServerByPK(new ServerPK(intID));
                pname = serv.getServerType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                Service service = findServiceByPK(new ServicePK(intID));
                pname = service.getServiceType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            default:
                throw new IllegalArgumentException("The passed entity type " +
                                                   "does not support config " +
                                                   "responses");
            }
            
            synchronized (nameMap) {
                nameMap.put(intID, pname);
            }
            return pname;

        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get a config response object merged through the hierarchy.
     * All entities are merged with the product's config response, and
     * any entity lower than them in the config stack.  Config responses
     * defining a specific attribute will override the same attribute if
     * it was declared lower in the application stack.  Only entities
     * within the same plugin will be processed, so the most likely
     * situation is a simple service + server + product or server + product
     * merge.
     *
     * Example:  Get the SERVICE MEASUREMENT merged response:
     *              PRODUCT[platform] + MEASUREMENT[platform]
     *              PRODUCT[server] + MEASUREMENT[server] + 
     *              PRODUCT[service] + MEASUREMENT[service]
     *
     *           Get the SERVER PRODUCT merged response:
     *              PRODUCT[platform]
     *              PRODUCT[server]
     *
     *           Get the PLATFORM PRODUCT merged response:
     *              PRODUCT[platform]
     *
     * In addition to the configuration, some inventory properties are also
     * merged in to aid in auto-configuration done by autoinventory.
     *
     * For Servers and Services:
     *   The install path of the server is included
     *
     * For all Resources:
     *   The first non-loopback ip address, fqdn, platform name and type.
     *
     * @param productType One of ProductPlugin.*
     * @param id          An AppdefEntityID of the object to get config for
     *
     * @return the merged ConfigResponse
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ConfigResponse getMergedConfigResponse(AuthzSubjectValue subject,
                                                  String productType,
                                                  AppdefEntityID id,
                                                  boolean required)
        throws AppdefEntityNotFoundException, ConfigFetchException,
               EncodingException, PermissionException
    {
        ConfigResponseValue configValue;
        AppdefEntityID platformId = null, serverId = null, serviceId = null;
        byte[][] responseList; // List of config responses to merge
        ConfigResponse res;
        int responseIdx;
        byte[] data;
        ServerConfigStuff server = null;
        PlatformConfigStuff platform = null;
        boolean origReq = false;
        boolean isServerOrService = false;
        boolean isProductType = productType.equals(ProductPlugin.TYPE_PRODUCT);

        if(id.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM &&
           id.getType() != AppdefEntityConstants.APPDEF_TYPE_SERVER &&
           id.getType() != AppdefEntityConstants.APPDEF_TYPE_SERVICE)
        {
            throw new IllegalArgumentException(id + " doesn't support " +
                                               "config merging");
        }

        // Setup
        responseList = new byte[6][];
        responseIdx  = 0;

        if(id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
            server = getServerStuffForService(id.getId());
            serverId = new AppdefEntityID(AppdefEntityConstants.
                                          APPDEF_TYPE_SERVER,
                                          new Integer(server.id));
            platform = getPlatformStuffForServer(serverId.getId());
            platformId = new AppdefEntityID(AppdefEntityConstants.
                                            APPDEF_TYPE_PLATFORM,
                                            new Integer(platform.id));
            serviceId = id;
            
            origReq = required;
            required = false;
            isServerOrService = true;
        } else if(id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            platform = getPlatformStuffForServer(id.getId());
            platformId = new AppdefEntityID(AppdefEntityConstants.
                                            APPDEF_TYPE_PLATFORM,
                                            new Integer(platform.id));
            serverId = id;
            server = getServerStuffForServer(serverId.getId());
            isServerOrService = true;
        } else {
            // Just the platform
            platformId = id;
            platform = getPlatformStuffForPlatform(platformId.getId());
        } 

        // Load IP address for all resources
        loadPlatformIp(platformId, platform);

        // Platform config
        if (platformId != null) {
            // hardcode required=false for server/service types
            // e.g. unlikely that a platform will have control config
            boolean platformConfigRequired =
                (isServerOrService) ? false : required;

            configValue = this.getConfigResponseValue(platformId);
            data = this.getConfigForType(configValue,
                                         ProductPlugin.TYPE_PRODUCT,
                                         platformId,
                                         platformConfigRequired);
            responseList[responseIdx++] = data;
        
            if(!isProductType) {
                if(productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
                    // Skip merging of response time configuration
                    // since platforms don't have it.
                } else {
                    data = this.getConfigForType(configValue, productType,
                                                 platformId,
                                                 platformConfigRequired);
                    responseList[responseIdx++] = data;
                }
            }
        }
        
        // Server config (if necessary)
        if (serverId != null) {
            configValue = this.getConfigResponseValue(serverId);
            data = this.getConfigForType(configValue,
                                         ProductPlugin.TYPE_PRODUCT,
                                         serverId, required);
            responseList[responseIdx++] = data;

            if(!isProductType) {
                if (productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
                    // Skip merging of response time configuration
                    // since servers don't have it.
                } else {
                    data = this.getConfigForType(configValue, productType,
                                                 serverId, required);
                    responseList[responseIdx++] = data;
                }
            }
        }
                                
        // Service config (if necessary)
        if (serviceId != null) {
            required = origReq;     // Reset the required flag
            configValue  = this.getConfigResponseValue(id);

            data = this.getConfigForType(configValue, 
                                         ProductPlugin.TYPE_PRODUCT,
                                         id, required);
            responseList[responseIdx++] = data;
        
            if(!isProductType){
                data = this.getConfigForType(configValue, productType, id,
                                             required);
                responseList[responseIdx++] = data;
            }
        }

        // Merge everything together
        res = new ConfigResponse();
        for(int i=0; i<responseIdx; i++){
            if (responseList[i] == null || responseList[i].length == 0) {
                continue;
            }

            res.merge(ConfigResponse.decode(responseList[i]), true);
        }

        // Set platform attributes for all resources
        try {
            res.setValue(ProductPlugin.PROP_PLATFORM_NAME,
                         platform.name);
            res.setValue(ProductPlugin.PROP_PLATFORM_FQDN,
                         platform.fqdn);
            res.setValue(ProductPlugin.PROP_PLATFORM_TYPE,
                         platform.typeName);
            res.setValue(ProductPlugin.PROP_PLATFORM_IP,
                         platform.ip);
            res.setValue(ProductPlugin.PROP_PLATFORM_ID,
                         String.valueOf(platform.id));
        } catch(Exception exc){
            this.log.warn("Error setting platform properies: " + exc,
                          exc);
        }

        // Set installpath attribute for server and service types.
        if(isServerOrService) {
            try {
                res.setValue(ProductPlugin.PROP_INSTALLPATH,
                             server.installPath);
            } catch(Exception exc){
                this.log.warn("Error setting installpath property: " + exc, 
                              exc);
            } 
        }

        return res;
    }

    /**
     * Set the config response for an entity/type combination.
     *
     * @param id       ID of the object to set the repsonse fo
     * @param response The response
     * @param type     One of ProductPlugin.TYPE_*
     *
     * @return an array of entities which may be affected by the change
     *         in configuration.  For updates to platform and service configs,
     *         there are no other entities other than the given ID returned.  
     *         If a server is updated, the associated services may require
     *         changes.  The passed entity will always be returned in the
     *         array.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public AppdefEntityID[] setConfigResponse(AuthzSubjectValue subject,
                                              AppdefEntityID id, 
                                              ConfigResponse response,
                                              String type,
                                              boolean sendConfigEvent)
        throws ConfigFetchException, AppdefEntityNotFoundException,
               PermissionException, EncodingException, FinderException {

        ConfigResponseValue cValue = getConfigResponseValue(id);
        byte[] encodedConfig = response.encode();
        
        if(type.equals(ProductPlugin.TYPE_PRODUCT)) {
            cValue.setProductResponse(encodedConfig);
        } else if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            cValue.setMeasurementResponse(encodedConfig);
        } else if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            cValue.setControlResponse(encodedConfig);
        } else if (type.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            cValue.setResponseTimeResponse(encodedConfig);
        } else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
            cValue.setAutoinventoryResponse(encodedConfig);
        } else {
            throw new IllegalArgumentException("Unknown config type: " + type);
        }

        return setConfigResponse(subject, id, cValue, sendConfigEvent);
    }

    /**
     * Clear the validation error string for a config response, indicating
     * that the current config is valid
     * @ejb:interface-method
     */
    public void clearValidationError (AuthzSubjectValue subject,
                                      AppdefEntityID id) {
        setValidationError(subject, id, null);
    }

    private static final String SQL_UPDATE_VERR_PLATFORM
        = "UPDATE EAM_CONFIG_RESPONSE  "
        + "SET VALIDATIONERR = ? WHERE ID = ?";
    private static final String SQL_GET_PLAT_RESP_ID
        = "SELECT c.ID FROM EAM_CONFIG_RESPONSE c, EAM_PLATFORM p"
        + " WHERE c.ID = p.CONFIG_RESPONSE_ID AND p.ID = ?";
    private static final String SQL_UPDATE_VERR_SERVER
        = "UPDATE EAM_CONFIG_RESPONSE  "
        + "SET VALIDATIONERR = ? WHERE ID = ?";
    private static final String SQL_GET_SERVER_RESP_ID
        = "SELECT c.ID FROM EAM_CONFIG_RESPONSE c, EAM_SERVER s"
        + "  WHERE c.ID = s.CONFIG_RESPONSE_ID AND s.ID = ?";
    private static final String SQL_UPDATE_VERR_SERVICE
        = "UPDATE EAM_CONFIG_RESPONSE  "
        + "SET VALIDATIONERR = ? WHERE ID = ?";
    private static final String SQL_GET_SERVICE_RESP_ID
        = "SELECT c.ID FROM EAM_CONFIG_RESPONSE c, EAM_SERVICE s"
        + "  WHERE c.ID = s.CONFIG_RESPONSE_ID AND s.ID = ?";
    /**
     * Update the validation error string for a config response
     * @param validationError The error string that occured during validation.
     * If this is null, that means that no error occurred and the config is
     * valid. 
     * @ejb:interface-method
     */
    public void setValidationError (AuthzSubjectValue subject,
                                    AppdefEntityID id,
                                    String validationError) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String idSql = null;
        String updateSql = null;
        switch(id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            updateSql = SQL_UPDATE_VERR_PLATFORM;
            idSql = SQL_GET_PLAT_RESP_ID;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            updateSql = SQL_UPDATE_VERR_SERVER;
            idSql = SQL_GET_SERVER_RESP_ID;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            updateSql = SQL_UPDATE_VERR_SERVICE;
            idSql = SQL_GET_SERVICE_RESP_ID;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
        default:
            throw new IllegalArgumentException("The passed entity type " +
                                               "does not support config " +
                                               "responses");
        }
        try {
            conn = getDBConn();
            // get the id of the config resp
            ps = conn.prepareStatement(idSql);
            ps.setInt(1, id.getID());
            rs = ps.executeQuery();
            rs.next();
            int respId = rs.getInt(1);
            rs.close();
            // now update it
            ps = conn.prepareStatement(updateSql);
            if (validationError != null) {
                if (validationError.length() > MAX_VALIDATION_ERR_LEN) {
                    validationError =
                        validationError.substring(0, MAX_VALIDATION_ERR_LEN-3) +
                        "...";  
                }
                ps.setString(1, validationError);
            } else {
                ps.setNull(1, java.sql.Types.VARCHAR);
            }
            ps.setInt(2, respId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new SystemException("Error setting config response's "
                                         + "validation error string for id: "
                                         + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    /**
     * Set all configs for a resource at once.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public AppdefEntityID[] setConfigResponse(AuthzSubjectValue subject,
                                              AppdefEntityID id, 
                                              ConfigResponseValue newConfig,
                                              boolean sendConfigEvent) 
        throws FinderException, PermissionException, 
               AppdefEntityNotFoundException {

        ConfigResponseValue existing = this.getConfigResponseValue(id);
        boolean configWasUpdated = false;
        boolean appdefCreated = false;
        byte[] storedConf, newConf;

        storedConf = existing.getProductResponse();
        newConf = newConfig.getProductResponse();
        if(!Arrays.equals(storedConf, newConf)) { 
            existing.setProductResponse(newConf);
            configWasUpdated = true;
        }

        storedConf = existing.getMeasurementResponse();
        newConf = newConfig.getMeasurementResponse();
        if(!Arrays.equals(storedConf, newConf)) { 
            existing.setMeasurementResponse(newConf);
            configWasUpdated = true;
        }

        storedConf = existing.getControlResponse();
        newConf = newConfig.getControlResponse();
        if(!Arrays.equals(storedConf, newConf)) { 
            existing.setControlResponse(newConf);
            configWasUpdated = true;
        }

        storedConf = existing.getResponseTimeResponse();
        newConf = newConfig.getResponseTimeResponse();
        if(!Arrays.equals(storedConf, newConf)) { 
            existing.setResponseTimeResponse(newConf);
            configWasUpdated = true;
        }

        storedConf = existing.getAutoinventoryResponse();
        newConf = newConfig.getAutoinventoryResponse();
        if(!Arrays.equals(storedConf, newConf)) { 
            existing.setAutoinventoryResponse(newConf);
            configWasUpdated = true;
        }

        boolean um = newConfig.getUserManaged();
        if (existing.getUserManaged() != um) {
            existing.setUserManaged(um);
            configWasUpdated = true;
        }

        List res = new ArrayList();
        res.add(id);

        // - ADDING SERVICES TO AFFECTED ENTRIES IS DISABLED
        //
        // since this op requires a new transaction, the only case for 
        // rollback is if we're unable to assemble the list of affected
        // entities
        //try {
        //    if(id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER){
        //        ServiceManagerLocal sMan;
        //        List vals; 
        //
        //        sMan = this.getServiceManagerLocal();
        //        vals = sMan.getServicesByServer(subject, id.getId(),
        //                                        PageControl.PAGE_ALL);
        //        
        //        for(Iterator i=vals.iterator(); i.hasNext(); ){
        //            res.add(((AppdefResourceValue)i.next()).getEntityId());
        //        }
        //    }
        //} catch (AppdefEntityNotFoundException e) {
        //    throw e;
        //} catch (PermissionException e) {
        //    throw e;
        //}

        AppdefEntityID[] affectedEntities = 
                (AppdefEntityID[])res.toArray(new AppdefEntityID[res.size()]);

        // Update the config response
        if (configWasUpdated) {
            ConfigResponseDB cLocal
                = getConfigResponseLocalHome()
                .findByPrimaryKey(existing.getPrimaryKey());
            cLocal.setConfigResponseValue(existing);
        
            if (appdefCreated) {
                // Creation event
                AIConversionUtil.sendCreateEvent(subject, id);
            } else if (sendConfigEvent) {
                AIConversionUtil.sendNewConfigEvent(subject, id);
            }
        }

        return affectedEntities;
    }

    /** Update the appdef entities based on TypeInfo
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void updateAppdefEntities(String pluginName,
                                     TypeInfo[] entities)
        throws FinderException, RemoveException, CreateException
    {
        ArrayList platforms = new ArrayList();
        ArrayList servers   = new ArrayList();
        ArrayList services  = new ArrayList();

        // Organize the entity infos first
        for (int i = 0; i < entities.length; i++) {
            TypeInfo ei = entities[i];

            switch (ei.getType()) {
                case TypeInfo.TYPE_PLATFORM:
                    platforms.add(ei);
                    break;
                case TypeInfo.TYPE_SERVER:
                    servers.add(ei);
                    break;
                case TypeInfo.TYPE_SERVICE:
                    services.add(ei);
                    break;
                default:
                    break;
            }
        }

        // Update platforms
        if (platforms.size() > 0) {
            this.getPlatformManagerLocal().updatePlatformTypes(
                pluginName, (PlatformTypeInfo[])
                platforms.toArray(new PlatformTypeInfo[0]));
        }
        
        // Update servers
        if (servers.size() > 0) {
            this.getServerManagerLocal().updateServerTypes(pluginName,
                (ServerTypeInfo[]) servers.toArray(new ServerTypeInfo[0]));
        }
        
        // Update services
        if (services.size() > 0) {
            this.getServiceManagerLocal().updateServiceTypes(pluginName,
                (ServiceTypeInfo[]) services.toArray(new ServiceTypeInfo[0]));
        }
    }

    private byte[] getConfigForType(ConfigResponseValue val,
                                    String productType, 
                                    AppdefEntityID id,
                                    boolean fail)
        throws ConfigFetchException
    {
        byte[] res;

        if(productType.equals(ProductPlugin.TYPE_PRODUCT)){
            res = val.getProductResponse();
        } else if(productType.equals(ProductPlugin.TYPE_CONTROL)){
            res = val.getControlResponse();
        } else if(productType.equals(ProductPlugin.TYPE_MEASUREMENT)){
            res = val.getMeasurementResponse();
        } else if(productType.equals(ProductPlugin.TYPE_AUTOINVENTORY)){
            res = val.getAutoinventoryResponse();
        } else if(productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)){
            res = val.getResponseTimeResponse();
        } else {
            throw new IllegalArgumentException("Unknown product type");
        }

        if((res == null || res.length == 0) && fail) {
            throw new ConfigFetchException(productType, id);
        }
        return res;
    }

    public void ejbCreate() throws CreateException {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}

    private Connection getDBConn() throws SQLException {
        try {
            return DBUtil.getConnByContext(this.getInitialContext(), 
                                            HQConstants.DATASOURCE);
        } catch(NamingException exc){
            throw new SystemException("Unable to get database context: " +
                                         exc.getMessage(), exc);
        }
    }

    private static final String SQL_SERVERBYSERVICE
        = "SELECT s.ID, s.INSTALLPATH FROM EAM_SERVER s, EAM_SERVICE svc "
        + "WHERE svc.SERVER_ID = s.ID AND svc.ID = ?";
    private ServerConfigStuff getServerStuffForService(Integer id) 
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = SQL_SERVERBYSERVICE;
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ServiceNotFoundException(id);
            }
            return new ServerConfigStuff(rs.getInt(1),
                                         rs.getString(2));
        } catch (SQLException e) {
            throw new SystemException("Error looking up config response "
                                         + "for service: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }
    
    private static final String SQL_SERVER
        = "SELECT s.ID, s.INSTALLPATH FROM EAM_SERVER s "
        + "WHERE s.ID = ?";
    private ServerConfigStuff getServerStuffForServer(Integer id) 
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = SQL_SERVER;
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ServerNotFoundException(id);
            }
            return new ServerConfigStuff(rs.getInt(1),
                                         rs.getString(2));
        } catch (SQLException e) {
            throw new SystemException("Error looking up config response "
                                         + "for server: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    private static final String SQL_PLATFORMBYSERVER =
        "SELECT p.ID, p.NAME, p.FQDN, pt.NAME FROM EAM_PLATFORM p, " +
        "EAM_PLATFORM_TYPE pt, EAM_SERVER s WHERE s.PLATFORM_ID = p.ID " +
        "AND p.PLATFORM_TYPE_ID = pt.id AND s.ID = ?";
    private PlatformConfigStuff getPlatformStuffForServer(Integer id) 
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = SQL_PLATFORMBYSERVER;
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ServiceNotFoundException(id);
            }
            return new PlatformConfigStuff(rs.getInt(1),
                                           rs.getString(2),
                                           rs.getString(3),
                                           rs.getString(4));
        } catch (SQLException e) {
            throw new SystemException("Error looking up config response " +
                                      "for server: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    private static final String SQL_PLATFORM =
        "SELECT p.ID, p.NAME, p.FQDN, pt.NAME FROM EAM_PLATFORM p, " +
        "EAM_PLATFORM_TYPE pt WHERE p.PLATFORM_TYPE_ID = pt.id AND " +
        "p.ID = ?";
    private PlatformConfigStuff getPlatformStuffForPlatform(Integer id) 
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = SQL_PLATFORM;
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new PlatformNotFoundException(id);
            }
            return new PlatformConfigStuff(rs.getInt(1),
                                           rs.getString(2),
                                           rs.getString(3),
                                           rs.getString(4));
        } catch (SQLException e) {
            throw new SystemException("Error looking up config response " +
                                      "for server: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    private static final String SQL_PLATFORM_IPS =
        "SELECT address FROM EAM_IP WHERE address != '127.0.0.1' " +
        "AND platform_id = ?";
    private static final String SQL_PLATFORM_IPS_BY_SERVER =
        "SELECT address FROM EAM_IP ip, EAM_SERVER s, EAM_PLATFORM p " +
        "WHERE address != '127.0.0.1' AND s.platform_id = p.id AND " +
        "ip.platform_id = p.id AND s.id = ?";
    private void  loadPlatformIp(AppdefEntityID id, PlatformConfigStuff platform) 
        throws AppdefEntityNotFoundException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql;

        if(id.getType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            sql = SQL_PLATFORM_IPS;
        } else if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            sql = SQL_PLATFORM_IPS_BY_SERVER;
        } else {
            // Not gonna happen
            throw new IllegalArgumentException("Platform IPs cannot be " +
                                               "looked up by type " +
                                               id.getType());
        }

        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id.getId().intValue());
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new PlatformNotFoundException(id);
            }
            
            // Just get the first IP
            platform.ip = rs.getString(1);

        } catch (SQLException e) {
            throw new SystemException("Error looking up config response " +
                                      "for server: " + id + ": " + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
    }

    private static final String SQL_UNCONF4METRICS
        = "SELECT res.ID AS resId, s.ID, st.NAME AS typeName, s.NAME, s.CTIME "
        + "FROM EAM_SERVER s, EAM_SERVER_TYPE st, EAM_RESOURCE res, "
        + "EAM_CONFIG_RESPONSE c " + PermissionManager.AUTHZ_FROM
        + "WHERE s.SERVER_TYPE_ID = st.ID "
        + "AND res.INSTANCE_ID = s.ID "
        + "AND res.RESOURCE_TYPE_ID = " + AuthzConstants.authzServer + " "
        + "AND res.ID NOT IN ( %%IGNORELIST%% ) "
        + "AND s.CONFIG_RESPONSE_ID = c.ID "
        + "AND (c.PRODUCT_RESPONSE IS NULL "
        +      "OR c.MEASUREMENT_RESPONSE IS NULL) ";
    /**
     * @param ignoreList a List of Integers representing the resource ids
     * (that is EAM_RESOURCE.ID) of servers that should NOT be included
     * in the results.
     * @return a List of MiniResourceValue objects representing the
     * unconfigured servers in HQ (adjusted according to the pagecontrol
     * of course)
     * @ejb:interface-method
     */
    public PageList getServersNotConfiguredForMetrics
        (AuthzSubjectValue subject, PageControl pc, List ignoreList) {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql 
            = SQL_UNCONF4METRICS
            + pm.getSQLWhere(subject.getId(), "s.ID");
        PageList results = new PageList();
        int seekCount, i, col;
        if (ignoreList == null ) ignoreList = new ArrayList();
        if (ignoreList.size() == 0) ignoreList.add(MathUtil.NEGATIVE_ONE);
        // we could cache these substitutions for a li'l performance boost
        sql = StringUtil.replace(sql, "%%IGNORELIST%%", 
                                 StringUtil.listToString(ignoreList));
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            pm.prepareSQL(ps, 1, subject.getId(),
                          AuthzConstants.authzServer,
                          AuthzConstants.perm_modifyServer);
            rs = ps.executeQuery();
            seekCount = DBUtil.seek(rs, pc);
            int pageSize = pc.getPagesize();
            boolean isUnlimited = (pageSize == PageControl.SIZE_UNLIMITED);
            for (i=0; (isUnlimited || i<pageSize) && rs.next(); i++) {
                col = 1;
                results.add(new MiniResourceValue(rs.getInt(col++),
                                                  rs.getInt(col++),
                                       AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                                  rs.getString(col++),
                                                  rs.getString(col++),
                                                  rs.getLong(col++)));
            }
            int totalSize = DBUtil.countRows(seekCount+i, rs, conn);
            results.setTotalSize(totalSize);

        } catch (SQLException e) {
            throw new SystemException("Error looking up resources not "
                                         + "configured for metrics: " 
                                         + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
        return results;
    }

    private static final String SQL_SERVERS_WITH_INVALID_CONFIG
        = "SELECT res.ID AS resId, s.ID, st.NAME as TNAME, s.NAME, s.CTIME, " +
        "c.VALIDATIONERR "
        + "FROM EAM_SERVER s, EAM_SERVER_TYPE st, EAM_RESOURCE res, "
        + "EAM_CONFIG_RESPONSE c " + PermissionManager.AUTHZ_FROM
        + "WHERE s.SERVER_TYPE_ID = st.ID "
        + "AND res.INSTANCE_ID = s.ID "
        + "AND res.RESOURCE_TYPE_ID = " + AuthzConstants.authzServer + " "
        + "AND res.ID NOT IN ( %%IGNORELIST%% ) "
        + "AND s.CONFIG_RESPONSE_ID = c.ID "
        + "AND c.VALIDATIONERR IS NOT NULL ";
    /**
     * @param ignoreList a List of Integers representing the resource ids
     * (that is EAM_RESOURCE.ID) of servers that should NOT be included
     * in the results.
     * @return a List of MiniResourceValue objects representing the
     * unconfigured servers in HQ (adjusted according to the pagecontrol
     * of course).  The "notes" attribute of each MiniResourceValue will
     * contain the validation error string.
     * @ejb:interface-method
     */
    public PageList getServersWithInvalidConfig
        (AuthzSubjectValue subject, PageControl pc, List ignoreList) {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql 
            = SQL_SERVERS_WITH_INVALID_CONFIG
            + pm.getSQLWhere(subject.getId(), "s.ID");
        PageList results = new PageList();
        int seekCount, i, col;
        if (ignoreList == null ) ignoreList = new ArrayList();
        if (ignoreList.size() == 0) ignoreList.add(MathUtil.NEGATIVE_ONE);
        // we could cache these substitutions for a li'l performance boost
        sql = StringUtil.replace(sql, "%%IGNORELIST%%", 
                                 StringUtil.listToString(ignoreList));
        try {
            conn = getDBConn();
            ps = conn.prepareStatement(sql);
            pm.prepareSQL(ps, 1, subject.getId(),
                          AuthzConstants.authzServer,
                          AuthzConstants.perm_modifyServer);
            rs = ps.executeQuery();
            seekCount = DBUtil.seek(rs, pc);
            int pageSize = pc.getPagesize();
            boolean isUnlimited = (pageSize == PageControl.SIZE_UNLIMITED);
            MiniResourceValue mrv;
            for (i=0; (isUnlimited || i<pageSize) && rs.next(); i++) {
                col = 1;
                mrv = new MiniResourceValue(rs.getInt(col++),
                                            rs.getInt(col++),
                                       AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                            rs.getString(col++),
                                            rs.getString(col++),
                                            rs.getLong(col++),
                                            rs.getString(col++));
                results.add(mrv);
            }
            int totalSize = DBUtil.countRows(seekCount+i, rs, conn);
            results.setTotalSize(totalSize);

        } catch (SQLException e) {
            throw new SystemException("Error looking up resources with "
                                         + "invalid configs: " 
                                         + e, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, ps, rs);
        }
        return results;
    }
    
    // Utility classes used by getMergedConfig
    class ServerConfigStuff {
        public int id;
        public String installPath;
        public ServerConfigStuff(int id, String installPath) {
            this.id = id;
            this.installPath = installPath;
        }
    }

    class PlatformConfigStuff {
        public int id;
        public String ip;
        public String name;
        public String fqdn;
        public String typeName;
            
        public PlatformConfigStuff(int id, String name, String fqdn, 
                                   String typeName) {
            this.id = id;
            this.name = name;
            this.fqdn = fqdn;
            this.typeName = typeName;
        }
    }
}
