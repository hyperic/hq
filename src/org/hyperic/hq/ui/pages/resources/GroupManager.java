package org.hyperic.hq.ui.pages.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.bizapp.explorer.ExplorerContext;
import org.hyperic.hq.bizapp.explorer.ExplorerItem;
import org.hyperic.hq.bizapp.explorer.ExplorerManager;
import org.hyperic.hq.bizapp.explorer.SimpleExplorerContext;
import org.hyperic.hq.bizapp.explorer.types.GroupManagerRootItem;
import org.hyperic.hq.bizapp.explorer.types.GroupManagerRootItemType;
import org.hyperic.hq.ui.pages.MenuPage;
import org.hyperic.ui.tapestry.components.panel.ListPanel.ListItem;
import org.hyperic.util.StringUtil;

public abstract class GroupManager extends MenuPage {
    private final Log _log = LogFactory.getLog(GroupManager.class);
    private final ExplorerManager _expMan = ExplorerManager.getInstance();
    
    /**
     * Take an explorer element, traverse down it, adding to the list of
     * listItems
     */
    private void flattenList(ExplorerContext ctx, List addList, 
                             final ExplorerItem item, final int depth) 
    {
        ListItem listItem = new ListItem() {
            public String getLabel() {
                return StringUtil.repeatChars('-', depth) + 
                        " " + item.getLabel();
            }
            
            public String getId() {
                return _expMan.composePathTo(item);
            }
        };
        addList.add(listItem);
        
        List children = _expMan.findAllChildren(ctx, item);
        for (Iterator i=children.iterator(); i.hasNext(); ) {
            ExplorerItem child = (ExplorerItem)i.next();
            
            flattenList(ctx, addList, child, depth + 1);
        }
    }
    
    public List getGroupList() {
        Integer sId = getBaseSessionBean().getWebUser().getSubject().getId();
        AuthzSubject subject = 
            AuthzSubjectManagerEJBImpl.getOne().findSubjectById(sId);
        SimpleExplorerContext ctx = new SimpleExplorerContext(subject);
        
        GroupManagerRootItem root = (GroupManagerRootItem) 
            _expMan.findChild(ctx, null, GroupManagerRootItemType.CODE); 
                              
        List res = new ArrayList();
        flattenList(ctx, res, root, 0);
        
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
