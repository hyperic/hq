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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.hq.bizapp.server.session.DashboardConfig;
import org.hyperic.hq.bizapp.server.session.UserDashboardConfig;
import org.hyperic.hq.bizapp.server.session.RoleDashboardConfig;
import org.hyperic.hq.bizapp.shared.DashboardManagerUtil;
import org.hyperic.hq.bizapp.shared.DashboardManagerLocal;

/**
 * @ejb:bean name="DashboardManager"
 *      jndi-name="ejb/bizapp/DashboardManager"
 *      local-jndi-name="LocalDashboardManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */

public class DashboardManagerEJBImpl implements SessionBean {

    protected SessionManager _manager = SessionManager.getInstance();

    private DashboardConfigDAO _dashDAO = 
        new DashboardConfigDAO(DAOFactory.getDAOFactory());
    
    /** @ejb:create-method */
    public void ejbCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    /**
     * @ejb:interface-method
     */
    public UserDashboardConfig getUserDashboard(AuthzSubject me,
                                                AuthzSubject user) 
        throws PermissionException
    {
        PermissionManager permMan = PermissionManagerFactory.getInstance();
        
        if (!me.equals(user) && 
            !permMan.hasAdminPermission(me.getAuthzSubjectValue()))
        {
            throw new PermissionException("You are unauthorized to see this " +
                                          "dashboard");
        }
        
        return _dashDAO.findDashboard(user);
    }
    
    /**
     * @ejb:interface-method
     */
    public RoleDashboardConfig getRoleDashboard(AuthzSubject me, Role r)
        throws PermissionException
    {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        permMan.check(me.getId(), r.getResource().getResourceType(),
                      r.getId(), AuthzConstants.roleOpModifyRole);

        return _dashDAO.findDashboard(r);
    }

    private ConfigResponse getDefaultConfig() {
        return new ConfigResponse();
    }
    
    /**
     * @ejb:interface-method
     */
    public UserDashboardConfig createUserDashboard(AuthzSubject me,
                                                   AuthzSubject user,
                                                   String name) 
        throws PermissionException
    {
        PermissionManager permMan = PermissionManagerFactory.getInstance();
        
        if (!me.equals(user) && 
            !permMan.hasAdminPermission(me.getAuthzSubjectValue()))
        {
            throw new PermissionException("You are unauthorized to create " + 
                                          "this dashboard");
        }
        
        Crispo cfg = CrispoManagerEJBImpl.getOne().create(getDefaultConfig());
        UserDashboardConfig dash = new UserDashboardConfig(user, name, cfg);
        _dashDAO.save(dash);
        return dash;
    }

    /**
     * @ejb:interface-method
     */
    public RoleDashboardConfig createRoleDashboard(AuthzSubject me, Role r, 
                                                   String name)
        throws PermissionException
    {
        PermissionManager permMan = PermissionManagerFactory.getInstance();
        
        permMan.check(me.getId(), r.getResource().getResourceType(),
                      r.getId(), AuthzConstants.roleOpModifyRole);
        
        Crispo cfg = CrispoManagerEJBImpl.getOne().create(getDefaultConfig());
        RoleDashboardConfig dash = new RoleDashboardConfig(r, name, cfg);
        _dashDAO.save(dash);
        return dash;
    }

    /**
     * Reconfigure a user's dashboard
     * 
     * @ejb:interface-method
     */
    public void configureDashboard(AuthzSubject me, DashboardConfig cfg, 
                                   ConfigResponse newCfg)
        throws PermissionException
    {
        if (!cfg.isEditable(me)) {
            throw new PermissionException("You are unauthorized to modify " + 
                                          "this dashboard");
        }
        CrispoManagerEJBImpl.getOne().update(cfg.getCrispo(), newCfg);
    }
    
    /**
     * Determine if a dashboard is editable by the passed user
     * 
     * @ejb:interface-method
     */
    public boolean isEditable(AuthzSubject me, DashboardConfig dash) { 
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        if (permMan.hasAdminPermission(me.getAuthzSubjectValue()))  
            return true;

        return dash.isEditable(me);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getDashboards(AuthzSubject me) 
        throws PermissionException
    {
        Collection res = new ArrayList();

        PermissionManager permMan = PermissionManagerFactory.getInstance();
        if (permMan.hasAdminPermission(me.getAuthzSubjectValue())) {
            return _dashDAO.findAll();
        }
        
        UserDashboardConfig cfg = getUserDashboard(me, me);
        if (cfg != null)
            res.add(cfg);
        
        res.addAll(_dashDAO.findRolesFor(me));
        return res;
    }
    
    public static DashboardManagerLocal getOne() {
        try {
            return DashboardManagerUtil.getLocalHome().create();    
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
