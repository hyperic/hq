/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Utilities class that provides general convenience methods.
 */
public class DashboardUtils {
    public static final char MULTI_PORTLET_TOKEN = '_';
    public static final Log log = LogFactory.getLog(DashboardUtils.class.getName());
    
    public static List<AppdefResourceValue> listAsResources(List list, ServletContext ctx,
                                       WebUser user, AppdefBoss appdefBoss)
        throws Exception {
        List entityIds = listAsEntityIds(list);
        ArrayList<AppdefResourceValue> resources = new ArrayList<AppdefResourceValue>();
        for (Iterator i = entityIds.iterator(); i.hasNext();) {
            AppdefEntityID entityID = (AppdefEntityID) i.next();
            
            // Adding try/catch block as a safe guard to issue filed in [SUPPORT-5402]
            // Because of the existing logic, a pending resource list could contain a resource
            // that has since been deleted causing an AppdefEntityNotFound exception.  It wasn't being handled
            // before causing bad results in the UI, now we're catching and logging as it's non fatal.
            // TODO look at redesigning this logic. IMO transient state (which the pending resources list essentially is) 
            //      shouldn't be stored as a user preference...
            try {
                AppdefResourceValue resource = appdefBoss.findById(user.getSessionId().intValue(), entityID);
                
                resources.add(resource);
            } catch(AppdefEntityNotFoundException e) {
                // ...we have a pending appdef id that does not exist
                // log it, and remove it from the list
                log.warn("Appdef Id: " + entityID + " does not exist, so ignoring it.", e);
            }
        }

        return resources;
    }
    
    public static List<AppdefEntityID> listAsEntityIds(List list) {
        ArrayList resources = new ArrayList();
        Iterator i = list.iterator();

        while (i.hasNext()) {
            try {
                resources.add(new AppdefEntityID((String) i.next()));
            } catch (InvalidAppdefTypeException e) {
                // Skip resource
            }
        }

        return resources;
    }


    public static List preferencesAsEntityIds(String key, WebUser user) {
        try {
            List resourceList = 
                user.getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);
            return listAsEntityIds(resourceList);
        } catch (InvalidOptionException e) {
            return new ArrayList(0);
        }
    }
    
    public static List<AppdefEntityID> preferencesAsEntityIds(String key,
                                              ConfigResponse userConfig) {
        try {
            List resourceList = 
                userConfig.getPreferenceAsList(key,
                                               Constants.DASHBOARD_DELIMITER);
            return listAsEntityIds(resourceList);
        } catch (InvalidOptionException e) {
            return new ArrayList(0);
        }
    }

    // TODO: It doesn't look like this method is being used in the app.  May want 
    //       to yank it out or at least change the impl to call the other 
    //       removePortlet method below
    public static void removePortlet(WebUser user, String portlet)
        throws InvalidOptionException, InvalidOptionValueException {
		
		// Get list of portlets from first column...
		String portlets = user.getPreference(Constants.USER_PORTLETS_FIRST);
		// ...make sure there's something there before assigning a value...
		String first =  (portlets != null) ? 
				        portlets + Constants.DASHBOARD_DELIMITER : 
			        	// ...if portlet is null just assign the delimiter,
				        //    otherwise, the dashboard will think it should assign defaults
				        Constants.DASHBOARD_DELIMITER;
	    // Do the same for portlets from second column...
		portlets = user.getPreference(Constants.USER_PORTLETS_SECOND);
	    // ...don't forget to check before assigning
	    String second = (portlets != null) ? 
	    		        portlets + Constants.DASHBOARD_DELIMITER :
                        Constants.DASHBOARD_DELIMITER;

        first =
            StringUtil.remove(first,  portlet + Constants.DASHBOARD_DELIMITER);
        second =
            StringUtil.remove(second, portlet + Constants.DASHBOARD_DELIMITER);

        first = StringUtil.replace(first,  Constants.EMPTY_DELIMITER,
                                   Constants.DASHBOARD_DELIMITER);
        second = StringUtil.replace(second,Constants.EMPTY_DELIMITER,
                                    Constants.DASHBOARD_DELIMITER);

        user.setPreference(Constants.USER_PORTLETS_FIRST, first);
        user.setPreference(Constants.USER_PORTLETS_SECOND, second);
        
        // Need to clear out the preferences for multiple portlets
        int index;
        if ((index = portlet.lastIndexOf(MULTI_PORTLET_TOKEN)) > -1) {
            String token = portlet.substring(index);
            
            // If there are other portlets with the same token, then we can't
            // arbitrarily delete preferences
            if (first.indexOf(token) > -1 || second.indexOf(token) > -1) {
                return;
            }
            
            // Look through preferences for the token
            ConfigResponse prefs = user.getPreferences();
            String[] keys = (String[]) prefs.getKeys().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].indexOf(token) > -1) {
                    prefs.unsetValue(keys[i]);
                }
            }
            
            user.setPreferences(prefs);
        }
    }
    
	public static void removePortlet(ConfigResponse config, String portlet)
	    throws InvalidOptionException, InvalidOptionValueException {
		
		// Get list of portlets from first column...
		String portlets = config.getValue(Constants.USER_PORTLETS_FIRST);
		// ...make sure there's something there before assigning a value...
		String first =  (portlets != null) ? 
				        portlets + Constants.DASHBOARD_DELIMITER : 
			        	// ...if portlet is null just assign the delimiter,
				        //    otherwise, the dashboard will think it should assign defaults
				        "";
	    // Do the same for portlets from second column...
		portlets = config.getValue(Constants.USER_PORTLETS_SECOND);
	    // ...don't forget to check before assigning
	    String second = (portlets != null) ? 
	    		        portlets + Constants.DASHBOARD_DELIMITER :
                        "";
	
	    first =
	        StringUtil.remove(first,  portlet + Constants.DASHBOARD_DELIMITER);
	    second =
	        StringUtil.remove(second, portlet + Constants.DASHBOARD_DELIMITER);
	
	    first = StringUtil.replace(first,  Constants.EMPTY_DELIMITER,
	                               Constants.DASHBOARD_DELIMITER);
	    second = StringUtil.replace(second,Constants.EMPTY_DELIMITER,
	                                Constants.DASHBOARD_DELIMITER);
	
	    config.setValue(Constants.USER_PORTLETS_FIRST, first);
	    config.setValue(Constants.USER_PORTLETS_SECOND, second);
	    
	    // Need to clear out the preferences for multiple portlets
	    int index;
	    if ((index = portlet.lastIndexOf(MULTI_PORTLET_TOKEN)) > -1) {
	        String token = portlet.substring(index);
	        
	        // If there are other portlets with the same token, then we can't
	        // arbitrarily delete preferences
	        if (first.indexOf(token) > -1 || second.indexOf(token) > -1) {
	            return;
	        }
	        
	        // Look through preferences for the token
	        String[] keys = (String[]) config.getKeys().toArray(new String[0]);
	        for (int i = 0; i < keys.length; i++) {
	            if (keys[i].indexOf(token) > -1) {
	            	config.unsetValue(keys[i]);
	            }
	        }
	        
	    }
	}

	public static void removeResources(String[] ids, String key, WebUser user)
        throws Exception {
        String resources = user.getPreference(key);

        for (int i = 0; i < ids.length; i++) {
            String resource = ids[i];
            resources = StringUtil.remove(resources, resource);
            resources = StringUtil.replace(resources, Constants.EMPTY_DELIMITER,
                                           Constants.DASHBOARD_DELIMITER);
        }

        user.setPreference(key, resources);
    }
    
    public static void removeResources(String[] ids, String key, 
    		ConfigResponse userConfg)
    	throws Exception {
	    String resources = userConfg.getValue(key);
	
	    for (int i = 0; i < ids.length; i++) {
	        String resource = ids[i];
	        resources = StringUtil.remove(resources, resource);
	        resources = StringUtil.replace(resources, Constants.EMPTY_DELIMITER,
	                                       Constants.DASHBOARD_DELIMITER);
	    }

	    userConfg.setValue(key, resources);
    }

    public static void verifyResources(String key, ServletContext ctx,
			ConfigResponse config, WebUser user, AppdefBoss appdefBoss, AuthzBoss authzBoss) throws Exception {
		List resourcelist = preferencesAsEntityIds(key, config);
		ArrayList toRemove = new ArrayList();
		for (Iterator i = resourcelist.iterator(); i.hasNext();) {
			AppdefEntityID entityID = (AppdefEntityID) i.next();
			try {
				appdefBoss.findById(user.getSessionId().intValue(), entityID);
			} catch (Exception e) {
				String entityid = entityID.getAppdefKey();
				toRemove.add(entityid);
			}
		}

		if (toRemove.size() > 0) {
			String[] ids = (String[]) toRemove.toArray(new String[0]);
			removeResources(ids, key, config);
			authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user
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
        
        user.setPreference(key,
                        StringUtil.listToString(existing,
                                                Constants.DASHBOARD_DELIMITER));
    }
    
    public static boolean addEntityToPreferences(String key,
                                                 ConfigResponse userConfig,
                                                 AppdefEntityID newId,
                                                 int max) {
		List existing = preferencesAsEntityIds(key, userConfig);
        
        int lastIdx = existing.lastIndexOf(newId);
        if (existing.size() > 0 && lastIdx == (existing.size() - 1)) {
            return false;       // Already the last one
        }
        
        // Now add the new one
        existing.add(newId);

        if (lastIdx > -1) {
            for (Iterator it = existing.iterator(); it.hasNext();) {
                AppdefEntityID entityID = (AppdefEntityID) it.next();
                if (entityID.equals(newId)) {
                    it.remove();
                    break;
                }
            }
        }
        else {
            if (max < Integer.MAX_VALUE && existing.size() > max)
                existing.remove(0);    
        }

		userConfig.setValue(key, StringUtil.listToString(existing,
				Constants.DASHBOARD_DELIMITER));
        
        return true;
	}
    
    public static DashboardConfig findDashboard(ArrayList dashboardList, Integer id) {
		Iterator i = dashboardList.iterator();
		while (i.hasNext()) {
			DashboardConfig config = (DashboardConfig) i.next();
			if (config.getId().equals(id)) {
				return config;
			}
		}
		return null;
	}
    
    /**
     * Find the user's dashboard
     */
    public static ConfigResponse findUserDashboardConfig(WebUser user, DashboardManager dashboardManager, SessionManager sessionManager)
    throws SessionNotFoundException, SessionTimeoutException, PermissionException, RemoteException 
    {
    	AuthzSubject subj = sessionManager.getSubject(user.getSessionId());
     	return dashboardManager.getUserDashboard(subj, subj).getConfig();
    }
}
