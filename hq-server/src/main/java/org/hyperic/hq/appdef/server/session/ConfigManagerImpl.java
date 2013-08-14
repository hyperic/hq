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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AICompare;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigDiff;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.sigar.NetFlags;
import org.hyperic.util.Classifier;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@org.springframework.stereotype.Service("ConfigManager")
public class ConfigManagerImpl implements ConfigManager {
    private static final int MAX_VALIDATION_ERR_LEN = 512;
    protected final Log log = LogFactory.getLog(ConfigManagerImpl.class.getName());
    private final MonitorableTypeDAO monitorableTypeDAO;
    private final ConfigResponseDAO configResponseDAO;
    private final ServiceDAO serviceDAO;
    private final ServerDAO serverDAO;
    private final PlatformDAO platformDAO;
    private final ResourceManager resourceManager;
//    private PlatformManager platformManager;
//    private ProductManager productManager;

    @Autowired
    public ConfigManagerImpl(ConfigResponseDAO configResponseDAO, ServiceDAO serviceDAO,
                             ServerDAO serverDAO, PlatformDAO platformDAO, ResourceManager resourceManager,
                             MonitorableTypeDAO monitorableTypeDAO) {
        this.configResponseDAO = configResponseDAO;
        this.serviceDAO = serviceDAO;
        this.serverDAO = serverDAO;
        this.platformDAO = platformDAO;
        this.resourceManager = resourceManager;
        this.monitorableTypeDAO = monitorableTypeDAO;
    }

//    @PostConstruct
//    public void init() {
//        this.platformManager = (PlatformManager) Bootstrap.getBean("PlatformManager");
//        this.productManager = (ProductManager) Bootstrap.getBean("ProductManager");
//    }
    
    /**
     * 
     */
    public ConfigResponseDB createConfigResponse(byte[] productResponse, byte[] measResponse, byte[] controlResponse,
                                                 byte[] rtResponse) {
        ConfigResponseDB cr = configResponseDAO.create();
        cr.setProductResponse(productResponse);
        cr.setMeasurementResponse(measResponse);
        cr.setControlResponse(controlResponse);
        cr.setResponseTimeResponse(rtResponse);
        return cr;
    }
    
    @Transactional(readOnly=true)
    public Map<Resource, ConfigResponse> getConfigResponses(Set<Resource> resources, boolean hideSecrets) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final Map<Integer, Collection<Resource>> resourcesByType = new Classifier<Resource, Integer, Resource>() {
            @Override
            public NameValue<Integer, Resource> classify(Resource r) {
                return new NameValue<Integer, Resource>(r.getResourceType().getId(), r);
            }
        }.classify(resources);
        final Map<Resource, ConfigResponseDB> tmp = new HashMap<Resource, ConfigResponseDB>();
        final ProductPluginDeployer productPluginDeployer = Bootstrap.getBean(ProductPluginDeployer.class);
        if (debug) {
            watch.markTimeBegin("getResourceConfigs");
        }
        for (final Entry<Integer, Collection<Resource>> entry : resourcesByType.entrySet()) {
            final Integer resourceTypeId = entry.getKey();
            final List<Resource> list = new ArrayList<Resource>(entry.getValue());
            if (resourceTypeId.equals(AuthzConstants.authzPlatform)) {
                tmp.putAll(configResponseDAO.getPlatformConfigs(list));
            } else if (resourceTypeId.equals(AuthzConstants.authzServer)) {
                tmp.putAll(configResponseDAO.getServerConfigs(list));
            } else if (resourceTypeId.equals(AuthzConstants.authzService)) {
                tmp.putAll(configResponseDAO.getServiceConfigs(list));
            }
        }
        if (debug) {
            watch.markTimeEnd("getResourceConfigs");
        }
        final List<MonitorableType> all = monitorableTypeDAO.findAll();
        final Map<String, String> monitorableTypeMap = new Classifier<MonitorableType, String, String>() {
            @Override
            public NameValue<String, String> classify(MonitorableType key) {
                return new NameValue<String, String>(key.getName(), key.getPlugin());
            }
        }.classifyUnique(all);
        final Map<Resource, ConfigResponse> rtn = new HashMap<Resource, ConfigResponse>();
        for (final Entry<Resource, ConfigResponseDB> entry : tmp.entrySet()) {
            final Resource resource = entry.getKey();
            if ((resource == null) || resource.isInAsyncDeleteState() || resource.isSystem()) {
                continue;
            }
            final ConfigResponseDB crdb = entry.getValue();
            final ConfigResponse configResponse = new ConfigResponse();
            final byte[] productResponse = crdb.getProductResponse();
            final byte[] controlResponse = crdb.getControlResponse();
            final byte[] measurementResponse = crdb.getMeasurementResponse();
            rtn.put(resource, configResponse);
            if (resource.getResourceType().getId().equals(AuthzConstants.authzPlatform)) {
                final Platform platform = platformDAO.get(resource.getInstanceId());
                if (platform == null) {
                    continue;
                }
                configResponse.setValue(ProductPlugin.PROP_PLATFORM_NAME, platform.getName());
                configResponse.setValue(ProductPlugin.PROP_PLATFORM_FQDN, platform.getFqdn());
                configResponse.setValue(ProductPlugin.PROP_PLATFORM_TYPE, resource.getPrototype().getName());
                configResponse.setValue(ProductPlugin.PROP_PLATFORM_IP, getIp(platform));
                configResponse.setValue(ProductPlugin.PROP_PLATFORM_ID, String.valueOf(platform.getId()));
            }
            try {
                if ((measurementResponse != null) && (measurementResponse.length > 0)) {
                    configResponse.merge(ConfigResponse.decode(measurementResponse), true);
                }
                if ((productResponse != null) && (productResponse.length > 0)) {
                    configResponse.merge(ConfigResponse.decode(productResponse), true);
                }
                if ((controlResponse != null) && (controlResponse.length > 0)) {
                    configResponse.merge(ConfigResponse.decode(controlResponse), true);
                }
                // This is the bottleneck of this method
                if (debug) {
                    watch.markTimeBegin("mergeWithConfigSchema");
                }
                mergeWithConfigSchema(resource, configResponse, monitorableTypeMap, hideSecrets, productPluginDeployer);
                if (debug) {
                    watch.markTimeEnd("mergeWithConfigSchema");
                }
            } catch (EncodingException e) {
                log.warn("could not decode config associated with resourceId=" + resource.getId());
                log.debug(e,e);
            }
        }
        if (debug) {
            log.debug(watch);
        }
        return rtn;
    }

    @Transactional(readOnly=true)
    private ConfigSchema getConfigSchema(AuthzSubject subject, PlatformManager platformManager, ProductManager productManager, AppdefEntityID id, String type, ConfigResponse baseResponse) {
        String name;
        try {
            if (type.equals(ProductPlugin.TYPE_PRODUCT)) {
                name = this.getPluginName(id);
            } else {
                name = platformManager.getPlatformPluginName(id);
            }

            AppdefEntityValue aval = new AppdefEntityValue(id, subject);

            return productManager.getConfigSchema(type, name, aval, baseResponse);
        }catch(AppdefEntityNotFoundException e) {
            log.error(e,e);
        }catch(PermissionException e) {
            log.error(e,e);
        }catch(PluginException e) {
            log.error(e,e);
        }
        return null;
    }  
    
    
    private void mergeWithConfigSchema(Resource r, ConfigResponse config, Map<String, String> monitorableTypeMap,
                                       boolean hideSecrets, ProductPluginDeployer productPluginDeployer) {
        final AppdefEntityID id = AppdefUtil.newAppdefEntityId(r);
        
        final Integer resourceTypeId = r.getResourceType().getId();
        final String proto = r.getPrototype().getName();
        final String plugin = monitorableTypeMap.get(proto);
        TypeInfo type = null;
        if (resourceTypeId.equals(AuthzConstants.authzPlatform)) {
            type = new PlatformTypeInfo(proto);
        } else if (resourceTypeId.equals(AuthzConstants.authzServer)) {
            type = new ServerTypeInfo(proto, "", "");
        } else if (resourceTypeId.equals(AuthzConstants.authzService)) {
            final Service service = serviceDAO.get(r.getInstanceId());
            final Server server = service.getServer();
            final ServerType serverType = server.getServerType();
            final ServerTypeInfo serverTypeInfo = new ServerTypeInfo(serverType.getName(), serverType.getDescription(), "");
            type = new ServiceTypeInfo(proto, service.getServiceType().getDescription(), serverTypeInfo);
        }
        try {
            final ProductPluginManager productPluginManager = productPluginDeployer.getProductPluginManager();
            final PluginManager measurementPluginManager = productPluginManager.getPluginManager("measurement");
            final PluginManager controlPluginManager = productPluginManager.getPluginManager("control");
            final List<ConfigOption> options = new ArrayList<ConfigOption>();
            options.addAll(getConfigOptions(productPluginManager, plugin, type, config));
            options.addAll(getConfigOptions(measurementPluginManager, proto, type, config));
            options.addAll(getConfigOptions(controlPluginManager, proto, type, config));
            final Set<String> keys = config.getKeys();
            for (final ConfigOption o : options) {
                final String key = o.getName();
                if (!keys.contains(key)) {
                    config.setValue(key, o.getDefault());
                }
                if ((o instanceof StringConfigOption) && ((StringConfigOption) o).isHidden()) {
                    config.unsetValue(key);
                } else if (hideSecrets && (o instanceof StringConfigOption) && ((StringConfigOption) o).isSecret()) {
                    config.setValue(key, "*********");
                }
            }
        } catch (PluginException e) {
            log.warn(e);
            log.debug(e,e);
        }
    }
    
    private List<ConfigOption> getConfigOptions2(PluginManager pluginManager, String key, TypeInfo type,
            ConfigResponse config) {
        try {
            ConfigSchema configSchema = pluginManager.getConfigSchema(key, type, config);
            return configSchema.getOptions();
        } catch (PluginNotFoundException e) {
            // normally log.debug with the stack is fine, but it is a very common scenario where a plugin
            // does not support a certain type.  This api should probably return null instead of throwing
            // an exception.  The stacktrace is really not helpful
            log.debug(e);
            log.trace(e,e);
        }
        return Collections.emptyList();
    }
    
    private List<ConfigOption> getConfigOptions(PluginManager pluginManager, String key, TypeInfo type,
                                                ConfigResponse config) {
        try {
            ConfigSchema configSchema = pluginManager.getConfigSchema(key, type, config);
            return configSchema.getOptions();
        } catch (PluginNotFoundException e) {
            // normally log.debug with the stack is fine, but it is a very common scenario where a plugin
            // does not support a certain type.  This api should probably return null instead of throwing
            // an exception.  The stacktrace is really not helpful
            log.debug(e);
            log.trace(e,e);
        }
        return Collections.emptyList();
    }

    private String getIp(Platform platform) {
        Collection<Ip> ips = platform.getIps();
        for (Ip ip : ips) {
            String address = ip.getAddress();
            if (!address.equals(NetFlags.LOOPBACK_ADDRESS)) {
                return address;
            }
        }
        return null;
    }

    /**
     * 
     * Get the ConfigResponse for the given ID, creating it if it does not
     * already exist.
     * 
     */
    @Transactional(readOnly=true)
    public ConfigResponseDB getConfigResponse(AppdefEntityID id) {
        ConfigResponseDB config = null;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                Platform platform = platformDAO.get(id.getId());
                if(platform != null) {
                    config = platform.getConfigResponse();
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server server = serverDAO.get(id.getId());
                if(server != null) {
                    config = server.getConfigResponse();
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                Service service = serviceDAO.get(id.getId());
                if(service != null) {
                    config = service.getConfigResponse();
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            default:
                throw new IllegalArgumentException("The resource[ " + id + "] does not support config " + "responses");
        }
        // Platforms, servers, and services should have a config response record.
        // A null config response could indicate that the resource has been deleted.
        if (config == null) {
            throw new IllegalArgumentException(
                "No config response found for resource[" + id + "]");
        }
        return config;
    }

    private Platform findPlatformById(Integer id) throws PlatformNotFoundException {
        Platform platform = platformDAO.get(id);

        if (platform == null) {
            throw new PlatformNotFoundException(id);
        }

        // Make sure that resource is loaded as to not get
        // LazyInitializationException
        platform.getName();

        return platform;
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public String getPluginName(AppdefEntityID id) throws AppdefEntityNotFoundException {
        Integer intID = id.getId();
        String pname;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                Platform plat = findPlatformById(intID);
                pname = plat.getPlatformType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server serv = serverDAO.get(intID);
                if (serv == null) {
                    throw new ServerNotFoundException(intID);
                }
                pname = serv.getServerType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                org.hyperic.hq.appdef.server.session.Service service = serviceDAO.get(intID);
                if (service == null) {
                    throw new ServiceNotFoundException(intID);
                }
                pname = service.getServiceType().getPlugin();
                break;

            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            default:
                throw new IllegalArgumentException("The passed entity type " + "does not support config responses");
        }

        return pname;
    }

    /**
     * Get a config response object merged through the hierarchy. All entities
     * are merged with the product's config response, and any entity lower than
     * them in the config stack. Config responses defining a specific attribute
     * will override the same attribute if it was declared lower in the
     * application stack. Only entities within the same plugin will be
     * processed, so the most likely situation is a simple service + server +
     * product or server + product merge.
     * 
     * Example: Get the SERVICE MEASUREMENT merged response: PRODUCT[platform] +
     * MEASUREMENT[platform] PRODUCT[server] + MEASUREMENT[server] +
     * PRODUCT[service] + MEASUREMENT[service]
     * 
     * Get the SERVER PRODUCT merged response: PRODUCT[platform] PRODUCT[server]
     * 
     * Get the PLATFORM PRODUCT merged response: PRODUCT[platform]
     * 
     * In addition to the configuration, some inventory properties are also
     * merged in to aid in auto-configuration done by autoinventory.
     * 
     * For Servers and Services: The install path of the server is included
     * 
     * For all Resources: The first non-loopback ip address, fqdn, platform name
     * and type.
     * 
     * @param productType One of ProductPlugin.*
     * @param id An AppdefEntityID of the object to get config for
     * 
     * @return the merged ConfigResponse
     * 
     * 
     * 
     */

    @Transactional(readOnly=true)
    public ConfigResponse getMergedConfigResponse(AuthzSubject subject, String productType, AppdefEntityID id,
                                                  boolean required) throws AppdefEntityNotFoundException,
        ConfigFetchException, EncodingException, PermissionException {
        ConfigResponseDB configValue;
        AppdefEntityID platformId = null, serverId = null, serviceId = null;
        byte[][] responseList; // List of config responses to merge
        ConfigResponse res;
        int responseIdx;
        byte[] data;
        ServerConfigStuff server = null;
        PlatformConfigStuff platform = null;
        boolean origReq = required;
        boolean isServerOrService = false;
        boolean isProductType = productType.equals(ProductPlugin.TYPE_PRODUCT);

        if ((id.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM) &&
            (id.getType() != AppdefEntityConstants.APPDEF_TYPE_SERVER) &&
            (id.getType() != AppdefEntityConstants.APPDEF_TYPE_SERVICE)) {
            throw new IllegalArgumentException(id + " doesn't support " + "config merging");
        }

        // Setup
        responseList = new byte[6][];
        responseIdx = 0;

        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            server = getServerStuffForService(id.getId());
            if (server != null) {
                serverId = AppdefEntityID.newServerID(new Integer(server.id));
                platform = getPlatformStuffForServer(serverId.getId());
                if (platform != null) {
                    platformId = AppdefEntityID.newPlatformID(new Integer(platform.id));
                }
                serviceId = id;
                origReq = required;
                required = false;
                isServerOrService = true;
            }
        } else if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            platform = getPlatformStuffForServer(id.getId());
            if (platform != null) {
                platformId = AppdefEntityID.newPlatformID(new Integer(platform.id));
                serverId = id;
                server = getServerStuffForServer(serverId.getId());
                isServerOrService = true;
            }
        } else {
            // Just the platform
            platformId = id;
            platform = getPlatformStuffForPlatform(platformId.getId());
        }

        // Platform config
        if (platformId != null) {
            // hardcode required=false for server/service types
            // e.g. unlikely that a platform will have control config
            boolean platformConfigRequired = isServerOrService ? false : required;

            configValue = getConfigResponse(platformId);
            data = getConfigForType(configValue, ProductPlugin.TYPE_PRODUCT, platformId, platformConfigRequired);
            responseList[responseIdx++] = data;

            if (!isProductType) {
                if (productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
                    // Skip merging of response time configuration
                    // since platforms don't have it.
                } else {
                    data = getConfigForType(configValue, productType, platformId, platformConfigRequired);
                    responseList[responseIdx++] = data;
                }
            }
        }

        // Server config (if necessary)
        if (serverId != null) {
            if (id.isServer()) {
                required = isProductType ? origReq : false;
            }

            configValue = getConfigResponse(serverId);
            data = getConfigForType(configValue, ProductPlugin.TYPE_PRODUCT, serverId, required);
            responseList[responseIdx++] = data;

            if (!isProductType) {
                required = id.isServer() && origReq; // Reset the required flag

                if (productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
                    // Skip merging of response time configuration
                    // since servers don't have it.
                } else {
                    data = getConfigForType(configValue, productType, serverId, required);
                    responseList[responseIdx++] = data;
                }
            }
        }

        // Service config (if necessary)
        if (serviceId != null) {
            required = isProductType ? origReq : false;

            configValue = getConfigResponse(id);
            data = getConfigForType(configValue, ProductPlugin.TYPE_PRODUCT, id, required);
            responseList[responseIdx++] = data;

            if (!isProductType) {
                required = origReq; // Reset the required flag
                data = getConfigForType(configValue, productType, id, required);
                responseList[responseIdx++] = data;
            }
        }

        // Merge everything together
        res = new ConfigResponse();
        for (int i = 0; i < responseIdx; i++) {
            if ((responseList[i] == null) || (responseList[i].length == 0)) {
                continue;
            }

            res.merge(ConfigResponse.decode(responseList[i]), true);
        }

        // Set platform attributes for all resources
        try {
            if (platform != null) {
                res.setValue(ProductPlugin.PROP_PLATFORM_NAME, platform.name);
                res.setValue(ProductPlugin.PROP_PLATFORM_FQDN, platform.fqdn);
                res.setValue(ProductPlugin.PROP_PLATFORM_TYPE, platform.typeName);
                res.setValue(ProductPlugin.PROP_PLATFORM_IP, platform.ip);
                res.setValue(ProductPlugin.PROP_PLATFORM_ID, String.valueOf(platform.id));
            }
        } catch (Exception exc) {
            log.warn("Error setting platform properies: " + exc, exc);
        }

        // Set installpath attribute for server and service types.
        if (isServerOrService && (server != null)) {
            try {
                res.setValue(ProductPlugin.PROP_INSTALLPATH, server.installPath);
            } catch (Exception exc) {
                log.warn("Error setting installpath property: " + exc, exc);
            }
        }

        return res;
    }

    /**
     * Clear the validation error string for a config response, indicating that
     * the current config is valid
     * 
     */
    public void clearValidationError(AuthzSubject subject, AppdefEntityID id) {
        ConfigResponseDB config = getConfigResponse(id);
        config.setValidationError(null);
    }

    /**
     * Method to merge configs, maintaining any existing values that are not
     * present in the AI config (e.g. log/config track enablement)
     * 
     * @param existingBytes The existing configuration
     * @param newBytes The new configuration
     * @param overwrite TODO
     * @param force TODO
     * @return The newly merged configuration
     */
    private static byte[] mergeConfig(byte[] existingBytes, byte[] newBytes, boolean overwrite, boolean force) {
        if (force || (existingBytes == null) || (existingBytes.length == 0 && newBytes != null )) {
            return newBytes;
        }

        if ((newBytes == null) || (newBytes.length == 0)) {
            // likely a manually created platform service where
            // inventory-properties are auto-discovered but config
            // is left unchanged.
            return existingBytes;
        }

        try {
            ConfigResponse existingConfig = ConfigResponse.decode(existingBytes);
            ConfigResponse newConfig = ConfigResponse.decode(newBytes);
            existingConfig.merge(newConfig, overwrite);
            return existingConfig.encode();
        } catch (EncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Update the validation error string for a config response
     * @param validationError The error string that occured during validation.
     *        If this is null, that means that no error occurred and the config
     *        is valid.
     * 
     * 
     */
    @Transactional
    public void setValidationError(AuthzSubject subject, AppdefEntityID id, String validationError) {
        ConfigResponseDB config = getConfigResponse(id);

        if (validationError != null) {
            if (validationError.length() > MAX_VALIDATION_ERR_LEN) {
                validationError = validationError.substring(0, MAX_VALIDATION_ERR_LEN - 3) + "...";
            }
        }

        configResponseDAO.setValidationError(config, validationError);
    }

    /**
     * Set the config response for an entity/type combination.
     * @param id ID of the object to set the repsonse fo
     * @param response The response
     * @param type One of ProductPlugin.TYPE_
     * @return an array of entities which may be affected by the change in
     *         configuration. For updates to platform and service configs, there
     *         are no other entities other than the given ID returned. If a
     *         server is updated, the associated services may require changes.
     *         The passed entity will always be returned in the array.
     * 
     * 
     * 
     */
    @Transactional
    public AppdefEntityID setConfigResponse(AuthzSubject subject, AppdefEntityID id, ConfigResponse response,
                                            String type, boolean userManaged, boolean sendConfigEvent) throws ConfigFetchException,
        AppdefEntityNotFoundException, PermissionException, EncodingException {
        byte[] productBytes = null;
        byte[] measurementBytes = null;
        byte[] controlBytes = null;
        byte[] rtBytes = null;

        if (type.equals(ProductPlugin.TYPE_PRODUCT)) {
            productBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            measurementBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            controlBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            rtBytes = response.encode();
        } else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
        } else {
            throw new IllegalArgumentException("Unknown config type: " + type);
        }

        ConfigResponseDB existingConfig = getConfigResponse(id);
        boolean wasUpdated = configureResponse(subject, existingConfig, id, productBytes, measurementBytes, controlBytes, rtBytes,
            userManaged, false);
        if (sendConfigEvent) {
            Resource r = resourceManager.findResource(id);
            resourceManager.resourceHierarchyUpdated(subject, Collections.singletonList(r));
        }
        return wasUpdated ? id : null;
    }

    /**
     * 
     *
     */
    @Transactional
    public boolean configureResponse(AuthzSubject subject, ConfigResponseDB existingConfig,
                                            AppdefEntityID appdefID, byte[] productConfig, byte[] measurementConfig,
                                            byte[] controlConfig, byte[] rtConfig, Boolean userManaged,
                                            boolean force) {
        boolean wasUpdated = false;
        byte[] configBytes;

        boolean overwrite = ((userManaged != null) && userManaged.booleanValue()) || // via
                            // UI
                            // or
                            // CLI
                            !existingConfig.isUserManaged(); // via AI, dont
        // overwrite
        // changes made via
        // UI or CLI

        configBytes = mergeConfig(existingConfig.getProductResponse(), productConfig, overwrite, force);
        if (!AICompare.configsEqual(configBytes, existingConfig.getProductResponse())) {
            existingConfig.setProductResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getMeasurementResponse(), measurementConfig, overwrite, force);
        if (!AICompare.configsEqual(configBytes, existingConfig.getMeasurementResponse())) {
            existingConfig.setMeasurementResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getControlResponse(), controlConfig, overwrite, false);
        if (!AICompare.configsEqual(configBytes, existingConfig.getControlResponse())) {
            existingConfig.setControlResponse(configBytes);
            wasUpdated = true;
        }

        configBytes = mergeConfig(existingConfig.getResponseTimeResponse(), rtConfig, overwrite, false);
        if (!AICompare.configsEqual(configBytes, existingConfig.getResponseTimeResponse())) {
            existingConfig.setResponseTimeResponse(configBytes);
            wasUpdated = true;
        }

        if ((userManaged != null) && (existingConfig.getUserManaged() != userManaged.booleanValue())) {
            existingConfig.setUserManaged(userManaged.booleanValue());
            wasUpdated = true;
        }

        return wasUpdated;
    }

    class ConfigDiff implements ConfigManager.ConfigDiff{
        protected boolean wasUpdated;
        protected AllConfigDiff allConfigResponses;

        public boolean isWasUpdated() {
            return wasUpdated;
        }
        public void setWasUpdated(boolean wasUpdated) {
            this.wasUpdated = wasUpdated;
        }
        public AllConfigDiff getAllConfigDiff() {
            return this.allConfigResponses;
        }
        public void setAllConfigDiff(AllConfigDiff allConfigResponses) {
            this.allConfigResponses=allConfigResponses;
        }
    }
    
    @Transactional
    public ConfigDiff configureResponseDiff(AuthzSubject subject, ConfigResponseDB existingConfig,
                                            AppdefEntityID appdefID, byte[] productConfig, byte[] measurementConfig,
                                            byte[] controlConfig, byte[] rtConfig, Boolean userManaged,
                                            boolean force) throws EncodingException {
        byte[] configBytes;

        boolean overwrite = ((userManaged != null) && userManaged.booleanValue()) || // via
                            // UI
                            // or
                            // CLI
                            !existingConfig.isUserManaged(); // via AI, dont
        // overwrite
        // changes made via
        // UI or CLI
        ConfigDiff diffs = new ConfigDiff();
        AllConfigResponses allNewConfigResponses = new AllConfigResponses();
        AllConfigResponses allChangedConfigResponses = new AllConfigResponses();
        AllConfigResponses allDeletedConfigResponses = new AllConfigResponses();

        configBytes = mergeConfig(existingConfig.getProductResponse(), productConfig, overwrite, force);
        AICompare.ConfigDiff productDiffs = AICompare.configsDiff(configBytes, existingConfig.getProductResponse());
        if (((null == existingConfig.getProductResponse()) && (null != configBytes))
                || ((productDiffs != null) && ((productDiffs.getNewConf().size() > 0)
                        || (productDiffs.getChangedConf().size() > 0) || (productDiffs.getDeletedConf().size() > 0)))) {
            existingConfig.setProductResponse(configBytes);
            allNewConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT, productDiffs.getNewConf());
            allChangedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT, productDiffs.getChangedConf());
            allDeletedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT, productDiffs.getDeletedConf());
            diffs.setWasUpdated(true);
        }

        configBytes = mergeConfig(existingConfig.getMeasurementResponse(), measurementConfig, overwrite, force);
        AICompare.ConfigDiff msmtDiffs = AICompare.configsDiff(configBytes, existingConfig.getMeasurementResponse());
        if (((null == existingConfig.getMeasurementResponse()) && (null != configBytes))
                || ((msmtDiffs != null) && ((msmtDiffs.getNewConf().size() > 0)
                        || (msmtDiffs.getChangedConf().size() > 0) || (msmtDiffs.getDeletedConf().size() > 0)))) {
            existingConfig.setMeasurementResponse(configBytes);
            allNewConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT, msmtDiffs.getNewConf());
            allChangedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT, msmtDiffs.getChangedConf());
            allDeletedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT, msmtDiffs.getDeletedConf());
            diffs.setWasUpdated(true);
        }

        configBytes = mergeConfig(existingConfig.getControlResponse(), controlConfig, overwrite, false);
        AICompare.ConfigDiff controlDiffs = AICompare.configsDiff(configBytes, existingConfig.getControlResponse());
        if (((null == existingConfig.getControlResponse()) && (null != configBytes))
                || ((controlDiffs != null) && ((controlDiffs.getNewConf().size() > 0)
                        || (controlDiffs.getChangedConf().size() > 0) || (controlDiffs.getDeletedConf().size() > 0)))) {
            existingConfig.setControlResponse(configBytes);
            allNewConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_CONTROL, controlDiffs.getNewConf());
            allChangedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_CONTROL, controlDiffs.getChangedConf());
            allDeletedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_CONTROL, controlDiffs.getDeletedConf());
            diffs.setWasUpdated(true);
        }

        configBytes = mergeConfig(existingConfig.getResponseTimeResponse(), rtConfig, overwrite, false);
        AICompare.ConfigDiff responseTimeDiffs = AICompare.configsDiff(configBytes, existingConfig.getResponseTimeResponse());
        if (((null == existingConfig.getResponseTimeResponse()) && (null != configBytes))
                || ((responseTimeDiffs != null) && ((responseTimeDiffs.getNewConf().size() > 0)
                        || (responseTimeDiffs.getChangedConf().size() > 0) || (responseTimeDiffs.getDeletedConf()
                        .size() > 0)))) {
            existingConfig.setResponseTimeResponse(configBytes);
            allNewConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, responseTimeDiffs.getNewConf());
            allChangedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, responseTimeDiffs.getChangedConf());
            allDeletedConfigResponses.setConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, responseTimeDiffs.getDeletedConf());
            diffs.setWasUpdated(true);
        }

        if ((userManaged != null) && (existingConfig.getUserManaged() != userManaged.booleanValue())) {
            existingConfig.setUserManaged(userManaged.booleanValue());
            diffs.setWasUpdated(true);
        }
        
        diffs.setAllConfigDiff(new AllConfigDiff(allNewConfigResponses,allChangedConfigResponses,allDeletedConfigResponses));
        return diffs;
    }

    private byte[] getConfigForType(ConfigResponseDB val, String productType, AppdefEntityID id, boolean fail)
    throws ConfigFetchException {
        byte[] res;
        if (productType.equals(ProductPlugin.TYPE_PRODUCT)) {
            res = val.getProductResponse();
        } else if (productType.equals(ProductPlugin.TYPE_CONTROL)) {
            res = val.getControlResponse();
        } else if (productType.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            res = val.getMeasurementResponse();
        } else if (productType.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
            res = val.getAutoInventoryResponse();
        } else if (productType.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            res = val.getResponseTimeResponse();
        } else {
            throw new IllegalArgumentException("Unknown product type");
        }
        if (((res == null) || (res.length == 0)) && fail) {
            throw new ConfigFetchException(productType, id);
        }
        return res;
    }

    private ServerConfigStuff getServerStuffForService(Integer id) throws AppdefEntityNotFoundException {
        // HQ-4096, need to catch ObjectNotFoundException
        try {
            org.hyperic.hq.appdef.server.session.Service service = serviceDAO.get(id);
            if (service == null) {
                return null;
            }
            Server server = service.getServer();
            if (server == null) {
                return null;
            }
            return new ServerConfigStuff(server.getId().intValue(), server.getInstallPath());
        } catch (ObjectNotFoundException e) {
            log.debug(e,e);
            return null;
        }
    }

    private ServerConfigStuff getServerStuffForServer(Integer id) throws AppdefEntityNotFoundException {
        try {
            Server server = serverDAO.findById(id);
            return new ServerConfigStuff(server.getId().intValue(), server.getInstallPath());
        } catch (ObjectNotFoundException e) {
            log.debug(e,e);
            return null;
        }
    }

    private PlatformConfigStuff getPlatformStuffForServer(Integer id) throws AppdefEntityNotFoundException {
        try {
            Server server = serverDAO.findById(id);
            Platform platform = server.getPlatform();
            if (platform == null) {
                return null;
            }
            PlatformConfigStuff pConfig = new PlatformConfigStuff(platform.getId(), platform.getName(),
                                                                  platform.getFqdn(),
                                                                  platform.getPlatformType().getName());
            loadPlatformIp(platform, pConfig);
            return pConfig;
        } catch (ObjectNotFoundException e) {
            log.debug(e,e);
            return null;
        }
    }

    private PlatformConfigStuff getPlatformStuffForPlatform(Integer id) throws AppdefEntityNotFoundException {
        Platform platform = platformDAO.get(id);
        if (platform == null) {
            return null;
        }
        PlatformConfigStuff pConfig = new PlatformConfigStuff(platform.getId(), platform.getName(), platform.getFqdn(),
                                                              platform.getPlatformType().getName());
        loadPlatformIp(platform, pConfig);
        return pConfig;
    }

    private void loadPlatformIp(Platform platform, PlatformConfigStuff pConfig) throws AppdefEntityNotFoundException {

        Collection<Ip> ips = platform.getIps();
        for (Ip ip : ips) {

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

        public PlatformConfigStuff(int id, String name, String fqdn, String typeName) {
            this.id = id;
            this.name = name;
            this.fqdn = fqdn;
            this.typeName = typeName;
        }
    }
}
