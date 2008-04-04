package org.hyperic.hq.ui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.util.config.ConfigResponse;

public class UIUtilsImpl {
    public static List<AppdefResourceValue> getFavoriteResources(ServletContext ctx, WebUser user) {
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        ConfigResponse userPrefs = user.getPreferences();
        
        //Build the recent Resources List
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;
        if (userPrefs.getValue(key, null) != null) {
            List<AppdefResourceValue> list;
            try {
                list = getResourcesFromKeys(key, boss, user, userPrefs);
            } catch (Exception e) {
                try {
                    DashboardUtils.verifyResources(key, ctx, userPrefs, user);
                    list = getResourcesFromKeys(key, boss, user, userPrefs);
                } catch (Exception ex) {
                    return new ArrayList<AppdefResourceValue>();
                }
            }
            return list;
        } else {
            return new ArrayList<AppdefResourceValue>();
        }
    }
    
    public static List<AppdefResourceValue> getResourcesFromKeys(String key, AppdefBoss boss, WebUser user,
            ConfigResponse dashPrefs) throws Exception {
        List entityIds = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
        Collections.reverse(entityIds); // Most recent on top
        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = (AppdefEntityID[]) entityIds.toArray(arrayIds);
        return boss.findByIds(user.getSessionId().intValue(), arrayIds);
    }
}
