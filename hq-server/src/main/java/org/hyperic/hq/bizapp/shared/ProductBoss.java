/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.bizapp.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;

/**
 * Local interface for ProductBoss.
 */
public interface ProductBoss {
    /**
     * Get the merged config responses for group entries. This routine has the
     * same functionality as getMergedConfigResponse, except it takes in a
     * groupId and returns multiple configResponse objects -- 1 for each entity
     * in the group.
     * @param productType one of ProductPTYPE_*
     * @param groupId ID of the group to get configs for
     * @param required If true, all the entities required to make a merged
     *        config response must exist. Else as many values as can be gotten
     *        are tried.
     */
    public ConfigResponse[] getMergedGroupConfigResponse(int sessionId, String productType, int groupId,
                                                         boolean required) throws AppdefEntityNotFoundException,
        PermissionException, ConfigFetchException, SessionNotFoundException, SessionTimeoutException, EncodingException;

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
     * @param productType one of ProductPTYPE_*
     * @param id Entity to get config for
     * @param required If true, all the entities required to make a merged
     *        config response must exist. Else as many values as can be gotten
     *        are tried.
     */
    public ConfigResponse getMergedConfigResponse(int sessionId, String productType, AppdefEntityID id, boolean required)
        throws AppdefEntityNotFoundException, EncodingException, PermissionException, ConfigFetchException,
        SessionNotFoundException, SessionTimeoutException;

    public ConfigResponse getMergedConfigResponse(AuthzSubject subject, String productType, AppdefEntityID id,
                                                  boolean required) throws AppdefEntityNotFoundException,
        PermissionException, ConfigFetchException, EncodingException;

    public ConfigResponseDB getConfigResponse(int sessionId, AppdefEntityID id) throws AppdefEntityNotFoundException,
        SessionNotFoundException, SessionTimeoutException;

    public String getMonitoringHelp(int sessionId, AppdefEntityID id, Map<?, ?> props) throws PluginNotFoundException,
        PermissionException, AppdefEntityNotFoundException, SessionNotFoundException, SessionTimeoutException;

    /**
     * Get the config schema used to configure an entity. If the appropriate
     * base entities have not yet been configured, an exception will be thrown
     * indicating which resource must be configured.
     */
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, String type, ConfigResponse resp)
        throws SessionTimeoutException, SessionNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException;

    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, String type) throws ConfigFetchException,
        EncodingException, PluginNotFoundException, PluginException, SessionTimeoutException, SessionNotFoundException,
        PermissionException, AppdefEntityNotFoundException;

    
    public ConfigSchema getConfigSchema(ConfigResponse config, String platformName, TypeInfo resourceTypeInfo, String configType) throws PluginException; 
    
    /**
     * Get configuration schemas of the specified prototype
     * per platform for the given configuration type
     * @param prototypeName
     * @param configType ProductPlugin.TYPE_PRODUCT or ProductPlugin.TYPE_MEASUREMENT or ProductPlugin.TYPE_CONTROL
     * @return configuration schema per platform
     * @throws PluginException 
     */
    public Map<String, ConfigSchema> getConfigSchemas(String prototypeName, String configType) throws PluginException;
    
    /**
     * Get a configuration schema.
     * @param id Entity to be configured
     * @param type One of ProductPTYPE_*
     * @param validateFlow If true a ConfigFetchException will be thrown if the
     *        appropriate base entities are not already configured.
     */
    public ProductBossImpl.ConfigSchemaAndBaseResponse getConfigSchemaAndBaseResponse(AuthzSubject subject,
                                                                                      AppdefEntityID id, String type,
                                                                                      boolean validateFlow)
        throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException;

    public ConfigSchema getConfigSchema(AuthzSubject subject, AppdefEntityID id, String type, boolean validateFlow)
        throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException,
        AppdefEntityNotFoundException;

    /**
     * Set the config response for an entity/type combination. Note that setting
     * the config response for any entity may cause a chain reaction of things
     * to occur. For instance, agents may get updated with new measurements for
     * entities which were affected by the configuration change.
     * @param id ID of the object to set the repsonse fo
     * @param response The response
     * @param type One of ProductPTYPE_*
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     */
    public void setConfigResponse(int sessionId, AppdefEntityID id, ConfigResponse response, String type)
        throws InvalidConfigException, SessionTimeoutException, EncodingException, PermissionException,
        ConfigFetchException, AppdefEntityNotFoundException, SessionNotFoundException;

    public void setConfigResponse(AuthzSubject subject, AppdefEntityID id, ConfigResponse response, String type)
        throws EncodingException, PermissionException, InvalidConfigException, ConfigFetchException,
        AppdefEntityNotFoundException;

    /**
     * Gets the version number
     */
    public String getVersion();
    
    /**
    * @return A List of Maps, where each Map contains individual cache stats
    */
    public List<Map<String,Object>> getCacheHealths();

    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     */
    public Collection<AttachmentDescriptor> findAttachments(int sessionId, AttachType type) throws SessionException;

    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     */
    public Collection<AttachmentDescriptor> findAttachments(int sessionId, AppdefEntityID ent, ViewResourceCategory cat)
        throws SessionException;

    public AttachmentDescriptor findAttachment(int sessionId, Integer descId) throws SessionException;

    /**
     * Get an attachment view by ID
     */
    public View findViewById(int sessionId, Integer id);

}