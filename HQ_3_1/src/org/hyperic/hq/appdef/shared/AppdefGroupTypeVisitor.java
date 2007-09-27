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

import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.hq.grouping.shared.GroupVisitor;
import org.hyperic.hq.grouping.shared.GroupValue;
import org.hyperic.hq.grouping.shared.GroupEntry;

/** AppdefGroupTypeVisitor operates on appdef groups and enforces 
 *  type and resource type compatibility. It implements the GroupVisitor
 *  interface which enables its operations to be registered as a
 *  callback.
 * 
*/
public class AppdefGroupTypeVisitor implements GroupVisitor,
        Serializable {
    private int entType = -1;
    private int entResType = -1;

    public AppdefGroupTypeVisitor (int entType ) { 
        this.entType = entType; 
    }

    public AppdefGroupTypeVisitor (int entType, int entResType) {
        this.entType = entType;
        this.entResType = entResType;
    }

   /**
    * Visit the entire group and enforce type on all members.
    * @param groupValue - the group value object.
    * @throws GroupVisitorException
    * */
    public void visitGroup (GroupValue groupValue)
        throws GroupVisitorException {

    }

   /**
    * Visit an incrementally added group entry. Ensure that this entity
    * adheres to either a strict group definition (i.e. all members have
    * same entity type.) or a compatible group definition (i.e. all members
    * have same entity type AND entity resource type).
    * @param groupEntry - An instance of AppdefEntityID
    * @throws GroupVisitorException
    */
   public void visitGroupIncremental (GroupEntry groupEntry)
       throws GroupVisitorException {

   }


    public Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

}
