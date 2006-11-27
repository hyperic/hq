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

package org.hyperic.hq.ui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Utilities class that provides general convenience methods.
 */
public class DashboardUtils {

    public static List listAsResources(List list, ServletContext ctx,
                                       WebUser user)
        throws Exception {
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        List entityIds = listAsEntityIds(list);
        ArrayList resources = new ArrayList();
        for (Iterator i = entityIds.iterator(); i.hasNext();) {
            AppdefEntityID entityID = (AppdefEntityID) i.next();
            AppdefResourceValue resource =
                appdefBoss.findById(user.getSessionId().intValue(), entityID);
            resources.add(resource);
        }

        return resources;
    }

    public static List listAsEntityIds(List list) throws Exception {
        ArrayList resources = new ArrayList();
        Iterator i = list.iterator();

        while (i.hasNext()) {
            ArrayList resourceIds =
                (ArrayList) StringUtil.explode((String) i.next(), ":");

            Iterator j = resourceIds.iterator();
            int type = Integer.parseInt((String) j.next());
            int id = Integer.parseInt((String) j.next());

            AppdefEntityID entityID = new AppdefEntityID(type, id);
            resources.add(entityID);
        }

        return resources;
    }

    public static List preferencesAsResources(String key, ServletContext ctx,
                                              WebUser user)
        throws Exception {
        List resourceList = user
            .getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);
        return listAsResources(resourceList, ctx, user);
    }

    public static List preferencesAsEntityIds(String key, WebUser user)
        throws Exception {
        try {
            List resourceList = 
                user.getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);
            return listAsEntityIds(resourceList);
        } catch (InvalidOptionException e) {
            return new ArrayList(0);
        }
    }

    public static void removePortlet(WebUser user, String PortletName)
        throws InvalidOptionException, InvalidOptionValueException {
        String first = user.getPreference(Constants.USER_PORTLETS_FIRST);
        String second = user.getPreference(Constants.USER_PORTLETS_SECOND);

        first = StringUtil.remove(first, PortletName);
        second = StringUtil.remove(second, PortletName);

        first = StringUtil.replace(first, "||", Constants.DASHBOARD_DELIMITER);
        second = StringUtil
            .replace(second, "||", Constants.DASHBOARD_DELIMITER);

        user.setPreference(Constants.USER_PORTLETS_FIRST, first);
        user.setPreference(Constants.USER_PORTLETS_SECOND, second);
        
        // XXX - Need to clear out the preferences for multiple portlets
    }

    public static void removeResources(String[] ids, String key, WebUser user)
        throws Exception {
        String resources = user.getPreference(key);

        for (int i = 0; i < ids.length; i++) {
            String resource = ids[i];
            resources = StringUtil.remove(resources, resource);
            resources = StringUtil.replace(resources, "||",
                                           Constants.DASHBOARD_DELIMITER);
        }

        user.setPreference(key, resources);
    }

    public static void verifyResources(String key, ServletContext ctx,
                                       WebUser user)
        throws Exception {
        List resourcelist = preferencesAsEntityIds(key, user);
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        AuthzBoss authzboss = ContextUtils.getAuthzBoss(ctx);
        for (Iterator i = resourcelist.iterator(); i.hasNext();) {
            AppdefEntityID entityID = (AppdefEntityID) i.next();
            try {
                appdefBoss.findById(user.getSessionId().intValue(), entityID);
            } catch (Exception e) {
                String entityid = entityID.getAppdefKey();
                removeResources(new String[] { entityid }, key, user);

            }
            authzboss.setUserPrefs(user.getSessionId(), user.getId(), user
                .getPreferences());
        }
    }
    
    public static void addEntityToPreferences(String key, WebUser user,
                                              AppdefEntityID newId, int max)
        throws Exception {
        List existing = preferencesAsEntityIds(key, user);
        for (Iterator it = existing.iterator(); it.hasNext();) {
            AppdefEntityID entityID = (AppdefEntityID) it.next();
            if (entityID.equals(newId))
                it.remove();
        }
        
        // Now add the new one
        existing.add(newId);
        if (max < Integer.MAX_VALUE && existing.size() > max)
            existing.remove(0);
        
        user.setPreference(key, StringUtil
            .listToString(existing, Constants.DASHBOARD_DELIMITER));
    }

    public static final char MULTI_PORTLET_TOKEN = '@';
}
