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

package org.hyperic.hq.ui.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("uiUtils")
public class UIUtilsImpl implements UIUtils {

    protected AppdefBoss appdefBoss;
    private AuthzBoss authzBoss;

    @Autowired
    public UIUtilsImpl(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.authzBoss = authzBoss;
    }

    public List<AppdefResourceValue> getFavoriteResources(ServletContext ctx, WebUser user) {

        ConfigResponse userPrefs = user.getPreferences();

        // Build the recent Resources List
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;
        if (userPrefs.getValue(key, null) != null) {
            List<AppdefResourceValue> list;
            try {
                list = getResourcesFromKeys(key, user, userPrefs);
            } catch (Exception e) {
                try {
                    DashboardUtils
                        .verifyResources(key, ctx, userPrefs, user, appdefBoss, authzBoss);
                    list = getResourcesFromKeys(key, user, userPrefs);
                } catch (Exception ex) {
                    return new ArrayList<AppdefResourceValue>();
                }
            }
            return list;
        } else {
            return new ArrayList<AppdefResourceValue>();
        }
    }

    public List<AppdefResourceValue> getResourcesFromKeys(String key, WebUser user,
                                                          ConfigResponse dashPrefs)
        throws Exception {
        List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
        Collections.reverse(entityIds); // Most recent on top
        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = (AppdefEntityID[]) entityIds.toArray(arrayIds);
        return appdefBoss.findByIds(user.getSessionId().intValue(), arrayIds, PageControl.PAGE_ALL);
    }

    public void setResourceFlags(AppdefResourceValue resource, boolean config,
                                 HttpServletRequest request, ServletContext ctx) throws Exception {
        // No-Op
    }

    public List getResourceTypes(ServletContext ctx, Integer sessionId) throws PermissionException,
        SessionTimeoutException, SessionNotFoundException, RemoteException {
        return new ArrayList();
    }

    public boolean isResourceAlertable(AppdefResourceValue rv) {
    	switch (rv.getEntityId().getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return true;
        default:
            return false;
    }    }
}
