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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.explorer.ExplorerContext;
import org.hyperic.hq.bizapp.explorer.ExplorerItem;
import org.hyperic.hq.bizapp.explorer.ExplorerItemType;

/**
 * Represents a single group and has no children.
 */
public class GroupItemType implements ExplorerItemType {
    private static final Log _log = 
        LogFactory.getLog(GroupItemType.class);
    
    public static final String NAME         = "Group";
    public static final String CODE_PREFIX  = "g=";
    
    public String getName() {
        return NAME;
    }
    
    public String toString() {
        return "A group";
    }
    
    public List getAdoptedChildren(ExplorerContext ctx, ExplorerItem parent) {
        return Collections.EMPTY_LIST;
    }
    
    public ExplorerItem getChild(ExplorerContext ctx, ExplorerItem parent, 
                                 String code) 
    {
        if (!code.startsWith(CODE_PREFIX)) {
            return null;
        }

        String sgroupId = code.substring(CODE_PREFIX.length());
        Integer groupId = Integer.valueOf(sgroupId);
        
        AuthzSubject subject = ctx.getSubject();
        ResourceGroup group;
        try {
            group = ResourceGroupManagerEJBImpl.getOne()
                .findResourceGroupById(subject, groupId);
        } catch(PermissionException e) {
            _log.warn("Permission denied, looking up group id=" + groupId +
                      " for user=[" + subject.getName() + " id=" + 
                      subject.getId() + "]");
            return null;
        }
        if (group == null)
            return null;
        
        return new GroupItem(parent, group);
    }
}
