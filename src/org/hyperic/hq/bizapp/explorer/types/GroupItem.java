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

import java.util.Collections;
import java.util.List;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.bizapp.explorer.ExplorerItem;
import org.hyperic.hq.bizapp.explorer.ExplorerItemType;

public class GroupItem implements ExplorerItem {
    private final ExplorerItem  _parent;
    private final ResourceGroup _group;
    
    GroupItem(ExplorerItem parent, ResourceGroup group) {
        _parent = parent;
        _group  = group;
    }
    
    public ExplorerItem getParent() {
        return _parent;
    }

    public String getCode() {
        return GroupItemType.CODE_PREFIX + _group.getId();
    }
    
    public String getLabel() {
        return _group.getName();
    }
    
    public String toString() {
        return getLabel();
    }
    
    public List getChildren() {
        return Collections.EMPTY_LIST;
    }
    
    public ExplorerItemType getType() {
        return new GroupItemType();
    }
}
