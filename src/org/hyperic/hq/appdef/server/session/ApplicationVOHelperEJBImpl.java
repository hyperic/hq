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

import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.Application;
import org.hyperic.hq.common.SystemException;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="ApplicationVOHelper"
 *      jndi-name="ejb/appdef/ApplicationVOHelper"
 *      local-jndi-name="LocalApplicationVOHelper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class ApplicationVOHelperEJBImpl extends AppdefSessionEJB 
    implements SessionBean {
    
    private Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.session.ApplicationVOHelperEJBImpl");
    /**
     * Get a value object for this application
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ApplicationValue getApplicationValue(ApplicationPK apk) throws FinderException,
        NamingException {
            ApplicationValue vo = VOCache.getInstance().getApplication(apk.getId());
            if(vo != null) {
                return vo;
            }
            Application ejb = getApplicationDAO().findByPrimaryKey(apk);
            return getApplicationValue(ejb);
    }
                
    /**
     * Get a value object for this application
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ApplicationValue getApplicationValue(Application ejb)
        throws NamingException {
        ApplicationValue vo = VOCache.getInstance()
            .getApplication(ejb.getId());
        if(vo != null) {
            log.debug("Returning cached application: " + vo.getId());
            return vo;            
        }
        return getApplicationValueImpl(ejb);
    }

    /**
     * synchronized method to do the actual retrieval of the valie object
     */
    private ApplicationValue getApplicationValueImpl(Application ejb)
        throws NamingException {
        VOCache cache = VOCache.getInstance();
        ApplicationValue vo;
        synchronized(cache.getApplicationLock()) {
            // try to get the VO again  
            vo = VOCache.getInstance()
                .getApplication(((ApplicationPK)ejb.getPrimaryKey()).getId());
            if(vo != null) {
                log.debug("Returning cached application: " + vo.getId());
                return vo;
            }
            // first get the flat vo
            vo = ejb.getApplicationValueObject();
            Iterator asIt = ejb.getAppServiceSnapshot().iterator();
            while (asIt.hasNext()){
                try {
                    vo.addAppServiceValue( ((AppService)asIt.next()).getAppServiceValue() );
                } catch (NoSuchObjectLocalException e) {
                // the app service was removed during our iteration
                // not a problem.
                }
            }
            cache.put(vo.getId(), vo);
        }
        return vo;
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
