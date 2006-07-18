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

package org.hyperic.hq.authz.server.session;

import java.util.Iterator;
import java.util.Collection;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.authz.server.session.VOCache;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourcePK;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.ResourceTypePK;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.OperationLocal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @ejb:bean name="ResourceVOHelper"
 *      jndi-name="ejb/authz/ResourceVOHelper"
 *      local-jndi-name="LocalResourceVOHelper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class ResourceVOHelperEJBImpl extends AuthzSession 
    implements SessionBean {

    private Log log = LogFactory.getLog(
        "org.hyperic.hq.authz.server.session.ResourceVOHelperEJBImpl");
    private SessionContext myCtx;

    /**
     * Get the resource value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     */    
    public ResourceValue getResourceValue(ResourcePK pk) 
        throws FinderException, NamingException {
        ResourceValue vo = VOCache.getInstance().getResource(pk.getId());
        if(vo != null) {
            log.debug("Returning cached instance for resource: " + vo.getId());
            return vo;            
        }

        ResourceLocal ejb = ResourceUtil.getLocalHome().findByPrimaryKey(pk);
        return getResourceValueImpl(ejb);
    }
            
    /**
     * Get the server value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     */    
    public ResourceValue getResourceValue(ResourceLocal ejb) 
        throws FinderException, NamingException {
        ResourcePK pk = (ResourcePK)ejb.getPrimaryKey();
        ResourceValue vo = VOCache.getInstance().getResource(pk.getId());
        if(vo != null) {
            log.debug("Returning cached instance for resource: " + vo.getId());
            return vo;            
        }
        return getResourceValueImpl(ejb);
    }

    /**
     * Sunchronized VO retrieval
     */
    private ResourceValue getResourceValueImpl(ResourceLocal ejb) 
        throws FinderException, NamingException {
        VOCache cache = VOCache.getInstance();
        ResourceValue vo;
        synchronized(cache.getResourceLock()) {
            ResourcePK pk = (ResourcePK)ejb.getPrimaryKey();
            vo = cache.getResource(pk.getId());
            if(vo != null) {
                log.debug("Returning cached instance for resource: " + vo.getId());
                return vo;
            }
            vo = ejb.getResourceValueObject();
            // now do the resource type... this needs its own invocation to avoid
            // self deadlocks
            vo.setResourceTypeValue(getResourceTypeValue(ejb.getResourceType().getName()));
            cache.put(vo.getId(), vo);
        }
        return vo;
    }

    /**
     * Get the resource type value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceTypeValue getResourceTypeValue(ResourceTypePK pk)
        throws FinderException, NamingException {
        ResourceTypeLocal ejb = ResourceTypeUtil.getLocalHome().findByPrimaryKey(pk);
        return getResourceTypeValue(ejb.getName());
    }

    /**
     * Get the resource type value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceTypeValue getResourceTypeValue(String name) 
        throws FinderException, NamingException {
        ResourceTypeValue vo = VOCache.getInstance().getResourceType(name);
        if(vo != null) {
            log.debug("Returning cached instance for resource type: " + vo.getId());
            return vo;
        }
        return getResourceTypeValueImpl(name);
    }

    /**
     * Synchronized VO retrieval
     */
    private ResourceTypeValue getResourceTypeValueImpl(String name) 
        throws FinderException, NamingException {
        VOCache cache = VOCache.getInstance();
        ResourceTypeValue vo;
        synchronized(cache.getResourceTypeLock()) {
            vo = VOCache.getInstance().getResourceType(name);
            if(vo != null) {
                log.debug("Returning cached instance for resource type: " + vo.getId());
                return vo;
            }
        /**
            Instead of fetching these one by one, we're gonna go ahead and get them all
            since this data never changes
        **/
            Collection rts = ResourceTypeUtil.getLocalHome().findAll();
            for(Iterator j = rts.iterator(); j.hasNext();) {
                ResourceTypeLocal anEjb = (ResourceTypeLocal)j.next();
                ResourceTypeValue aVo = anEjb.getResourceTypeValueObject();
                for(Iterator i = anEjb.getOperationsSnapshot().iterator(); i.hasNext();) {
                    OperationLocal opEjb = (OperationLocal)i.next();
                    aVo.addOperationValue(opEjb.getOperationValue());
                }
                cache.put(aVo.getName(), aVo);
                if (aVo.getName().equals(name)) {
                    vo = aVo;
                }
            }
        }
        return vo;
    }

    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }

    public void setSessionContext(SessionContext ctx) {
        myCtx = ctx;
    }

    public SessionContext getSessionContext() {
        return myCtx;
    }
}
