package org.hyperic.hq.ui.action.portlet.search;

import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.springframework.stereotype.Component;

@Component("searchViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {
	private final Log log = LogFactory.getLog(ViewActionNG.class);
	
	@Resource
	private AppdefBoss appdefBoss;
	
	private final String  prefix = "resource.hub.filter.";

	public void execute(TilesRequestContext requestContext, AttributeContext attrContext) {
		
        String[][] entityTypes = appdefBoss.getAppdefTypeStrArrMap();
        HashMap<String,String> resourcesList = new LinkedHashMap<String,String>();
        if (entityTypes != null) {
            for (int i = 0; i < entityTypes.length; i++) {
                if (!entityTypes[i][0].equals("5")) {
                    resourcesList.put(entityTypes[i][0],prefix+entityTypes[i][1]);
                }
            }
        	resourcesList.put("5",prefix+"mixedGroups");
            resourcesList.put( "6",prefix+"compatibleGroups");
        }
        
        requestContext.getRequestScope().put("resourcesHub", resourcesList);    
	}

}
