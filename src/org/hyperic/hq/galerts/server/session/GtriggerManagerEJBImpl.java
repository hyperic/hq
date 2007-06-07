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

package org.hyperic.hq.galerts.server.session;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;
import org.hyperic.hq.galerts.shared.GtriggerManagerLocal;
import org.hyperic.hq.galerts.shared.GtriggerManagerUtil;

/**
 * @ejb:bean name="GtriggerManager"
 *      jndi-name="ejb/galerts/GtriggerManager"
 *      local-jndi-name="LocalGtriggerManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class GtriggerManagerEJBImpl 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(GtriggerManagerEJBImpl.class);

    private GtriggerTypeInfoDAO _ttypeDAO;
    
    public GtriggerManagerEJBImpl() {
        _ttypeDAO = new GtriggerTypeInfoDAO(DAOFactory.getDAOFactory()); 
    }

    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo findTriggerType(GtriggerType type) {
        return _ttypeDAO.find(type);
    }
    
    /**
     * Register a trigger type.  
     * 
     * @param triggerType Trigger type to register
     * @return the persisted metadata about the trigger type
     * @ejb:interface-method
     */
    public GtriggerTypeInfo registerTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo res;
     
        res = _ttypeDAO.find(triggerType);
        if (res != null) {
            _log.warn("Attempted to register GtriggerType class [" + 
                      triggerType.getClass() + "] but it was already " + 
                      "registered");
            return res;
        }
        res = new GtriggerTypeInfo(triggerType.getClass());
        _ttypeDAO.save(res);
        return res;
    }
    
    /**
     * Unregister a trigger type.  This method will fail if any alert
     * definitions are using triggers of this type.
     * 
     * @param triggerType Trigger type to unregister
     * @ejb:interface-method
     */
    public void unregisterTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo info = _ttypeDAO.find(triggerType);
        
        if (info == null) {
            _log.warn("Tried to unregister a trigger type which was not " + 
                      "registered");
            return;
        }
            
        _ttypeDAO.remove(info);
    }
    
    public static GtriggerManagerLocal getOne() {
        try {
            return GtriggerManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
