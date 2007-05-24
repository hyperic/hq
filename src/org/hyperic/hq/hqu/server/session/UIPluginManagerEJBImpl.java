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

package org.hyperic.hq.hqu.server.session;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.UIPluginDescriptor;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.hq.hqu.shared.UIPluginManagerUtil;
import java.util.Collection;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;

/**
 * @ejb:bean name="UIPluginManager"
 *      jndi-name="ejb/hqu/UIPluginManager"
 *      local-jndi-name="LocalUIPluginManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class UIPluginManagerEJBImpl 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(UIPluginManagerEJBImpl.class);

    private UIPluginDAO _pluginDAO;
    
    public UIPluginManagerEJBImpl() {
        _pluginDAO = new UIPluginDAO(DAOFactory.getDAOFactory()); 
    }

    /**
     * @ejb:interface-method
     */
    public UIPlugin createPlugin(UIPluginDescriptor pInfo) {
        return _pluginDAO.create(pInfo);
    }
    
    /**
     * @ejb:interface-method
     */
    public Collection findAll() {
        return _pluginDAO.findAll();
    }
    
    public static UIPluginManagerLocal getOne() {
        try {
            return UIPluginManagerUtil.getLocalHome().create();
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
