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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.bizapp.shared.AppdefBoss;

public class CheckPermissionsUtil {
	private static Log log = LogFactory.getLog(CheckPermissionsUtil.class.getName());

	// Modifies the passed in list by removing resource that the user doesn't have access to
	public static List<AppdefEntityID> filterEntityIdsByViewPermission(final int sessionId, final List<AppdefEntityID> appDefIds, final AppdefBoss adBoss) {
		List<AppdefEntityID> result = new ArrayList<AppdefEntityID>() {{
			addAll(appDefIds);
		}};
		
		for (Iterator<AppdefEntityID> i = result.iterator(); i.hasNext();) {
    		AppdefEntityID adeId = i.next();
    		
    		try {
    			AppdefResourcePermissions permissions = adBoss.getResourcePermissions(sessionId, adeId);
    			
    			if (!permissions.canView()) {
    				// If user can't view this resource, pluck it off the list
    				i.remove();
    			}
    		} catch (Exception e) {
				// Some problem occurred while checking this resource's permissions
    			// Pluck it off the list to be safe and log it...
    			i.remove();
			}
    	}
		
		return result;
    }
    
	public static boolean canUserViewChart(int sessionId, String url, AppdefBoss apBoss) {
	   boolean result = false;
		   
	   // First parse the url to get the resource type and id of the resource
	   final String RESOURCE_TYPE_PARAM = "type";
	   final String RESOURCE_ID_PARAM = "rid";
	   final String URL_PARAM_DELIMITER = "?";
	   
	   // Check for query parameters, if we don't have one we can't make the 
	   // determination and can't verify the user can see this resource...
	   String[] splitUrl = url.split("\\" + URL_PARAM_DELIMITER);
   
	   if (splitUrl.length == 2) {
		   String queryParams = splitUrl[1];
			   
		   // Make query parameters a little easier to deal with
		   Map<String, List<String>> paramMap = createParameterMap(queryParams);
			   
		   // Now find the values we need, to continue on
		   if (paramMap.containsKey(RESOURCE_ID_PARAM) && paramMap.containsKey(RESOURCE_TYPE_PARAM)) {
			   // If we've gotten this far we know there will be at least one value in both of these
			   // lists.  If we have to handle the case of multiple values, this would have to change
			   // Hopefully, the design will be revisited at that point...
			   String resourceType = paramMap.get(RESOURCE_TYPE_PARAM).get(0);
			   String resourceId = paramMap.get(RESOURCE_ID_PARAM).get(0);
				   
			   try {
				   AppdefEntityID adeId = new AppdefEntityID(Integer.parseInt(resourceType),
			    	                                         Integer.parseInt(resourceId));
				   List<AppdefEntityID> ids = new ArrayList<AppdefEntityID>();
						
				   ids.add(adeId);
					   
				   // Now check it
				   List<AppdefEntityID> list = CheckPermissionsUtil.filterEntityIdsByViewPermission(sessionId, 
	                          											                            ids,
	                          											                            apBoss);
				       
				   result = list.size() == 1;
			   } catch(Exception e) {
				   // Problemos...can't validate the permissions...
				   // Should log something here
			   log.warn("Could not verify view permissions for url: " + url, e);
			   }
		   }
	   }
	   
	   return result;
   }
	   
   private static Map<String, List<String>> createParameterMap(String queryParameters) {
	   final String QUERY_PARAM_DELIMITER = "&";
		   
	   Map<String, List<String>> result = new HashMap<String, List<String>>();
		   
	   if (queryParameters.indexOf(QUERY_PARAM_DELIMITER) > -1) {
		   String[] nameValuePairs = queryParameters.split(QUERY_PARAM_DELIMITER);
	   
		   for (String nameValuePair : nameValuePairs)
			   processNameValuePair(result, nameValuePair);
	   }
	   
	   return result;
   }  
	   
   private static void processNameValuePair(Map<String, List<String>> map, String nameValuePair) {
	   final String NAME_VALUE_PAIR_DELIMITER = "=";
	   
	   if (nameValuePair.indexOf(NAME_VALUE_PAIR_DELIMITER) > -1) {
		   String[] nameAndValue = nameValuePair.split(NAME_VALUE_PAIR_DELIMITER);
		   String name = nameAndValue[0];
		   String value = nameAndValue.length == 2 ? nameAndValue[1] : ""; // Could be an empty value
		   List<String> values;
		   
		   // Check for multiple query parameters of the same name
		   if (map.containsKey(name)) {
			   values = map.get(name);
		   } else {
			   values = new ArrayList<String>();
		   }
		   
		   // Update list of values
		   values.add(value);
			   
		   // Update parameter map
		   map.put(name, values);
	   }
   }
}
