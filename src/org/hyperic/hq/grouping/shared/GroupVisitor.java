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

package org.hyperic.hq.grouping.shared;

public interface GroupVisitor {

    /** VisitGroup operates on the elements of an entire group
     *  all at once in batch.
     * @param object instance implementing GroupValue interface.
     * @throws GroupVisitorException */
    public void visitGroup (GroupValue group)
                         throws GroupVisitorException;

    /** VisitGroupIncremental operates on the group elements of
     *  the group on an incremental bases (i.e. as they are added).
     * @param object instance of GroupElement
     * @throws GroupVisitorException */
    public void visitGroupIncremental(GroupEntry entry)
                         throws GroupVisitorException;

    /** Concrete groups must implement cloneable because in
     * most cases, the GroupManager (which populates the groups)
     * doesn't have any knowledge of the actual class that it
     * needs instances of. As the group objects are cloned, their
     * registered visitors must be cloned as well.
     * @throws CloneNotSupportedException (only as a future precaution) */
    public Object clone () throws CloneNotSupportedException;
}
