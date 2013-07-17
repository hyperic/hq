/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.hqu.shared.UIPluginManager;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


/**
 * The Product Boss
 */
@Service
@Transactional
public class ProductBossImpl implements ProductBoss {
    private ResourceGroupManager resourceGroupManager;
    private ProductManager productManager;
    private PlatformManager platformManager;
    private ConfigManager configManager;
    private UIPluginManager uiPluginManager;
    private AuthzSubjectManager authzSubjectManager;
    private SessionManager sessionManager;
    private ProductPluginDeployer productPluginDeployer;
    private MonitorableTypeDAO monitorableTypeDAO;    
    
    protected Log log = LogFactory.getLog(ProductBossImpl.class.getName());    
    

    @Autowired
    public ProductBossImpl(ResourceGroupManager resourceGroupManager,
                           PlatformManager platformManager, ConfigManager configManager,
                           UIPluginManager uiPluginManager, AuthzSubjectManager authzSubjectManager,
                           SessionManager sessionManager,
                           ProductManager productManager,
                           ProductPluginDeployer productPluginDeployer, MonitorableTypeDAO monitorableTypeDAO) {
        this.resourceGroupManager = resourceGroupManager;
        this.platformManager = platformManager;
        this.configManager = configManager;
        this.uiPluginManager = uiPluginManager;
        this.authzSubjectManager = authzSubjectManager;
        this.sessionManager = sessionManager;
        this.productManager = productManager;
        this.productPluginDeployer = productPluginDeployer;
        this.monitorableTypeDAO = monitorableTypeDAO;
    }

    private AuthzSubject getOverlord() {
        return authzSubjectManager.getOverlordPojo();
    }
      
    /**
     * TODO stats could be accessed via MBean or some other centralized mechanism
     * @return A List of Maps, where each Map contains individual cache stats
     */
    public List<Map<String,Object>> getCacheHealths() {
        List<Cache> caches = getCaches();
        List<Map<String,Object>> healths = new ArrayList<Map<String,Object>>(caches.size());
        for (Cache cache : caches ) {
            Map<String,Object> health = new HashMap<String,Object>();
            health.put("region", cache.getName());
            health.put("size", new Integer(cache.getSize()));
            health.put("hits", new Integer(cache.getHitCount()));
            health.put("misses", new Integer(cache.getMissCountNotFound()));
            healths.add(health);
        }
        return healths;
    }
    
    private List<Cache> getCaches() {
        CacheManager cacheManager = CacheManager.getInstance();
        String[] caches = cacheManager.getCacheNames();
        List<Cache> res = new ArrayList<Cache>(caches.length);
        
        for (String cache : caches) {
            res.add(cacheManager.getCache(cache));
        }
        return res;
    }

    /**
     * Get the merged config responses for group entries. This routine has the
     * same functionality as getMergedConfigResponse, except it takes in a
     * groupId and returns multiple configResponse objects -- 1 for each entity
     * in the group.
     * 
     * @param productType one of ProductPlugin.TYPE_*
     * @param groupId ID of the group to get configs for
     * @param required If true, all the entities required to make a merged
     *        config response must exist. Else as many values as can be gotten
     *        are tried.
     */
    @Transactional(readOnly=true)
    public ConfigResponse[] getMergedGroupConfigResponse(int sessionId, String productType, int groupId,
                                                         boolean required) throws AppdefEntityNotFoundException,
        PermissionException, ConfigFetchException, SessionNotFoundException, SessionTimeoutException, EncodingException {
        // validate the session
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, new Integer(groupId));

        // use the overlord to pull the merge
        subject = getOverlord();
        Collection<Resource> members = resourceGroupManager.getMembers(group);
        ConfigResponse[] res = new ConfigResponse[members.size()];
        int idx = 0;
        for (Resource r : members) {
            AppdefEntityID id = AppdefUtil.newAppdefEntityId(r);
            res[idx++] = getMergedConfigResponse(subject, productType, id, required);
        }

        return res;
    }

    /**
     * Get a config response object merged through the hierarchy. All entities
     * are merged with the product's config response, and any entity lower than
     * them in the config stack. Config responses defining a specific attribute
     * will override the same attribute if it was declared lower in the
     * application stack. Only entities within the same plugin will be
     * processed, so the most likely situation is a simple service + server +
     * product or server + product merge. Additionally, due to restrictions in
     * the authz model, this merge is performed as the overlord user regardless
     * of the caller. The reason for this is that the caller may not have view
     * access to the entire hierarchy, but still wants to view the merged
     * configuration. This will need to be reviewed post-release
     * 
     * @param productType one of ProductPlugin.TYPE_*
     * @param id Entity to get config for
     * @param required If true, all the entities required to make a merged
     *        config response must exist. Else as many values as can be gotten
     *        are tried.
     */
    @Transactional(readOnly=true)
    public ConfigResponse getMergedConfigResponse(int sessionId, String productType,
                                                  AppdefEntityID id, boolean required)
    throws AppdefEntityNotFoundException, EncodingException, PermissionException,
           ConfigFetchException, SessionNotFoundException, SessionTimeoutException {
        // validate the session
        sessionManager.authenticate(sessionId);
        // use the overlord to pull the merge
        // FIXME - this is a pretty ugly compromise.
        return getMergedConfigResponse(getOverlord(), productType, id, required);
    }

    /**
     */
    @Transactional(readOnly=true)
    public ConfigResponse getMergedConfigResponse(AuthzSubject subject, String productType,
                                                  AppdefEntityID id, boolean required)
    throws AppdefEntityNotFoundException, PermissionException, ConfigFetchException,
           EncodingException {
        // Get the merged config
        return configManager.getMergedConfigResponse(subject, productType, id, required);
    }

    /**
     */
    @Transactional(readOnly=true)
    public ConfigResponseDB getConfigResponse(int sessionId, AppdefEntityID id)
    throws AppdefEntityNotFoundException,
        SessionNotFoundException, SessionTimeoutException {
        sessionManager.authenticate(sessionId);
        return configManager.getConfigResponse(id);
    }

    /**
     */
    @Transactional(readOnly=true)
    
    public String getMonitoringHelp(int sessionId, AppdefEntityID id, Map<?, ?> props) throws PluginNotFoundException,
        PermissionException, AppdefEntityNotFoundException, SessionNotFoundException, SessionTimeoutException {
        AppdefEntityValue aval = new AppdefEntityValue(id, getOverlord());
        return productManager.getMonitoringHelp(aval, props);
    }

    /**
     * Get the config schema used to configure an entity. If the appropriate
     * base entities have not yet been configured, an exception will be thrown
     * indicating which resource must be configured.
     */
    @Transactional(readOnly=true)
    
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, String type, ConfigResponse resp)
        throws SessionTimeoutException, SessionNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException {
        sessionManager.authenticate(sessionId);
        return getConfigSchema(id, type, resp);
    }

    /**
     */
    @Transactional(readOnly=true)
    
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, String type) throws ConfigFetchException,
        EncodingException, PluginNotFoundException, PluginException, SessionTimeoutException, SessionNotFoundException,
        PermissionException, AppdefEntityNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return getConfigSchema(subject, id, type, true);
    }

    public static class ConfigSchemaAndBaseResponse {
        private ConfigSchema schema;
        private ConfigResponse response;

        ConfigSchemaAndBaseResponse(ConfigSchema schema, ConfigResponse resp) {
            this.schema = schema;
            this.response = resp;
        }

        public ConfigSchema getSchema() {
            return schema;
        }

        public ConfigResponse getResponse() {
            return response;
        }
    }

    /**
     * Get a configuration schema.
     * 
     * @param id Entity to be configured
     * @param type One of ProductPlugin.TYPE_*
     * @param validateFlow If true a ConfigFetchException will be thrown if the
     *        appropriate base entities are not already configured.
     */
    @Transactional(readOnly=true)
    
    public ConfigSchemaAndBaseResponse getConfigSchemaAndBaseResponse(AuthzSubject subject, AppdefEntityID id,
                                                                      String type, boolean validateFlow)
        throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException {
        ConfigResponse baseResponse = null;

        AuthzSubject overlord = getOverlord();
        if (validateFlow == true) {
            try {
                baseResponse = configManager.getMergedConfigResponse(overlord, type, id, true);
            } catch (ConfigFetchException exc) {
                // If the thing that failed is the thing we are trying to
                // configure, then everything is okey-dokey ... else
                if (!exc.matchesQuery(id, type)) {
                    throw exc;
                }
            }
        }

        if (baseResponse == null) {
            baseResponse = configManager.getMergedConfigResponse(overlord, type, id, false);
        }

        return new ConfigSchemaAndBaseResponse(getConfigSchema(id, type, baseResponse), baseResponse);
    }

    /**
     */
    @Transactional(readOnly=true)
    
    public ConfigSchema getConfigSchema(AuthzSubject subject, AppdefEntityID id, String type, boolean validateFlow)
        throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException {
        return getConfigSchemaAndBaseResponse(subject, id, type, validateFlow).getSchema();
    }

    /**
     * Get a configuration schema.
     * 
     * @param id Entity to be configured
     * @param type One of ProductPlugin.TYPE_*
     * @param baseResponse the response object of the given type
     */
    @Transactional(readOnly=true)
    private ConfigSchema getConfigSchema(AppdefEntityID id, String type, ConfigResponse baseResponse)
        throws PluginException, PermissionException, AppdefEntityNotFoundException {

        String name;
        if (type.equals(ProductPlugin.TYPE_PRODUCT)) {
            name = configManager.getPluginName(id);
        } else {
            name = platformManager.getPlatformPluginName(id);
        }

        AppdefEntityValue aval = new AppdefEntityValue(id, getOverlord());

        return productManager.getConfigSchema(type, name, aval, baseResponse);
    }  
          
    @Transactional(readOnly=true)
    public ConfigSchema getConfigSchema(ConfigResponse config, String platformName, TypeInfo resourceTypeInfo, String configType)
            throws PluginException {
        
        if ((null == configType) || (null == resourceTypeInfo)) {
            return null;
        }
        
        final String typeName = resourceTypeInfo.getName();
        MonitorableType pluginByName = monitorableTypeDAO.findByName(typeName);
        String pluginName = ((pluginByName == null) ? "" : pluginByName.getPlugin());
                
        final ProductPluginManager pluginManager = productPluginDeployer.getProductPluginManager();                
        final PluginManager pm = pluginManager.getPluginManager(configType);
        
        final ConfigSchema configSchema = pm.getConfigSchema(pluginName, platformName, typeName, resourceTypeInfo, config);
        return configSchema;

    }  
    
    @Transactional(readOnly=true)
    public Map<String, ConfigSchema> getConfigSchemas(String prototypeName, String configType) throws PluginException {
//        AuthzSubject subject = sessionManager.getSubject(sessionId);
//                        
        final ProductPluginManager pluginManager = productPluginDeployer.getProductPluginManager();
        final Map<String, TypeInfo> prototypeTypeInfos = pluginManager.getTypeInfo(prototypeName);   
        if (CollectionUtils.isEmpty(prototypeTypeInfos)) {            
            return Collections.emptyMap();
        }
        Map<String, ConfigSchema> configurationSchemas = new HashMap<String, ConfigSchema>(10);
        boolean noPluginsFound = true;
        PluginNotFoundException pluginNotFoundExceptionCaught = null;
        for(Map.Entry<String, TypeInfo> prototypeTypeInfo:prototypeTypeInfos.entrySet()) {
            try {
                final String platformName = prototypeTypeInfo.getKey();
                final ConfigSchema configSchema = getConfigSchema(new ConfigResponse(), platformName, 
                                                                    prototypeTypeInfo.getValue(), configType);
                configurationSchemas.put(platformName, configSchema); 
                noPluginsFound = false;
            } catch (PluginNotFoundException e) {
                // not all plugins exist for all platforms
                pluginNotFoundExceptionCaught = e;
                log.debug("Plugin not found for prototype " + prototypeName + " for configType " + configType, e);
            }

        }            
        if ((null != pluginNotFoundExceptionCaught) && noPluginsFound) {
            throw pluginNotFoundExceptionCaught;
        }
        return configurationSchemas;        
    }
    
    
    /**
     * Set the config response for an entity/type combination. Note that setting
     * the config response for any entity may cause a chain reaction of things
     * to occur. For instance, agents may get updated with new measurements for
     * entities which were affected by the configuration change.
     * 
     * @param id ID of the object to set the repsonse fo
     * @param response The response
     * @param type One of ProductPlugin.TYPE_*
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     */
    public void setConfigResponse(int sessionId, AppdefEntityID id, ConfigResponse response, String type)
        throws InvalidConfigException, SessionTimeoutException, EncodingException, PermissionException,
        ConfigFetchException, AppdefEntityNotFoundException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        this.setConfigResponse(subject, id, response, type);
    }

    /**
     */
    public void setConfigResponse(AuthzSubject subject, AppdefEntityID id, ConfigResponse response, String type)
        throws EncodingException, PermissionException, InvalidConfigException, ConfigFetchException,
        AppdefEntityNotFoundException {
        this.setConfigResponse(subject, id, response, type, true);
    }

    /**
     * @return The array of IDs affected.
     */
    private AppdefEntityID[] setConfigResponse(AuthzSubject subject, AppdefEntityID id, ConfigResponse response,
                                               String type, boolean shouldValidate) throws EncodingException,
        PermissionException, InvalidConfigException, ConfigFetchException, AppdefEntityNotFoundException {
        boolean doRollback = true;
        try {
            if (configManager.setConfigResponse(subject, id, response, type, false/*userManaged*/, true) != null) {
                AppdefEntityID[] ids = new AppdefEntityID[] { id };

                ConfigValidator configValidator = (ConfigValidator) org.hyperic.hq.common.ProductProperties
                    .getPropertyInstance(ConfigValidator.PDT_PROP);

                if (shouldValidate) {
                    configValidator.validate(subject, type, ids);
                }
            }

            doRollback = false;
            return new AppdefEntityID[0];

        } finally {
            if (doRollback) {
                // FIXME: HE-215
                throw new IllegalStateException("Exception to cause rollback");
            }
        }
    }

    /**
     * Gets the version number
     */
    @Transactional(readOnly=true)
    
    public String getVersion() {
        return ProductProperties.getVersion();
    }

    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     */
    @Transactional(readOnly=true)
    
    public Collection<AttachmentDescriptor> findAttachments(int sessionId, AttachType type) throws SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return uiPluginManager.findAttachments(type, subject);
    }

    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     */
    @Transactional(readOnly=true)
    
    public Collection<AttachmentDescriptor> findAttachments(int sessionId, AppdefEntityID ent, ViewResourceCategory cat)
        throws SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return uiPluginManager.findAttachments(ent, cat, subject);
    }

    /**
     */
    @Transactional(readOnly=true)
    
    public AttachmentDescriptor findAttachment(int sessionId, Integer descId) throws SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return uiPluginManager.findAttachmentDescriptorById(descId, subject);
    }

    /**
     * Get an attachment view by ID
     */
    @Transactional(readOnly=true)
    
    public View findViewById(int sessionId, Integer id) {
        return uiPluginManager.findViewById(id);
    }

}