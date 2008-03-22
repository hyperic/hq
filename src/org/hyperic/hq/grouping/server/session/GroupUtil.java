/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.grouping.server.session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefCompatGrpComparator;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

public class GroupUtil {

    /** Get the members of a compatible group. This method will 
     *  complain if the group is not a compatible group.
     */
    public static List getCompatGroupMembers(AuthzSubject subject,
                                             AppdefEntityID entity, 
                                             int[] orderSpec) 
        throws AppdefEntityNotFoundException, PermissionException, 
               GroupNotCompatibleException {
        return GroupUtil.getCompatGroupMembers(subject,entity,orderSpec,null);
    }
    public static List getCompatGroupMembers(AuthzSubject subject,
                                             AppdefEntityID entity,
                                             int[] orderSpec, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException,
               GroupNotCompatibleException {
        List retVal;
        AppdefGroupValue agv;
        Comparator comparator;

        agv = GroupUtil.getGroup(subject,entity,pc);
        if ( !agv.isGroupCompat() ) {
            throw new GroupNotCompatibleException(entity);
        }

        if (orderSpec != null) { 
            comparator = (Comparator) new AppdefCompatGrpComparator(orderSpec);
        } else {
            comparator = null;
        }

        retVal = agv.getAppdefGroupEntries(comparator);

        if (retVal == null)
            retVal = new ArrayList();

        return retVal;
    }

    public static List getGroupMembers(AuthzSubject subject,
                                       AppdefEntityID entity, 
                                       int[] orderSpec)
        throws AppdefEntityNotFoundException, PermissionException {
        return GroupUtil.getGroupMembers(subject,entity,orderSpec,null);
    }
    
    public static PageList getGroupMembers(AuthzSubject subject,
                                           AppdefEntityID entity,
                                           int[] orderSpec,
                                           PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {

        AppdefGroupValue agv;
        Comparator comparator;

        agv = GroupUtil.getGroup(subject, entity, pc);

        if (orderSpec != null) { 
            comparator = (Comparator) new AppdefCompatGrpComparator(orderSpec);
        } else {
            comparator = null;
        }

        PageList retVal = agv.getAppdefGroupEntries(comparator);

        if (retVal == null)
            retVal = new PageList();

        return retVal;
    }

    public static AppdefGroupValue getGroup (AuthzSubject subject,
                                             AppdefEntityID entity )
        throws AppdefEntityNotFoundException, PermissionException {
        return GroupUtil.getGroup(subject,entity,null);
    }

    private static AppdefGroupValue getGroup (AuthzSubject subject,
                                              AppdefEntityID entity,
                                              PageControl pc )
        throws AppdefEntityNotFoundException, PermissionException {
        AppdefGroupManagerLocal groupMgr = AppdefGroupManagerEJBImpl.getOne();
        return groupMgr.findGroup(subject, entity);
    }
}
