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
package org.hyperic.hq.bizapp.explorer.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.bizapp.explorer.ExplorerContext;
import org.hyperic.hq.bizapp.explorer.ExplorerItem;
import org.hyperic.hq.bizapp.explorer.ExplorerItemType;
import org.hyperic.hq.common.SystemException;

public class GroupManagerRootItem implements ExplorerItem {
    private final ResourceGroupManagerLocal _groupMan =
        ResourceGroupManagerEJBImpl.getOne();
    
    private final ExplorerContext _ctx;
    private final ExplorerItem    _parent;
    private List _groupItems;
    
    GroupManagerRootItem(ExplorerContext ctx, ExplorerItem parent) {
        _ctx    = ctx;
        _parent = parent;
    }
    
    public ExplorerItem getParent() {
        return _parent;
    }

    public String getCode() {
        return GroupManagerRootItemType.CODE;
    }
    
    public String getLabel() {
        return "Groups";
    }
    
    public String toString() {
        return getLabel();
    }

    public ExplorerItemType getType() {
        return new GroupManagerRootItemType();
    }

    private void lazyInitialize() {
        if (_groupItems != null)
            return;

        Collection groups;
        try {
            groups = _groupMan.getAllResourceGroups(_ctx.getSubject(), true);
        } catch(PermissionException e) {
            throw new SystemException("Should not get a permission exception!", 
                                      e);
        }
        
        List sorted = new ArrayList(groups);
        Collections.sort(sorted, new Comparator() {
            public int compare(Object o1, Object o2) {
                ResourceGroup g1 = (ResourceGroup)o1;
                ResourceGroup g2 = (ResourceGroup)o2;
                
                return g1.getName().compareTo(g2.getName());
            }
        });
        
        _groupItems = new ArrayList(sorted.size());
        for (Iterator i=sorted.iterator(); i.hasNext(); ) {
            ResourceGroup g = (ResourceGroup)i.next();
            
            _groupItems.add(new GroupItem(this, g));
        }
        _groupItems = Collections.unmodifiableList(_groupItems);
    }
    
    public List getChildren() {
        lazyInitialize();
        return _groupItems;
    }
}
