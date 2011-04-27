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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.hyperic.hq.appdef.shared.AppdefConverter;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.hqu.shared.UIPluginManager;
import org.hyperic.hq.inventory.domain.ConfigType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Product Boss
 */
@Service
@Transactional
public class ProductBossImpl implements ProductBoss {
    private ResourceGroupManager resourceGroupManager;
    private ProductManager productManager;
    private ConfigManager configManager;
    private UIPluginManager uiPluginManager;
    private AuthzSubjectManager authzSubjectManager;
    private SessionManager sessionManager;
    private AppdefConverter appdefConverter;
    

    @Autowired
    public ProductBossImpl(ResourceGroupManager resourceGroupManager,
                           ConfigManager configManager,
                           UIPluginManager uiPluginManager, AuthzSubjectManager authzSubjectManager,
                           SessionManager sessionManager,
                           ProductManager productManager, AppdefConverter appdefConverter) {
        this.resourceGroupManager = resourceGroupManager;
        this.configManager = configManager;
        this.uiPluginManager = uiPluginManager;
        this.authzSubjectManager = authzSubjectManager;
        this.sessionManager = sessionManager;
        this.productManager = productManager;
        this.appdefConverter = appdefConverter;
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
        
        for (int i=0; i<caches.length; i++) {
            res.add(cacheManager.getCache(caches[i]));
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
            AppdefEntityID id = appdefConverter.newAppdefEntityId(r);
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
    
    public ConfigResponse getMergedConfigResponse(int sessionId, String productType, AppdefEntityID id, boolean required)
        throws AppdefEntityNotFoundException, EncodingException, PermissionException, ConfigFetchException,
        SessionNotFoundException, SessionTimeoutException {
        // validate the session
        sessionManager.authenticate(sessionId);
        // use the overlord to pull the merge
        // FIXME - this is a pretty ugly compromise.
        return getMergedConfigResponse(getOverlord(), productType, id, required);
    }

    /**
     */
    @Transactional(readOnly=true)
    
    public ConfigResponse getMergedConfigResponse(AuthzSubject subject, String productType, AppdefEntityID id,
                                                  boolean required) throws AppdefEntityNotFoundException,
        PermissionException, ConfigFetchException, EncodingException {
        // Get the merged config
        return configManager.getMergedConfigResponse(subject, productType, id, required);
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
    public ConfigType getConfigSchema(int sessionId, Integer id, String type)
        throws SessionTimeoutException, SessionNotFoundException, NotFoundException {
        sessionManager.authenticate(sessionId);
        return productManager.getConfigSchema(id,type);
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