/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.pager.PageList;

/** Interface specification for group value objects. Provides access
 * to group and elements for persistence.
 * 
 * (This really should be an abstract base class and should aggregate
 *  methods like registration of visitors, etc. However, this was not
 *  possible due to the necessity of its sole implementor
 *  (AppdefGroupValue) to subclass AppdefResourceValue.
 * 
*/
public interface GroupValue {

    /** The group identifier */
    public Integer   getId();
    public void      setId( Integer id );

    /** The group type (adhoc, compatible) */
    public int       getGroupType();
    public void      setGroupType( int groupType );

    /** The group entity type */
    public int       getGroupEntType();
    public void      setGroupEntType( int groupEntType );

    /** The group entity resource type */
    public int       getGroupEntResType();
    public void      setGroupEntResType( int groupEntResType );

    /** The group's clusterId */
    public int       getClusterId ();
    public void      setClusterId (int clusterId);

    /** The name of the group */
    public String    getName();
    public void      setName( String name );

    /** Group description */
    public String    getDescription();
    public void      setDescription (String desc);

    /** Group location */
    public String    getLocation();
    public void      setLocation(String location);

    /** The group owner */
    public void      setSubject(AuthzSubject s);
    public AuthzSubject getSubject();

    /** The group size */
    public int       getSize();

    /** The group total size */
    public int       getTotalSize();
    public void      setTotalSize( int groupTotalSize );

    /** The list of group entries */
    public PageList  getGroupEntries();

    /** Adds an entry to the group.
     * @param group entry value object.
     * @throws ApplicationException
     * */
    public void addEntry (GroupEntry entry) throws GroupVisitorException;

    /** Asserts that an entry exists in the group */
    public boolean existsEntry (GroupEntry entry);

    /** Removes an entry from the group.
     * @param id of entry to remove.
     * @throws ApplicationException
     * */
    public void removeEntry (GroupEntry entry);

    /** Iterate through all registered visitors and invoke their
     * visitGroup method passing this group as a parameter. Operations
     * will be performed in order of registration.
     * @throws ApplicationException
     * */
    public void visit () throws GroupVisitorException;
    public void visit (GroupVisitor gv) throws GroupVisitorException;
    public void clearVisitors();
    public void clearVisitorsInc();

    /** Register a visitor with this group value object for later
     * visitation. Visitation is delayed until visit() is invoked.
     * @param object implementing GroupVisitor interface.
     * */
    public void registerVisitor (GroupVisitor gv);

    /** Register an incremental visitor with this group value object
     * for later visitation. Visitation occurs automatically after
     * an add operation.
     * @param object implementing GroupVisitor interface.
     * */
    public void registerVisitorInc (GroupVisitor gv);

    public boolean equals(Object other);
    public int hashCode();

    /** Concrete groups must implement cloneable because in
     * most cases, the GroupManager (which populates the groups)
     * doesn't have any knowledge of the actual class that it
     * needs new instances of.
     * @throws CloneNotSupportedException (only as a future precaution) */
    public Object clone() throws CloneNotSupportedException;

    /* These are now recognized in authz */
    public String    getModifiedBy();
    public void      setModifiedBy (String s);
    public String    getOwner();
    public void      setOwner (String s);
    public Long      getCTime();
    public void      setCTime( Long l);
    public Long      getMTime();
    public void      setMTime( Long l);


}
