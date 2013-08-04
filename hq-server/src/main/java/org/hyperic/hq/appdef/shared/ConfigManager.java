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
package org.hyperic.hq.appdef.shared;

import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AllConfigDiff;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * Local interface for ConfigManager.
 */
public interface ConfigManager {

    public ConfigResponseDB createConfigResponse(byte[] productResponse, byte[] measResponse, byte[] controlResponse,
                                                 byte[] rtResponse);

    /**
     * Get the ConfigResponse for the given ID, creating it if it does not
     * already exist.
     */
    public ConfigResponseDB getConfigResponse(AppdefEntityID id);

    public String getPluginName(AppdefEntityID id) throws AppdefEntityNotFoundException;

    /**
     * Get a config response object merged through the hierarchy. All entities
     * are merged with the product's config response, and any entity lower than
     * them in the config stack. Config responses defining a specific attribute
     * will override the same attribute if it was declared lower in the
     * application stack. Only entities within the same plugin will be
     * processed, so the most likely situation is a simple service + server +
     * product or server + product merge. Example: Get the SERVICE MEASUREMENT
     * merged response: PRODUCT[platform] + MEASUREMENT[platform]
     * PRODUCT[server] + MEASUREMENT[server] + PRODUCT[service] +
     * MEASUREMENT[service] Get the SERVER PRODUCT merged response:
     * PRODUCT[platform] PRODUCT[server] Get the PLATFORM PRODUCT merged
     * response: PRODUCT[platform] In addition to the configuration, some
     * inventory properties are also merged in to aid in auto-configuration done
     * by autoinventory. For Servers and Services: The install path of the
     * server is included For all Resources: The first non-loopback ip address,
     * fqdn, platform name and type.
     * @param productType One of ProductPlugin.*
     * @param id An AppdefEntityID of the object to get config for
     * @return the merged ConfigResponse
     */
    public ConfigResponse getMergedConfigResponse(AuthzSubject subject, String productType, AppdefEntityID id,
                                                  boolean required) throws AppdefEntityNotFoundException,
        ConfigFetchException, EncodingException, PermissionException;

    /**
     * Clear the validation error string for a config response, indicating that
     * the current config is valid
     */
    public void clearValidationError(AuthzSubject subject, AppdefEntityID id);

    /**
     * Update the validation error string for a config response
     * @param validationError The error string that occured during validation.
     *        If this is null, that means that no error occurred and the config
     *        is valid.
     */
    public void setValidationError(AuthzSubject subject, AppdefEntityID id, String validationError);

    /**
     * Set the config response for an entity/type combination.
     * @param id ID of the object to set the repsonse fo
     * @param response The response
     * @param type One of ProductPlugin.TYPE_
     * @param userManaged is true if configured by user directly and should 
     *        override previous configuration 
     * @return an array of entities which may be affected by the change in
     *         configuration. For updates to platform and service configs, there
     *         are no other entities other than the given ID returned. If a
     *         server is updated, the associated services may require changes.
     *         The passed entity will always be returned in the array.
     */
    public AppdefEntityID setConfigResponse(AuthzSubject subject, AppdefEntityID id, ConfigResponse response,
                                            String type, boolean userManaged, boolean sendConfigEvent) throws ConfigFetchException,
        AppdefEntityNotFoundException, PermissionException, EncodingException;

    public boolean configureResponse(AuthzSubject subject, ConfigResponseDB existingConfig,
                                     AppdefEntityID appdefID, byte[] productConfig, byte[] measurementConfig,
                                     byte[] controlConfig, byte[] rtConfig, Boolean userManaged,
                                     boolean force);

    ConfigDiff configureResponseDiff(AuthzSubject subject, ConfigResponseDB existingConfig, AppdefEntityID appdefID,
            byte[] productConfig, byte[] measurementConfig, byte[] controlConfig, byte[] rtConfig, Boolean userManaged,
            boolean force) throws EncodingException;
    
    interface ConfigDiff {
        boolean isWasUpdated();
        void setWasUpdated(boolean wasUpdated);
        AllConfigDiff getAllConfigDiff();
        void setAllConfigDiff(AllConfigDiff allConfigResponses);
    }

    /**
     * Fetches all {@link ConfigResponse} objects associated with the resources param
     * @param hideSecrets ***s out config values which are marked as secret=true
     * @return {@link Map} of {@link Resource} to its associated {@link ConfigResponse}
     */
    public Map<Resource, ConfigResponse> getConfigResponses(Set<Resource> resources, boolean hideSecrets);
}
