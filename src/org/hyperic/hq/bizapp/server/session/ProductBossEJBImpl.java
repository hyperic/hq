/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ProductBossLocal;
import org.hyperic.hq.bizapp.shared.ProductBossUtil;
import org.hyperic.hq.bizapp.server.session.ProductBossEJBImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.file.FileUtil;

/**
 * The Product Boss
 *
 * @ejb:bean name="ProductBoss"
 *      jndi-name="ejb/bizapp/ProductBoss"
 *      local-jndi-name="LocalProductBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 */
public class ProductBossEJBImpl extends BizappSessionEJB implements SessionBean
{
    private Log log = LogFactory.getLog(ProductBossEJBImpl.class.getName());

    private SessionManager sessionManager = SessionManager.getInstance();

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}

    /**
     * Get the merged config responses for group entries.  This routine
     * has the same functionality as getMergedConfigResponse, except it
     * takes in a groupId and returns multiple configResponse objects --
     * 1 for each entity in the group.
     *
     * @param productType one of ProductPlugin.TYPE_*
     * @param groupId     ID of the group to get configs for
     * @param required    If true, all the entities required to make a 
     *                    merged config response must exist.  Else
     *                    as many values as can be gotten are tried.
     * @ejb:interface-method
     */
    public ConfigResponse[] 
        getMergedGroupConfigResponse(int sessionId, String productType, 
                                     int groupId, boolean required)
        throws AppdefEntityNotFoundException, PermissionException, 
               ConfigFetchException, SessionNotFoundException, 
               SessionTimeoutException, EncodingException
    {
        AuthzSubjectValue subject;
        AppdefGroupValue group;
        ConfigResponse[] res;
        List members;
        int idx;

        // validate the session
        subject = sessionManager.getSubject(sessionId);
        group   = this.getAppdefGroupManager().findGroup(subject, 
                                                         new Integer(groupId));

        // use the overlord to pull the merge
        // FIXME - this is a pretty ugly compromise.
        subject = this.getOverlord();

        members = group.getAppdefGroupEntries();
        res     = new ConfigResponse[members.size()];
        idx     = 0;
        for(Iterator i=members.iterator(); i.hasNext(); ){
            AppdefEntityID id = (AppdefEntityID)i.next();

            res[idx++] = this.getMergedConfigResponse(subject, productType,
                                                      id, required);
        }
        
        return res;
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
     * Additionally, due to restrictions in the authz model, this merge
     * is performed as the overlord user regardless of the caller. The reason
     * for this is that the caller may not have view access to the entire
     * hierarchy, but still wants to view the merged configuration. This
     * will need to be reviewed post-release
     *
     * @param productType one of ProductPlugin.TYPE_*
     * @param id          Entity to get config for
     * @param required    If true, all the entities required to make a 
     *                    merged config response must exist.  Else
     *                    as many values as can be gotten are tried.
     * @ejb:interface-method
     */
    public ConfigResponse getMergedConfigResponse(int sessionId,
                                                  String productType,
                                                  AppdefEntityID id,
                                                  boolean required)
        throws AppdefEntityNotFoundException, EncodingException,
               PermissionException, ConfigFetchException, 
               SessionNotFoundException, SessionTimeoutException {

        // validate the session
        sessionManager.getSubject(sessionId);
        // use the overlord to pull the merge
        // FIXME - this is a pretty ugly compromise.
        return this.getMergedConfigResponse(getOverlord(), productType, id, 
                                            required);
    }

    /**
     * @ejb:interface-method view-type="local"
     */
    public ConfigResponse getMergedConfigResponse(AuthzSubjectValue subject,
                                                  String productType,
                                                  AppdefEntityID id,
                                                  boolean required)
        throws AppdefEntityNotFoundException, PermissionException, 
               ConfigFetchException, EncodingException
    {
        ConfigManagerLocal cman;

        // Get the merged config
        cman = this.getConfigManager();
        return cman.getMergedConfigResponse(subject, productType, id, 
                                            required);
    }

    /**
     * @ejb:interface-method
     */
    public ConfigResponseDB getConfigResponse(int sessionId,
                                              AppdefEntityID id)
        throws AppdefEntityNotFoundException, 
               SessionNotFoundException, SessionTimeoutException
    {
        sessionManager.getSubject(sessionId);
        return this.getConfigManager().getConfigResponse(id);
    }

    /**
     * @ejb:interface-method 
     */
    public String getMonitoringHelp(int sessionId, AppdefEntityID id,
                                    Map props)
        throws PluginNotFoundException, PermissionException,
               AppdefEntityNotFoundException, SessionNotFoundException,
               SessionTimeoutException
    {
        AuthzSubjectValue subject =
            sessionManager.getSubject(sessionId);
        AppdefEntityValue aval = new AppdefEntityValue(id, getOverlord());
        return this.getTemplateManager().getMonitoringHelp(subject, aval,
                                                           props);
    }

    /**
     * Get the config schema used to configure an entity.  If the appropriate
     * base entities have not yet been configured, an exception will be thrown
     * indicating which resource must be configured.
     *
     * @ejb:interface-method
     */
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, 
                                        String type, ConfigResponse resp)
        throws SessionTimeoutException, SessionNotFoundException,
               PluginException, PermissionException,
               AppdefEntityNotFoundException {
       sessionManager.getSubjectPojo(sessionId);
        return this.getConfigSchema(id, type, resp);
    }

    /**
     * @ejb:interface-method 
     */
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id, 
                                        String type)
        throws ConfigFetchException, FinderException, EncodingException,
               PluginNotFoundException, PluginException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, AppdefEntityNotFoundException
    {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);
        return this.getConfigSchema(subject, id, type, true);
    }

    public static class ConfigSchemaAndBaseResponse {
        private ConfigSchema   _schema;
        private ConfigResponse _response;
        
        ConfigSchemaAndBaseResponse(ConfigSchema schema, ConfigResponse resp) {
            _schema   = schema;
            _response = resp;
        }
        
        public ConfigSchema getSchema() {
            return _schema;
        }
        
        public ConfigResponse getResponse() {
            return _response;
        }
    }
    
    /**
     * Get a configuration schema.  
     *
     * @param id           Entity to be configured
     * @param type         One of ProductPlugin.TYPE_*
     * @param validateFlow If true a ConfigFetchException will be thrown
     *                     if the appropriate base entities are not
     *                     already configured.
     *
     * @ejb:interface-method view-type="local"
     */
    public ConfigSchemaAndBaseResponse
        getConfigSchemaAndBaseResponse(AuthzSubjectValue subject, 
                                       AppdefEntityID id, 
                                       String type,
                                       boolean validateFlow)
        throws ConfigFetchException, EncodingException,
               PluginNotFoundException, PluginException, PermissionException,
               AppdefEntityNotFoundException
    {
        ConfigManagerLocal  cman;
        ConfigResponse      baseResponse = null;

        cman = this.getConfigManager();
        if(validateFlow == true){
            try {
                baseResponse =
                    cman.getMergedConfigResponse(getOverlord(), type, id, true);
            } catch(ConfigFetchException exc){
                // If the thing that failed is the thing we are trying to
                // configure, then everything is okey-dokey ... else
                if(!exc.matchesQuery(id, type)){
                    throw exc;
                }
            }
        }

        if (baseResponse == null)
            baseResponse = cman.getMergedConfigResponse(getOverlord(), type, id, 
                                                        false);

        return new ConfigSchemaAndBaseResponse(getConfigSchema(id, type, 
                                                               baseResponse),
                                               baseResponse);
    }

    /**
     * @ejb:interface-method
     */
    public ConfigSchema getConfigSchema(AuthzSubjectValue subject, 
                                        AppdefEntityID id, String type,
                                        boolean validateFlow)
        throws ConfigFetchException, EncodingException,
               PluginNotFoundException, PluginException, PermissionException,
               AppdefEntityNotFoundException
    {
        return getConfigSchemaAndBaseResponse(subject, id, type, 
                                              validateFlow).getSchema();
    }
    
    /**
     * Get a configuration schema.  
     *
     * @param id           Entity to be configured
     * @param type         One of ProductPlugin.TYPE_*
     * @param baseResponse the response object of the given type
     */
    private ConfigSchema getConfigSchema(AppdefEntityID id, String type,
                                         ConfigResponse baseResponse)
        throws PluginException, PermissionException,
               AppdefEntityNotFoundException {

        String name;
        if (type.equals(ProductPlugin.TYPE_PRODUCT)) {
            name = ConfigManagerEJBImpl.getOne().getPluginName(id);
        }
        else {
            name = getPlatformManager().getPlatformPluginName(id);
        }

        AppdefEntityValue aval = new AppdefEntityValue(id, getOverlord());

        return getProductManager().getConfigSchema(type, name, aval,
                                                   baseResponse);
    }

    /**
     * Set the config response for an entity/type combination.
     * Note that setting the config response for any entity may
     * cause a chain reaction of things to occur.  For instance,
     * agents may get updated with new measurements for entities 
     * which were affected by the configuration change.
     *
     * @param id       ID of the object to set the repsonse fo
     * @param response The response
     * @param type     One of ProductPlugin.TYPE_*
     * @throws SessionTimeoutException 
     * @throws SessionNotFoundException 
     *
     * @ejb:interface-method
     */
    public void setConfigResponse(int sessionId, AppdefEntityID id,
                                  ConfigResponse response, String type)
        throws FinderException, InvalidConfigException, SessionTimeoutException,
               EncodingException, PermissionException, ConfigFetchException,
               AppdefEntityNotFoundException, SessionNotFoundException
    {
        AuthzSubject subject = sessionManager.getSubjectPojo(sessionId);
        this.setConfigResponse(subject, id, response, type);
    }

    /**
     * @ejb:interface-method view-type="local"
     */
    public void setConfigResponse(AuthzSubject subject, AppdefEntityID id,
                                  ConfigResponse response, String type)
        throws EncodingException, FinderException, PermissionException,
               InvalidConfigException, ConfigFetchException,
               AppdefEntityNotFoundException
    {
        this.setConfigResponse(subject, id, response, type, true);
    }

    /**
     * @return The array of IDs affected.
     */
    private AppdefEntityID[] setConfigResponse(AuthzSubject subject, 
                                               AppdefEntityID id,
                                               ConfigResponse response, 
                                               String type,
                                               boolean shouldValidate)
        throws EncodingException, FinderException, PermissionException,
               InvalidConfigException, ConfigFetchException,
               AppdefEntityNotFoundException
    {
        ConfigManagerLocal cMan;
        AppdefEntityID[] ids;
        boolean doRollback = true;
        try {
            cMan = this.getConfigManager();
            ids  = cMan.setConfigResponse(subject.getAuthzSubjectValue(), id,
                                          response, type, true);
            
            if (shouldValidate) {
                doValidation(subject, type, ids);
            }
            doRollback = false;
            return ids;

        } finally {
            if (doRollback) {
                rollback();
            }
        }
    }

    /**
     * @ejb:interface-method view-type="local"
     */
    public void doValidation(AuthzSubject subject, String type,
                             AppdefEntityID[] ids)
        throws PermissionException, EncodingException, ConfigFetchException,
               AppdefEntityNotFoundException, InvalidConfigException {

        ConfigValidator configValidator = (ConfigValidator) ProductProperties
                .getPropertyInstance("hyperic.hq.bizapp.configValidator");
        
        if (configValidator != null)
            configValidator.validate(subject, type, ids);
    }

    /**
     * Gets the version number
     * @ejb:interface-method
     */
    public String getVersion(){
        return ProductProperties.getVersion();
    }

    /**
     * Gets the build number, date, and type.
     * @ejb:interface-method
     */
    public String getBuildNumber(){
        String build = ProductProperties.getBuild();
        String buildDate = ProductProperties.getBuildDate();
        String comment = ProductProperties.getComment();

        return "(build #" + build + " - " + buildDate + " - " + comment + ")";
    }
    
    /**
     * Preload the 2nd level caches
     * @ejb:interface-method
     */
    public void preload(){
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = 
            loader.getResourceAsStream("META-INF/preload_caches.txt"); 
        List lines;
            
        try {
            lines = FileUtil.readLines(is);
        } catch(IOException e) {
            log.warn("Unable to preload.  IO exception reading " + 
                     "preload_caches.txt", e);
            return;
        } finally {
            try { is.close(); } catch(Exception e) {}
        }
            
        Session s = Util.getSessionFactory().getCurrentSession();
        for (Iterator i=lines.iterator(); i.hasNext(); ) {
            String className = (String)i.next();
            long start, end;
            Collection vals;
            Class c;
            
            className = className.trim();
            if (className.length() == 0 || className.startsWith("#"))
                continue;
            
            try {
                c = Class.forName(className);
            } catch(Exception e) {
                log.warn("Unable to find preload cache for class [" + 
                         className + "]", e);
                continue;
            }
            
            start = System.currentTimeMillis();
            vals  = s.createCriteria(c).list();
            end   = System.currentTimeMillis();
            log.info("Preloaded " + vals.size()+ " [" + c.getName() + 
                     "] in " + (end - start) + " millis");
            
            // Evict, to avoid dirty checking everything in the inventory
            for (Iterator j=vals.iterator(); j.hasNext(); ) {
                s.evict(j.next());
            }
        }
    }
     
    /**
     * Clear out all the caches
     * @ejb:interface-method
     */
    public void clearCaches(int sessionId) {
        CacheManager cacheManager = CacheManager.getInstance();
        
        cacheManager.clearAll();
    }
    
    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     * @ejb:interface-method
     */
    public Collection findAttachments(int sessionId, AttachType type) 
        throws SessionException
    {
        AuthzSubject subject = sessionManager.getSubjectPojo(sessionId);
        
        return UIPluginManagerEJBImpl.getOne().findAttachments(type, subject);
    }
    
    /**
     * Find {@link AttachmentDescriptor}s attached to the target type
     * @ejb:interface-method
     */
    public Collection findAttachments(int sessionId, AppdefEntityID ent,
                                      ViewResourceCategory cat) 
        throws SessionException
    {
        AuthzSubject subject = sessionManager.getSubjectPojo(sessionId);
        
        return UIPluginManagerEJBImpl.getOne().findAttachments(ent, cat, 
                                                               subject);
    }

    /**
     * @ejb:interface-method
     */
    public AttachmentDescriptor findAttachment(int sessionId, Integer descId) 
        throws SessionException
    {
        AuthzSubject subject = sessionManager.getSubjectPojo(sessionId);
        
        return UIPluginManagerEJBImpl.getOne()
                    .findAttachmentDescriptorById(descId, subject);
    }
    
    /**
     * Get an attachment view by ID
     * @ejb:interface-method
     */
    public View findViewById(int sessionId, Integer id) {
        return UIPluginManagerEJBImpl.getOne().findViewById(id);
    }
    
    public static ProductBossLocal getOne() {
        try {
            return ProductBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
}
