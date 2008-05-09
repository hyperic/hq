package org.hyperic.hq.ui.pages.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.hyperic.hq.ui.pages.MenuPage;
import org.hyperic.ui.tapestry.components.panel.ListPanel.ListItem;

public abstract class GroupManager extends MenuPage {
    private final Log _log = LogFactory.getLog(GroupManager.class);
    
    public List getGroupList() {
        List res = new ArrayList();
        
        res.add(new ListItem() { 
            public String getLabel() {
                return "fubar";
            }

            public String getId() {
                return "i:fubar";
            }
        });
        res.add(new ListItem() { 
            public String getLabel() {
                return "barbaz";
            }

            public String getId() {
                return "i:barbaz";
            }
        });
        return res;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }
    
    public void selectGroup(IRequestCycle cycle, String id) {
        _log.info("Selected item [" + id + "]");
        cycle.getResponseBuilder().updateComponent("innerDiv");        
    }
}
