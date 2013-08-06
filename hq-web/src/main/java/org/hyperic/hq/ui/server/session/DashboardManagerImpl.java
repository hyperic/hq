/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzApplicationEvent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleCreatedEvent;
import org.hyperic.hq.authz.server.session.RoleDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.SubjectDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.events.subject.RoleMembersChangedZevent;
import org.hyperic.hq.authz.server.session.events.subject.SubjectRemovedFromRolesZevent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoOption;
import org.hyperic.hq.common.shared.CrispoManager;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Transactional
public class DashboardManagerImpl implements DashboardManager, ApplicationListener<AuthzApplicationEvent>, 
    ZeventListener<RoleMembersChangedZevent> {

    private Log log = LogFactory.getLog(DashboardManagerImpl.class);

    protected SessionManager sessionManager = SessionManager.getInstance();

    private DashboardConfigDAO dashDao;
    private CrispoManager crispoManager;
    private AuthzSubjectManager authzSubjectManager;
   

    @Autowired
    public DashboardManagerImpl(DashboardConfigDAO dashDao, CrispoManager crispoManager,
                                AuthzSubjectManager authzSubjectManager) {
        this.dashDao = dashDao;
        this.crispoManager = crispoManager;
        this.authzSubjectManager = authzSubjectManager;
    }

    /**
     */
    @Transactional(readOnly = true)
    public UserDashboardConfig getUserDashboard(AuthzSubject me, AuthzSubject user) throws PermissionException {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        if (!me.equals(user) && !permMan.hasAdminPermission(me.getId())) {
            throw new PermissionException("You are unauthorized to see this " + "dashboard");
        }

        return dashDao.findDashboard(user);
    }

    /**
     */
    @Transactional(readOnly = true)
    public RoleDashboardConfig getRoleDashboard(AuthzSubject me, Role r) throws PermissionException {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        permMan.check(me.getId(), r.getResource().getResourceType(), r.getId(), AuthzConstants.roleOpModifyRole);

        return dashDao.findDashboard(r);
    }

    private ConfigResponse getDefaultConfig() {
        return new ConfigResponse();
    }

    /**
     */
    public UserDashboardConfig createUserDashboard(AuthzSubject me, AuthzSubject user, String name)
        throws PermissionException {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        if (!me.equals(user) && !permMan.hasAdminPermission(me.getId())) {
            throw new PermissionException("You are unauthorized to create " + "this dashboard");
        }

        Crispo cfg = crispoManager.create(getDefaultConfig());
        UserDashboardConfig dash = new UserDashboardConfig(user, name, cfg);
        dashDao.save(dash);
        return dash;
    }

    /**
     */
    public RoleDashboardConfig createRoleDashboard(AuthzSubject me, Role r, String name) throws PermissionException {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        permMan.check(me.getId(), r.getResource().getResourceType(), r.getId(), AuthzConstants.roleOpModifyRole);

        Crispo cfg = crispoManager.create(getDefaultConfig());
        RoleDashboardConfig dash = new RoleDashboardConfig(r, name, cfg);
        dashDao.save(dash);
        return dash;
    }

    /**
     * Reconfigure a user's dashboard
     */
    public void configureDashboard(AuthzSubject me, DashboardConfig cfg, ConfigResponse newCfg)
        throws PermissionException {
        if (!isEditable(me, cfg)) {
            throw new PermissionException("You are unauthorized to modify " + "this dashboard");
        }
        crispoManager.update(cfg.getCrispo(), newCfg);
    }

    /**
     */
    public void renameDashboard(AuthzSubject me, DashboardConfig cfg, String name) throws PermissionException {
        if (!isEditable(me, cfg)) {
            throw new PermissionException("You are unauthorized to modify " + "this dashboard");
        }
        cfg.setName(name);
    }

    /**
     * Determine if a dashboard is editable by the passed user
     */
    @Transactional(readOnly = true)
    public boolean isEditable(AuthzSubject me, DashboardConfig dash) {
        PermissionManager permMan = PermissionManagerFactory.getInstance();

        if (permMan.hasAdminPermission(me.getId()))
            return true;

        return dash.isEditable(me);
    }

    /**
     */
    @Transactional(readOnly = true)
    public Collection<DashboardConfig> getDashboards(AuthzSubject me) throws PermissionException {
        Collection<DashboardConfig> res = new ArrayList<DashboardConfig>();

        PermissionManager permMan = PermissionManagerFactory.getInstance();
        if (permMan.hasGuestRole() && permMan.hasAdminPermission(me.getId())) {
            res.addAll(dashDao.findAllRoleDashboards());
            res.add(getUserDashboard(me, me));
            return res;
        }

        UserDashboardConfig cfg = getUserDashboard(me, me);
        if (cfg != null)
            res.add(cfg);

        if (permMan.hasGuestRole())
            res.addAll(dashDao.findRolesFor(me));

        return res;
    }

    /**
     * Update dashboard and user configs to account for resource deletion
     * 
     * @param ids An array of ID's of removed resources
     */
    public void handleResourceDelete(AppdefEntityID[] ids) {
        for (int i = 0; i < ids.length; i++) {
            String appdefKey = ids[i].getAppdefKey();
            List<CrispoOption> copts = crispoManager.findOptionByValue(appdefKey);

            for (CrispoOption o : copts) {
                String val = o.getValue();
                String newVal = removeResource(val, appdefKey);

                if (!val.equals(newVal)) {
                    crispoManager.updateOption(o, newVal);
                    log.debug("Update option key=" + o.getKey() + " old =" + val + " new =" + newVal);
                }
            }
        }
    }

    /**
     */
    @Transactional(readOnly = true)
    public ConfigResponse getRssUserPreferences(String user, String token) throws LoginException {
        ConfigResponse preferences;
        try {
            AuthzSubject me = authzSubjectManager.findSubjectByName(user);
            preferences = getUserDashboard(me, me).getConfig();
        } catch (Exception e) {
            throw new LoginException("Username has no preferences");
        }

        // Let's make sure that the rss auth token matches
        String prefToken = preferences.getValue(Constants.RSS_TOKEN);
        if (token == null || !token.equals(prefToken))
            throw new LoginException("Username and Auth token do not match");

        return preferences;
    }

    private String removeResource(String val, String resource) {
        val = StringUtil.remove(val, resource);
        val = StringUtil.replace(val, Constants.EMPTY_DELIMITER, Constants.DASHBOARD_DELIMITER);
        return val;
    }
    
    public void onApplicationEvent(AuthzApplicationEvent event) {
        if(event instanceof SubjectDeleteRequestedEvent) {
            dashDao.handleSubjectRemoval(((SubjectDeleteRequestedEvent)event).getSubject());
        }else if(event instanceof RoleDeleteRequestedEvent) {
            roleRemoved(((RoleDeleteRequestedEvent)event).getRole());
        }else if(event instanceof RoleCreatedEvent) {
            roleCreated(((RoleCreatedEvent)event).getRole());
//        }else if(event instanceof SubjectRemovedFromRolesEvent) {
//            handleSubjectRemovedFromRolesEvent((SubjectRemovedFromRolesEvent) event);
        }
    }

    public void processEvents(List<RoleMembersChangedZevent> events) {
        for (final RoleMembersChangedZevent zevent : events) {
            if (zevent instanceof SubjectRemovedFromRolesZevent) {
                handleSubjectRemovedFromRolesZevent((SubjectRemovedFromRolesZevent) zevent);
            }
        }
    }


    private void roleRemoved(Role role) {
        RoleDashboardConfig cfg = dashDao.findDashboard(role);
        if (cfg == null) {
            return;
        }
        List<CrispoOption> opts = crispoManager.findOptionByKey(Constants.DEFAULT_DASHBOARD_ID);
        for (CrispoOption opt : opts) {
            if (Integer.valueOf(opt.getValue()).equals(cfg.getId())) {
                crispoManager.updateOption(opt, null);
            }
        }
        dashDao.handleRoleRemoval(role);
    }
    
    private void roleCreated(Role role) {
        Crispo cfg = crispoManager.create(getDefaultConfig());
        RoleDashboardConfig dash = new RoleDashboardConfig(role, role.getName() + " Role Dashboard", cfg);
        dashDao.save(dash);
    }
    
    private void roleRemovedFromSubject(Role r, AuthzSubject from) {
        RoleDashboardConfig cfg = dashDao.findDashboard(r);
        Crispo c = from.getPrefs();
        if (c != null) {
            for (CrispoOption opt : c.getOptions()) {
                if (opt.getKey().equals(Constants.DEFAULT_DASHBOARD_ID) &&
                    Integer.valueOf(opt.getValue()).equals(cfg.getId())) {
                    crispoManager.updateOption(opt, null);
                    break;
                }
            }
        }
    }
    
    private void handleSubjectRemovedFromRolesZevent(SubjectRemovedFromRolesZevent zevent) {
            AuthzSubject subject = zevent.getSubject();
            Collection<Role> roles = zevent.getRoles();
            for (Role role : roles){
                roleRemovedFromSubject(role, subject);
            }
    }//EOM handleSubjectRemovedFromRolesZevent 


    @Transactional(readOnly = true)
    public List<DashboardConfig> findEditableDashboardConfigs(WebUser user, AuthzBoss boss)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, RemoteException {
        AuthzSubject me = boss.findSubjectById(user.getSessionId(), user.getSubject().getId());

        Collection<DashboardConfig> dashboardCollection = getDashboards(me);
        List<DashboardConfig> editableDashboardConfigs = new ArrayList<DashboardConfig>();

        for (DashboardConfig config : dashboardCollection) {
            if (isEditable(me, config)) {
                editableDashboardConfigs.add(config);
            }
        }

        return editableDashboardConfigs;
    }

    @Transactional(readOnly = true)
    public List<Dashboard> findEditableDashboards(WebUser user, AuthzBoss boss) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException, RemoteException {
        List<DashboardConfig> dashboardConfigs = findEditableDashboardConfigs(user, boss);
        List<Dashboard> editableDashboards = new ArrayList<Dashboard>();

        for (DashboardConfig config : dashboardConfigs) {
            Dashboard dashboard = new Dashboard();

            dashboard.set_name(config.getName());
            dashboard.setId(config.getId());

            editableDashboards.add(dashboard);
        }

        return editableDashboards;
    }

    /**
     * Find a given dashboard by its id
     * @param id the id of the dashboard
     * @param user current user
     * @param boss the authzboss
     * @return the DashboardConfig of the corresponding DashboardId or null if
     *         none
     */
    @Transactional(readOnly = true)
    public DashboardConfig findDashboard(Integer id, WebUser user, AuthzBoss boss) {
        Collection<DashboardConfig> dashboardCollection;

        try {
            AuthzSubject me = boss.findSubjectById(user.getSessionId(), user.getSubject().getId());
            dashboardCollection = getDashboards(me);
        } catch (Exception e) {
            return null;
        }

        for (DashboardConfig config : dashboardCollection) {
            if (config.getId().equals(id)) {
                return config;
            }
        }

        return null;
    }
}
