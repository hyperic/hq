package org.hyperic.hq.ui.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("uiUtils")
public class UIUtilsImpl implements UIUtils {
    
    protected AppdefBoss appdefBoss;
    
    
    @Autowired
    public UIUtilsImpl(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public List<AppdefResourceValue> getFavoriteResources(ServletContext ctx, WebUser user) {
       
        ConfigResponse userPrefs = user.getPreferences();
        
        //Build the recent Resources List
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;
        if (userPrefs.getValue(key, null) != null) {
            List<AppdefResourceValue> list;
            try {
                list = getResourcesFromKeys(key,  user, userPrefs);
            } catch (Exception e) {
                try {
                    DashboardUtils.verifyResources(key, ctx, userPrefs, user);
                    list = getResourcesFromKeys(key,  user, userPrefs);
                } catch (Exception ex) {
                    return new ArrayList<AppdefResourceValue>();
                }
            }
            return list;
        } else {
            return new ArrayList<AppdefResourceValue>();
        }
    }
    
    public List<AppdefResourceValue> getResourcesFromKeys(String key,  WebUser user,
            ConfigResponse dashPrefs) throws Exception {
        List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
        Collections.reverse(entityIds); // Most recent on top
        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = (AppdefEntityID[]) entityIds.toArray(arrayIds);
        return appdefBoss.findByIds(user.getSessionId().intValue(), arrayIds,
                              PageControl.PAGE_ALL);
    }
    
   

    public void setResourceFlags(AppdefResourceValue resource, boolean config, HttpServletRequest request, ServletContext ctx)
        throws Exception {
        // No-Op
    }

    public List getResourceTypes(ServletContext ctx, Integer sessionId) throws 
        NamingException, FinderException, CreateException, PermissionException, SessionTimeoutException,
        SessionNotFoundException, RemoteException {
        return new ArrayList();
    }

   
    
    
}
