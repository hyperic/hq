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
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.ConfigResponseDAO;
import org.hyperic.hq.dao.ServerDAO;
import org.hyperic.hq.dao.ServiceDAO;
import org.hyperic.hq.dao.PlatformDAO;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.autoinventory.AICompare;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

/**
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

    private PlatformManagerLocal getPlatformManagerLocal(){
        return PlatformManagerEJBImpl.getOne();
    }

    private ServerManagerLocal getServerManagerLocal(){
        return ServerManagerEJBImpl.getOne();
    }

    private ServiceManagerLocal getServiceManagerLocal(){
        return ServiceManagerEJBImpl.getOne();
    }

    /**
     *
     * Get the ConfigResponse for the given ID, creating it if it does not
     * already exist.
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ConfigResponseDB getConfigResponse(AppdefEntityID id)
        throws AppdefEntityNotFoundException {

        ConfigResponseDAO dao =
            DAOFactory.getDAOFactory().getConfigResponseDAO();
        ConfigResponseDB config;

        switch(id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            config = dao.findByPlatformId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            config = dao.findByServerId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            config = dao.findByServiceId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
        default:
            throw new IllegalArgumentException("The passed entity type " +
                                               "does not support config " +
                                               "responses");
        }

        return config;
    }

    /**
     * @ejb:interface-method
     */
    public String getPluginName(AppdefEntityID id)
        throws AppdefEntityNotFoundException
    {
        Integer intID = id.getId();
        String pname;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                Platform plat =
                    getPlatformManagerLocal().findPlatformById(intID);
                pname = plat.getPlatformType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server serv =
                    getServerManagerLocal().findServerById(intID);
                pname = serv.getServerType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                Service service =
                    getServiceManagerLocal().findServiceById(intID);
                pname = service.getServiceType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            default:
                throw new IllegalArgumentException("The passed entity type " +
                    "does not support config " +
                    "responses");
        }

        return pname;
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
     * @ejb:transaction type="Required"
     */
    public ConfigResponse getMergedConfigResponse(AuthzSubjectValue subject,
                                                  String productType,
                                                  AppdefEntityID id,
                                                  boolean required)
        throws AppdefEntityNotFoundException, ConfigFetchException,
               EncodingException, PermissionException
    {
        ConfigResponseDB configValue;
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

        // Platform config
        if (platformId != null) {
            // hardcode required=false for server/service types
            // e.g. unlikely that a platform will have control config
            boolean platformConfigRequired =
                (isServerOrService) ? false : required;

            configValue = getConfigResponse(platformId);
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
            configValue = getConfigResponse(serverId);
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
            configValue  = this.getConfigResponse(id);

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
     * Clear the validation error string for a config response, indicating
     * that the current config is valid
     * @ejb:interface-method
     */
    public void clearValidationError(AuthzSubjectValue subject,
                                     AppdefEntityID id) {
        setValidationError(subject, id, null);
    }

    /**
     * Update the validation error string for a config response
     * @param validationError The error string that occured during validation.
     * If this is null, that means that no error occurred and the config is
     * valid. 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void setValidationError(AuthzSubjectValue subject,
                                   AppdefEntityID id,
                                   String validationError) {

        ConfigResponseDAO dao =
            DAOFactory.getDAOFactory().getConfigResponseDAO();
        ConfigResponseDB config;

        switch(id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            config = dao.findByPlatformId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            config = dao.findByServerId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            config = dao.findByServiceId(id.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
        default:
            throw new IllegalArgumentException("The passed entity type " +
                                               "does not support config " +
                                               "responses");
        }

        if (validationError != null) {
            if (validationError.length() > MAX_VALIDATION_ERR_LEN) {
                validationError =
                    validationError.substring(0, MAX_VALIDATION_ERR_LEN-3) +
                        "...";
            }
        }
        config.setValidationError(validationError);
    }

    /**
     * Method to merge configs, maintaining any existing values that are not
     * present in the AI config (e.g. log/config track enablement)
     *
     * @param existingBytes The existing configuration
     * @param newBytes The new configuration
     * @param overwrite TODO
     * @return The newly merged configuration
     */
    private static byte[] mergeConfig(byte[] existingBytes, byte[] newBytes,
                                      boolean overwrite)
    {
        if ((existingBytes == null) || (existingBytes.length == 0)) {
            return newBytes;
        }

        if ((newBytes == null) || (newBytes.length == 0)) {
            //likely a manually created platform service where
            //inventory-properties are auto-discovered but config
            //is left unchanged.
            return existingBytes;
        }
        
        try {
            ConfigResponse existingConfig =
                ConfigResponse.decode(existingBytes);
            ConfigResponse newConfig =
                ConfigResponse.decode(newBytes);
            existingConfig.merge(newConfig, overwrite);
            return existingConfig.encode();
        } catch (EncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
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
     * @ejb:transaction type="Required"
     */
    public AppdefEntityID[] setConfigResponse(AuthzSubjectValue subject,
                                              AppdefEntityID id,
                                              ConfigResponse response,
                                              String type,
                                              boolean sendConfigEvent)
        throws ConfigFetchException, AppdefEntityNotFoundException,
               PermissionException, EncodingException, FinderException
    {
        byte[] productBytes = null;
        byte[] measurementBytes = null;
        byte[] controlBytes = null;
        byte[] rtBytes = null;
        byte[] autoinventoryBytes = null;

        if(type.equals(ProductPlugin.TYPE_PRODUCT)) {
            productBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            measurementBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            controlBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            rtBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
            //XXX: not handled.
            autoinventoryBytes = response.encode();
        } else {
            throw new IllegalArgumentException("Unknown config type: " + type);
        }

        return configureResource(subject, id, productBytes, measurementBytes,
                                 controlBytes, rtBytes, null, sendConfigEvent);
    }
        
    /**
     * Set all configs for a resource at once.  If any of the config entities
     * are updated and the sendConfigEvent flag is set to true, a
     * ResourceUpdatedZevent will be sent out for this resource.
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AppdefEntityID[] configureResource(AuthzSubjectValue subject,
                                              AppdefEntityID appdefID,
                                              byte[] productConfig,
                                              byte[] measurementConfig,
                                              byte[] controlConfig,
                                              byte[] rtConfig,
                                              Boolean userManaged,
                                              boolean sendConfigEvent)
        throws ConfigFetchException, AppdefEntityNotFoundException,
               PermissionException, FinderException
    {
        byte[] configBytes;

        boolean wasUpdated = false;
        ConfigResponseDB existingConfig = getConfigResponse(appdefID);
        boolean overwrite =
            ((userManaged != null) && (userManaged.booleanValue() == true)) || //via UI or CLI
            !existingConfig.isUserManaged(); //via AI, dont overwrite changes made via UI or CLI

        configBytes = mergeConfig(existingConfig.getProductResponse(),
                                  productConfig, overwrite);
        if (!AICompare.configsEqual(configBytes,
                                    existingConfig.getProductResponse())) {
            existingConfig.setProductResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getMeasurementResponse(),
                                  measurementConfig, overwrite);
        if (!AICompare.configsEqual(configBytes,
                                    existingConfig.getMeasurementResponse())) {
            existingConfig.setMeasurementResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getControlResponse(),
                                  controlConfig, overwrite);
        if (!AICompare.configsEqual(configBytes,
                                    existingConfig.getControlResponse())) {
            existingConfig.setControlResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getResponseTimeResponse(),
                                  rtConfig, overwrite);
        if (!AICompare.configsEqual(configBytes,
                                    existingConfig.getResponseTimeResponse())) {
            existingConfig.setResponseTimeResponse(configBytes);
            wasUpdated = true;
        }

        if (userManaged != null &&
            existingConfig.getUserManaged() != userManaged.booleanValue()) {
            existingConfig.setUserManaged(userManaged.booleanValue());
            wasUpdated = true;
        }

        // XXX, in some cases the configuration is not flushed when the
        // zevent is dispatched after the commit.  Need to figure out why, in
        // the meantime flush so that configuring servers by hand does not
        // result in config errors.
        ConfigResponseDAO dao =
            DAOFactory.getDAOFactory().getConfigResponseDAO();
        dao.getSession().flush();
        
        if (wasUpdated && sendConfigEvent) {
            ResourceUpdatedZevent event = new ResourceUpdatedZevent(subject,
                                                                    appdefID);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }

        // XXX: Need to cascade and send events for each resource that may
        // have been affected by this config update.
        AppdefEntityID[] ids = { appdefID };
        return ids;
    }

    /** Update the appdef entities based on TypeInfo
     * @ejb:interface-method
     * @ejb:transaction type="Required"
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

    private byte[] getConfigForType(ConfigResponseDB val,
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
            res = val.getAutoInventoryResponse();
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

    public static ConfigManagerLocal getOne() {
        try {
            return ConfigManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}

    private ServerConfigStuff getServerStuffForService(Integer id)
        throws AppdefEntityNotFoundException {

        ServiceDAO dao = DAOFactory.getDAOFactory().getServiceDAO();
        Service service = dao.findById(id);
        Server server = service.getServer();
        return new ServerConfigStuff(server.getId().intValue(),
                                     server.getInstallPath());
    }

    private ServerConfigStuff getServerStuffForServer(Integer id) 
        throws AppdefEntityNotFoundException {

        ServerDAO dao = DAOFactory.getDAOFactory().getServerDAO();
        Server server = dao.findById(id);

        return new ServerConfigStuff(server.getId().intValue(),
                                     server.getInstallPath());
    }

    private PlatformConfigStuff getPlatformStuffForServer(Integer id) 
        throws AppdefEntityNotFoundException {

        ServerDAO dao = DAOFactory.getDAOFactory().getServerDAO();
        Server server = dao.findById(id);
        Platform platform = server.getPlatform();

        PlatformConfigStuff pConfig =
            new PlatformConfigStuff(platform.getId().intValue(),
                                    platform.getName(),
                                    platform.getFqdn(),
                                    platform.getPlatformType().getName());
        loadPlatformIp(platform, pConfig);
        return pConfig;
    }

    private PlatformConfigStuff getPlatformStuffForPlatform(Integer id) 
        throws AppdefEntityNotFoundException {

        PlatformDAO dao = DAOFactory.getDAOFactory().getPlatformDAO();
        Platform platform = dao.findById(id);

        PlatformConfigStuff pConfig =
            new PlatformConfigStuff(platform.getId().intValue(),
                                    platform.getName(),
                                    platform.getFqdn(),
                                    platform.getPlatformType().getName());
        loadPlatformIp(platform, pConfig);
        return pConfig;
    }

    private void  loadPlatformIp(Platform platform,
                                 PlatformConfigStuff pConfig)
        throws AppdefEntityNotFoundException {

        Collection ips = platform.getIps();
        for (Iterator i = ips.iterator(); i.hasNext(); ) {
            Ip ip = (Ip)i.next();
            if (!ip.getAddress().equals("127.0.0.1")) {
                // First non-loopback address
                pConfig.ip = ip.getAddress();
                break;
            }
        }
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
