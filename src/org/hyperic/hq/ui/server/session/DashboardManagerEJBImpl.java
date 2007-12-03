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

package org.hyperic.hq.ui.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleRemoveCallback;
import org.hyperic.hq.authz.server.session.RoleRemoveFromSubjectCallback;
import org.hyperic.hq.authz.server.session.SubjectRemoveCallback;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.hq.common.server.session.CrispoOption;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.shared.DashboardManagerUtil;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * @ejb:bean name="DashboardManager"
 *      jndi-name="ejb/bizapp/DashboardManager"
 *      local-jndi-name="LocalDashboardManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */

public class DashboardManagerEJBImpl implements SessionBean {

    private static Log _log = LogFactory.getLog(DashboardManagerEJBImpl.class);

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
        
        if (!me.equals(user) && !permMan.hasAdminPermission(me.getId()))
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
        
        if (!me.equals(user) && !permMan.hasAdminPermission(me.getId()))
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
        if (!isEditable(me, cfg)) {
            throw new PermissionException("You are unauthorized to modify " + 
                                          "this dashboard");
        }
        CrispoManagerEJBImpl.getOne().update(cfg.getCrispo(), newCfg);
    }
    
    /**
     * @ejb:interface-method
     */
    public void renameDashboard(AuthzSubject me, DashboardConfig cfg, 
                                String name)  
        throws PermissionException
    {
        if (!isEditable(me, cfg)) {
            throw new PermissionException("You are unauthorized to modify " + 
                                          "this dashboard");
        }
        cfg.setName(name);
    }
    
    /**
     * Determine if a dashboard is editable by the passed user
     * 
     * @ejb:interface-method
     */
    public boolean isEditable(AuthzSubject me, DashboardConfig dash) { 
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        if (permMan.hasAdminPermission(me.getId()))  
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
        if (permMan.hasGuestRole() &&
            permMan.hasAdminPermission(me.getId())) {
            res.addAll(_dashDAO.findAllRoleDashboards());
            res.add(getUserDashboard(me, me));
            return res; 
        }
        
        UserDashboardConfig cfg = getUserDashboard(me, me);
        if (cfg != null)
            res.add(cfg);
        
        if (permMan.hasGuestRole())
            res.addAll(_dashDAO.findRolesFor(me));
        
        return res;
    }

    /**
     * Update dashboard and user configs to account for resource deletion
     * 
     * @param opts The set of user or dashboard properties to check
     * @param ids An array of ID's of removed resources
     * @ejb:interface-method
     */
    public void handleResourceDelete(Set opts, AppdefEntityID[] ids) {
        CrispoManagerLocal cm = CrispoManagerEJBImpl.getOne();

        for (Iterator i = opts.iterator(); i.hasNext(); ) {
            String opt = (String)i.next();
            List copts = cm.findOptionByKey(opt);

            for (Iterator j = copts.iterator(); j.hasNext(); ) {
                CrispoOption o = (CrispoOption)j.next();
                String val = o.getValue();
                String newVal = removeResources(ids, val);

                if (!val.equals(newVal)) {
                    cm.updateOption(o, newVal);
                    _log.debug("Update option key=" + o.getKey() +
                               " old =" + val + " new =" + newVal);
                }
            }
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public ConfigResponse getRssUserPreferences(String user, String token)
        throws LoginException {
        ConfigResponse preferences;
        try {
            AuthzSubject me = AuthzSubjectManagerEJBImpl.getOne()
                .findSubjectByName(user);
            preferences = getUserDashboard(me, me).getConfig();
        }
        catch (Exception e) {
            throw new LoginException("Username has no preferences");
        }
    
        // Let's make sure that the rss auth token matches
        String prefToken = preferences.getValue(Constants.RSS_TOKEN);
        if (token == null || !token.equals(prefToken))
            throw new LoginException("Username and Auth token do not match");
        
        return preferences;
    }    

    /**
     * Yanked from DashboardUtils so we don't need to include anything other
     * than server and session in the server hq.jar
     */
    private String removeResources(AppdefEntityID[] ids, String val) {
	    for (int i = 0; i < ids.length; i++) {
	        String resource = ids[i].getAppdefKey();
	        val = StringUtil.remove(val, resource);
	        val = StringUtil.replace(val, Constants.EMPTY_DELIMITER,
                                     Constants.DASHBOARD_DELIMITER);
	    }

	    return val;
    }

    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Dashboard Manager starting up");
        
        HQApp.getInstance()
            .registerCallbackListener(SubjectRemoveCallback.class,
                 new SubjectRemoveCallback() {
                    public void subjectRemoved(AuthzSubject toDelete) {
                        _dashDAO.handleSubjectRemoval(toDelete);
                    }
                }
            );
        HQApp.getInstance()
            .registerCallbackListener(RoleRemoveCallback.class,
                new RoleRemoveCallback() {
                    public void roleRemoved(Role r) {
                        RoleDashboardConfig cfg = _dashDAO.findDashboard(r);
                        
                        CrispoManagerLocal crispMgr =
                            CrispoManagerEJBImpl.getOne();
                        
                        List opts = crispMgr.findOptionByKey(
                            Constants.DEFAULT_DASHBOARD_ID);
                        
                        for (Iterator it = opts.iterator(); it.hasNext(); ) {
                            CrispoOption opt = (CrispoOption) it.next();
                            if (Integer.valueOf(opt.getValue()).equals(
                                cfg.getId())) {
                                crispMgr.updateOption(opt, null);
                            }
                        }
                        
                        _dashDAO.handleRoleRemoval(r);
                    }
                }
            );
        HQApp.getInstance()
            .registerCallbackListener(RoleRemoveFromSubjectCallback.class,
                 new RoleRemoveFromSubjectCallback() {
                    public void roleRemovedFromSubject(Role r,
                                                       AuthzSubject from) {
                        RoleDashboardConfig cfg = _dashDAO.findDashboard(r);
                        CrispoManagerLocal crispMgr =
                            CrispoManagerEJBImpl.getOne();
                        Crispo c = from.getPrefs();
                        if (c != null) {
                            for (Iterator it = c.getOptions().iterator();
                                 it.hasNext(); ) {
                                CrispoOption opt = (CrispoOption) it.next();
                                if (opt.getKey()
                                        .equals(Constants.DEFAULT_DASHBOARD_ID)
                                    && Integer.valueOf(opt.getValue())
                                        .equals(cfg.getId())) {
                                    crispMgr.updateOption(opt, null);
                                    break;
                                }
                            }
                        }
                    }
                }
            );
    }

    public static DashboardManagerLocal getOne() {
        try {
            return DashboardManagerUtil.getLocalHome().create();    
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
