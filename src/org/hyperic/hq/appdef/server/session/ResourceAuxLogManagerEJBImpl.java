/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.shared.ResourceAuxLogManagerLocal;
import org.hyperic.hq.appdef.shared.ResourceAuxLogManagerUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;

/**
 * @ejb:bean name="ResourceAuxLogManager"
 *      jndi-name="ejb/common/ResourceAuxLogManager"
 *      local-jndi-name="LocalResourceAuxLogManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ResourceAuxLogManagerEJBImpl 
    implements SessionBean
{
    public static ResourceAuxLogManagerLocal getOne() {
        try {
            return ResourceAuxLogManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    private ResourceAuxLogDAO getDAO() {
        return new ResourceAuxLogDAO(DAOFactory.getDAOFactory()); 
    }
    
    /**
     * @ejb:interface-method
     */
    public ResourceAuxLogPojo create(GalertAuxLog log, ResourceAuxLog logInfo) { 
        ResourceAuxLogPojo resourceLog = 
            new ResourceAuxLogPojo(log, logInfo, log.getAlert().getAlertDef());
        
        getDAO().save(resourceLog);
        return resourceLog;
    }
    
    /**
     * @ejb:interface-method
     */
    public void remove(GalertAuxLog log) { 
        getDAO().remove(getDAO().find(log));
    }

    /**
     * @ejb:interface-method
     */
    public ResourceAuxLogPojo find(GalertAuxLog log) { 
        return getDAO().find(log);
    }

    /**
     * @ejb:interface-method
     */
    public void removeAll(GalertDef def) {
        getDAO().removeAll(def);
    }

    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
