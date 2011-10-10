/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * Factory bean used to initialize instance of {@link CacheDataManager}. This
 * factory always returns a singleton implementation of {@link EhCacheDataManager}
 * if configuration is correct.
 * <p>
 * Instead of returning instance of
 * {@link org.springframework.beans.factory.FactoryBean}
 * during the bean creation, instance of {@code getObject} is used. 
 * <p>
 * Example bean configuration:
 * <p><blockquote><pre>
 * {@code
 * <bean id="cacheDataManager" class="org.hyperic.hq.cache.CacheDataManagerBean">
 *   <property name="provider" value="${server.cache.provider:ehcache}"/>
 * </bean>
 * }
 * </pre></blockquote>
 * <p>
 * 
 */
public class CacheDataManagerBean implements FactoryBean<CacheDataManager> {

    private static final Log log = 
        LogFactory.getLog(CacheDataManager.class);
    
    /**
     * Provider type set from spring configuration. Currently either 'ehcache' or 'gemfire'.
     * <b>NOTE: gemfire is not supported by open source version</b>
     */
    protected String provider = null;
    
    /** Cached instance for singleton access. */
    private EhCacheDataManager ehCacheDataManagerInstance = null; 

    /**
     * Returns singleton instance of {@link EhCacheDataManager}
     * 
     * @return Instance of {@link CacheDataManager}
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public CacheDataManager getObject() throws Exception {        
        if(provider != null && provider.equals(CacheDataManager.TYPE_EHCACHE)) {
            if(ehCacheDataManagerInstance == null) {                
                if(log.isDebugEnabled()) log.debug("Creating new singleton for EhCacheDataManager");
                ehCacheDataManagerInstance = new EhCacheDataManager();
            }
            if(log.isDebugEnabled()) log.debug("Returning provider for ehcache");
            return ehCacheDataManagerInstance;
        }
        throw new IllegalStateException((provider==null?"Provider is not sett":"Provider " + provider + " is not supported"));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class<CacheDataManager> getObjectType() {
        return CacheDataManager.class;
    }

    /**
     * We always return a singleton which is a hint to app context to
     * cache returned instance. Since this is not guaranteed singleton
     * is also cached to this factory.
     * 
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * Set a provider type of this factory.
     * 
     * @param provider Provider type
     */
    @Required
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
}
    