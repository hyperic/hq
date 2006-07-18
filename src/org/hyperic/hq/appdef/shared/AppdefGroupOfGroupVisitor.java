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

package org.hyperic.hq.appdef.shared;

import java.io.Serializable;
import java.util.Iterator;

import org.hyperic.hq.authz.shared.AuthzConstants;

import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.hq.grouping.shared.GroupVisitor;
import org.hyperic.hq.grouping.shared.GroupValue;
import org.hyperic.hq.grouping.shared.GroupEntry;

/** AppdefGroupOfGroupVisitor operates on a group of groups
 *  enforcing that each added member be of type "group". 
 *  It implements the grouping subsystem's GroupVisitor interface 
 *  which enables operations to be performed either incrementally
 *  or postponed until group save.
 * 
*/
public class AppdefGroupOfGroupVisitor implements GroupVisitor,
        Serializable {

    public AppdefGroupOfGroupVisitor () {}

   /**
    * Visit the entire group and perform operations on each member.
    * @param the group value object.
    * @throws GroupVisitorException
    * */
    public void visitGroup (GroupValue groupOfGroups)
        throws GroupVisitorException {

     // Iterate around group entries and validate their types.
        String type = null;
        for (Iterator i=groupOfGroups.getGroupEntries().iterator();
             i.hasNext();) {
            GroupEntry entry = (GroupEntry) i.next();
            if (!entry.getType().equals(AuthzConstants.groupResType)) {
                throw new GroupVisitorException ("Invalid entity type detected."+
                                                 "\"Groups of Groups\" may only "+
                                                 "contain other groups.");
            }
        }
    }

    /**
     * Visit and incrementally operate on the group entry.
     * on the group entry.
     * @param the group entry object.
     * @throws GroupVisitorException
     * */
    public void visitGroupIncremental (GroupEntry entry)
        throws GroupVisitorException {
        if (!entry.getType().equals(AuthzConstants.groupResType)) {
            throw new GroupVisitorException ("Invalid entity type detected."+
                                             "\"Groups of Groups\" may only "+
                                             "contain other groups.");
        }
    }

    public Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

}
