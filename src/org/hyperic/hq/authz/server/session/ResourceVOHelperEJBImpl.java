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

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.Operation;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;

/**
 *
 * @ejb:bean name="ResourceVOHelper"
 *      jndi-name="ejb/authz/ResourceVOHelper"
 *      local-jndi-name="LocalResourceVOHelper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * 
 * @ejb:transaction type="REQUIRED" 
 */
public class ResourceVOHelperEJBImpl extends AuthzSession 
    implements SessionBean {

    private Log log = LogFactory.getLog(
        "org.hyperic.hq.authz.server.session.ResourceVOHelperEJBImpl");
    private SessionContext myCtx;

    /**
     * Get the resource value object
     * @ejb:interface-method
     */    
    public ResourceValue getResourceValue(Integer id) {
        ResourceValue vo = VOCache.getInstance().getResource(id);
        if(vo != null) {
            log.debug("Returning cached instance for resource: " + vo.getId());
            return vo;            
        }

        Resource pojo= DAOFactory.getDAOFactory().getResourceDAO().findById(id);
        return getResourceValueImpl(pojo);
    }
            
    /**
     * Get the server value object
     * @ejb:interface-method
     */    
    public ResourceValue getResourceValue(Resource pojo)  {
        ResourceValue vo = VOCache.getInstance().getResource(pojo.getId());
        if(vo != null) {
            log.debug("Returning cached instance for resource: " + vo.getId());
            return vo;            
        }
        return getResourceValueImpl(pojo);
    }

    /**
     * Sunchronized VO retrieval
     */
    private ResourceValue getResourceValueImpl(Resource pojo) {
        VOCache cache = VOCache.getInstance();
        ResourceValue vo;
        synchronized(cache.getResourceLock()) {
            vo = cache.getResource(pojo.getId());
            if(vo != null) {
                log.debug("Returning cached instance for resource: " +
                          vo.getId());
                return vo;
            }
            vo = pojo.getResourceValue();
            
            // now do the resource type... this needs its own invocation to
            // avoid self deadlocks
            ResourceTypeValue tvo = cache.getResourceType(
                pojo.getResourceType().getName());
            if(tvo == null) {
                tvo = pojo.getResourceType().getResourceTypeValue();
                cache.put(tvo.getName(), tvo);
            }

            vo.setResourceTypeValue(tvo);
            cache.put(vo.getId(), vo);
        }
        return vo;
    }

    /**
     * Get the resource type value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceTypeValue getResourceTypeValue(String name) {
        ResourceTypeValue vo = VOCache.getInstance().getResourceType(name);
        if(vo != null) {
            log.debug("Returning cached instance for resource type: " +
                      vo.getId());
            return vo;
        }
        
        VOCache cache = VOCache.getInstance();
        ResourceTypeValue vo1;
        synchronized(cache.getResourceTypeLock()) {
            vo1 = VOCache.getInstance().getResourceType(name);
            if(vo1 != null) {
                log.debug("Returning cached instance for resource type: " +
                          vo1.getId());
                return vo1;
            }
            /**
             * Instead of fetching these one by one, we're gonna go ahead and
             * get them all since this data never changes
             */
            Collection rts = getResourceTypeDAO().findAll();
            for(Iterator j = rts.iterator(); j.hasNext();) {
                ResourceType anEjb = (ResourceType)j.next();
                ResourceTypeValue aVo = anEjb.getResourceTypeValue();
                for(Iterator i = anEjb.getOperations().iterator();
                    i.hasNext();) {
                    Operation opEjb = (Operation)i.next();
                    aVo.addOperationValue(opEjb.getOperationValue());
                }
                cache.put(aVo.getName(), aVo);
                if (aVo.getName().equals(name)) {
                    vo1 = aVo;
                }
            }
        }
        return vo1;
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
