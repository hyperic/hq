/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.ui.shared;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.util.config.ConfigResponse;

/**
 * Local interface for DashboardManager.
 */
public interface DashboardManager {

    public UserDashboardConfig getUserDashboard(AuthzSubject me, AuthzSubject user)
        throws PermissionException;

    public RoleDashboardConfig getRoleDashboard(AuthzSubject me, Role r) throws PermissionException;

    public UserDashboardConfig createUserDashboard(AuthzSubject me, AuthzSubject user, String name)
        throws PermissionException;

    public RoleDashboardConfig createRoleDashboard(AuthzSubject me, Role r, String name)
        throws PermissionException;

    /**
     * Reconfigure a user's dashboard
     */
    public void configureDashboard(AuthzSubject me, DashboardConfig cfg, ConfigResponse newCfg)
        throws PermissionException;

    public void renameDashboard(AuthzSubject me, DashboardConfig cfg, String name)
        throws PermissionException;

    /**
     * Determine if a dashboard is editable by the passed user
     */
    public boolean isEditable(AuthzSubject me, DashboardConfig dash);

    public Collection<DashboardConfig> getDashboards(AuthzSubject me) throws PermissionException;

    /**
     * Update dashboard and user configs to account for resource deletion
     * @param ids An array of ID's of removed resources
     */
    public void handleResourceDelete(AppdefEntityID[] ids);

    public ConfigResponse getRssUserPreferences(String user, String token) throws LoginException;

    public List<DashboardConfig> findEditableDashboardConfigs(WebUser user, AuthzBoss boss)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        RemoteException;

    public List<Dashboard> findEditableDashboards(WebUser user, AuthzBoss boss)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        RemoteException;

    public DashboardConfig findDashboard(Integer id, WebUser user, AuthzBoss boss);

}
