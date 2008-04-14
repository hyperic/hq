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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.grouping.shared.GroupValue;
import org.hyperic.hq.grouping.shared.GroupVisitor;
import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.util.pager.PageList;

/** 
 *  Appdef GroupValue implements the grouping subsystem's GroupValue
 *  interface and provides the implementation for its methods.
 *  The class extends the domain of groupable elements to include
 *  appdef entities. The group entries are converted to and from
 *  appdef entities as required to meet the needs of consumers.
 *  Operations intended to be applied circumstantially should be
 *  manifested as visitors and registered for either immediate or
 *  incremental application.
 *
 *  Note, despite the "Value" name and implied value object pattern, this
 *  class is not necessarily light weight. It is likely that an instance
 *  of this class will hold references to one or more visitor
 *  class instances. This makes it an extremely bad candidate for,
 *  say, sticking in the session.
 * 
 *  (MOST of this junk belongs in an abstract base class in the grouping
 *  subsystem. However, the requirement is that this class MUST
 *  extend AppdefResourceValue and since Java doesn't allow
 *  multiple inheritance, well, we're stuck with this.)
 */
public class AppdefGroupValue
    extends AppdefResourceValue 
    implements GroupValue, Cloneable, Serializable 
{ 
    private Integer  id;            // id of group.
    private int      groupType;     // adhoc|compat|clusterable
    private int      groupEntType;  // platform|server|group|app|service
    private int      groupEntResType; // jboss|oracle,etc.
    private int      clusterId;     // clusterId
    private String   name;          // group name
    private String   description;   // group description
    private AuthzSubjectValue subject; // group owner
    private String   owner;         // String value of owner
    private PageList groupEntries;  // list of group entries
    private List     visitors;      // registered visitors
    private List     visitorsInc;   // registered incremental visitors.
    
    private Long    cTime;
    private Long    mTime;
    private String  modifiedBy;
    private String  location;
    
    // The appdef resource type value of this group.
    private AppdefResourceTypeValue appdefResourceTypeValue = null;
    
    public AppdefGroupValue() {
        init();
    }
    public AppdefGroupValue (Integer id) {
        this.id = id;
        init();
    }

    private void init () {
        groupEntries = new PageList();
        appdefResourceTypeValue = new GroupTypeValue();
        clusterId = -1;
    }

    /** The group identifier */
    public Integer   getId()          { return this.id; }
    public void      setId( Integer id ) { this.id = id;  }

    public AppdefEntityID getEntityId() {
        return AppdefEntityID.newGroupID(getId());
    }
    
    /** The group type (adhoc, compatible) */
    public int       getGroupType()   { return this.groupType; }
    public void      setGroupType( int groupType ) {
        this.groupType = groupType;
    }

    /** The group type label */
    public String getGroupTypeLabel(){
        return getAppdefResourceTypeValue().getName();
    }

    /** Test whether the group type is one of the adhoc types. */
    public boolean isGroupAdhoc () {
        return AppdefEntityConstants.isGroupAdhoc(this.groupType);
    }

    /** Test whether the group type is one of the compatible types. */
    public boolean isGroupCompat() {
        return AppdefEntityConstants.isGroupCompat(this.groupType);
    }

    /** The group entity type */
    public int       getGroupEntType() { return groupEntType; }
    public void      setGroupEntType( int groupEntType ) {
        this.groupEntType=groupEntType;
    }

    /** The group entity resource type */
    public int       getGroupEntResType() { return groupEntResType; }
    public void      setGroupEntResType( int groupEntResType ) {
        this.groupEntResType = groupEntResType;
    }

    public int       getClusterId () { return this.clusterId; }
    public void      setClusterId (int clusterId) { this.clusterId=clusterId; }

    /** The name of the group */
    public String    getName() { return this.name; }
    public void      setName( String name ) { this.name = name; }

    /** Group description */
    public String    getDescription() { return this.description; }
    public void      setDescription (String desc) { this.description=desc; }

    /** The group owner */
    public void      setSubject(AuthzSubjectValue s){ this.subject = s; }
    public AuthzSubjectValue getSubject(){ return subject; }

    /** The group size PageList.size() */
    public int       getSize() { return this.groupEntries.size(); }

    /** The group total size (PageList.getTotalSize()) */
    public int       getTotalSize() { 
        return this.groupEntries.getTotalSize(); 
    }

    /** The group total size (PageList.setTotalSize()) */
    public void      setTotalSize( int groupTotalSize ) {
        this.groupEntries.setTotalSize( groupTotalSize );
    }

    /** The PageList of group entries */
    public PageList  getGroupEntries() {return this.groupEntries; }

    /** Fetch the group members as a paged list of AppdefEntityIDs. The group
     *  will always contain a page list of values because ResourceGroupManager
     *  sees to it that all requests for group members receives a paged list.
     *  To specify a page control, pass it into the AppdefGroupManager's findGroup
     *  method (or to GroupUtil's) methods.
     *  @return paged list of members.
     */
    public PageList  getAppdefGroupEntries () {
        return getAppdefGroupEntries(null);
    }

    /** Fetch the group members as a paged list of AppdefEntityIDs. The group
     *  will always contain a page list of values because ResourceGroupManager
     *  sees to it that all requests for group members receives a paged list.
     *  To specify a page control, pass it into the AppdefGroupManager's findGroup
     *  method (or to GroupUtil's) methods.
     *  @return paged list of members.
     *  @param optional comparator
     */
    public PageList  getAppdefGroupEntries (Comparator comparator) {
        List entities = new ArrayList();
        for (Iterator i=getGroupEntries().iterator();i.hasNext();) {
            GroupEntry entry = (GroupEntry)i.next();
            entities.add( entryToEntity( entry ) );
        }
        if (comparator !=null)
            Collections.sort(entities,comparator);

        return new PageList(entities,getGroupEntries().getTotalSize());
    }

    public Long getCTime() {
        return cTime;
    }

    public Long getMTime() {
        return mTime;
    }

    public void setCTime(Long ctime) {
        this.cTime = ctime;
    }

    public void setMTime(Long mtime) {
        this.mTime = mtime;
    }

    /** Adds an entity identified by AppdefEntityID to our group. Conversion
     *  to the underlying group entry type (GroupEntry) will be automatic.
     *  Any registered incremental visitors will visit the GroupEntry.
     * @param appdef entity id
     * @throws GroupVisitorException
     * */
    public void addAppdefEntity ( AppdefEntityID entity )
        throws GroupVisitorException {
        addEntry(  entityToEntry(entity)  );
    }

    /** Asserts that an element exists in the group.
     * @return true if in group, false if not in group */
    public boolean existsAppdefEntity ( AppdefEntityID entity ) {
        return existsEntry( entityToEntry(entity) );
    }

    /** Asserts that an element exists in the group.
     * @return true if in group, false if not in group */
    public boolean existsEntry (GroupEntry entry) {
        for (Iterator i=this.getGroupEntries().iterator();i.hasNext();) {
            GroupEntry ge = (GroupEntry)i.next();
            if (ge.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /** 
     * Removes an entity identified by AppdefEntityID from our group.
     */
    public void removeAppdefEntity (AppdefEntityID entity) {
        removeEntry( entityToEntry(entity) );
    }

    /** Gets the appdef resource type value of this group. 
     * @return value object dowcasted to its base abstract class
     */
    public AppdefResourceTypeValue getAppdefResourceTypeValue( ) {
        return appdefResourceTypeValue;
    }

    /** Sets the appdef resource type value of this group. 
     * @param artv - The entity type value object.
     */
    public void setAppdefResourceTypeValue ( AppdefResourceTypeValue artv) {
        this.appdefResourceTypeValue = artv;
    }

    /** Adds an entry to the group.
     * @param group entry value object.
     * */
    public void addEntry (GroupEntry entry)
        throws GroupVisitorException 
    {
        // If there are any incremental vistors registered, dispatch.
        if (visitorsInc != null) {
            for (Iterator i=visitorsInc.iterator();i.hasNext();) {
                GroupVisitor gv = (GroupVisitor) i.next();
                gv.visitGroupIncremental(entry);
            }
        }
        groupEntries.add(entry);
    }

    /** 
     * Removes an entry from the group.
     */
    public void removeEntry (GroupEntry goner) {
        for (Iterator i=groupEntries.iterator();i.hasNext();) {
            GroupEntry ge = (GroupEntry)i.next();
            if (ge.equals(goner)) {
                i.remove();
            }
        }
    }

    /** Iterate through all registered visitors and invoke their
     * visitGroup method passing this group as a parameter. Operations
     * will be performed in order of registration.
     */
    public void visit () throws GroupVisitorException {
        if (visitors != null) {
            for (Iterator i=visitors.iterator();i.hasNext();) {
                GroupVisitor gv = (GroupVisitor) i.next();
                gv.visitGroup(this);
            }
        }
    }
    
    /** With the argument visitor immediately invoke its
     * visitGroup method passing this group as a parameter.
     */
    public void visit (GroupVisitor gv) throws GroupVisitorException {
        gv.visitGroup(this);
    }

    public void clearVisitors() {
        if (visitors!=null)
            visitors.clear();
    }

    public void clearVisitorsInc() {
        if (visitorsInc!=null)
            visitorsInc.clear();
    }

    /** Register a visitor with this group value object for later
     *  visitation.
     */
    public void registerVisitor (GroupVisitor gv) {
        if (visitors == null)
            visitors = new ArrayList();
        visitors.add(gv);
    }

    /** Register an incremental visitor with this group value object
     * for later visitation. Visitation occurs automatically after
     * an add operation.
     */
    public void registerVisitorInc (GroupVisitor gv) {
        if (visitorsInc == null)
            visitorsInc = new ArrayList();
        visitorsInc.add(gv);
    }

    // utility method for converting back and forth from appdef to authz
    private GroupEntry entityToEntry (AppdefEntityID entity) {
        return  new GroupEntry ( entity.getId(),
                                 AppdefUtil.appdefTypeIdToAuthzTypeStr(
                                 entity.getType()));
    }
    // utility method for converting back and forth from appdef to authz
    private AppdefEntityID entryToEntity (GroupEntry entry) {
        return new AppdefEntityID ( AppdefUtil.resNameToAppdefTypeId(
                                    entry.getType()),
                                    entry.getId().intValue());
    }


   public String toString() {
        StringBuffer sb = new StringBuffer(AppdefGroupValue.class.getName());
        sb.append("[groupId=").append(getId().intValue());
        sb.append(",groupeType=").append(getGroupType());
        sb.append(",name=").append(getName());
        sb.append(",description=").append(getDescription());
        sb.append(",elements=(").append(getGroupEntries().toString());
        sb.append(")] super: ");
        sb.append(super.toString());
        return sb.toString();
   }

   public boolean equals(Object other)  {
      if (other instanceof AppdefGroupValue) {
         AppdefGroupValue that = (AppdefGroupValue) other;

         // Cannot perform equals if groupid is not defined.
         if (that.getId() == null || this.getId()==null )
            return false;

         return this.id.equals( that.id );
      } else  {
         return false;
      }
   }

   public int hashCode(){
      int result = 17;
      result = 37*result + (int) groupType;
      result = 37*result + ((this.description != null) ? 
                            this.description.hashCode() : 0);
      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);
      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);
      // XXX incorporate hashcode from individual entries.
      return result;
   }

    /** Owner - not applicable, here only to satisfy interface. */
    public String    getOwner() { return owner; }
    public void      setOwner (String s) { this.owner = s;  }

    /** ModifiedBy - now applicable */
    public String    getModifiedBy() { return this.modifiedBy; }
    public void      setModifiedBy (String s) { this.modifiedBy = s;  }

    /** Location - now applicable */
    public String    getLocation() { return this.location; }
    public void      setLocation (String s) { this.location=s; }

    /** A deep copy clone implementation specifically addresses the need
     * for the GroupManager to have a way to create new instances of the
     * concrete groups for which it has no class definition.
     * @throws CloneNotSupportedException
     * */
    public Object clone () throws CloneNotSupportedException {
        AppdefGroupValue newGroupVo = (AppdefGroupValue) super.clone();
        newGroupVo.clearVisitors();

        if (visitors != null)
            for (Iterator i = this.visitors.iterator();i.hasNext();)
                newGroupVo.registerVisitor((GroupVisitor) i.next());

        if (visitorsInc != null)
            for (Iterator i= this.visitorsInc.iterator();i.hasNext();)
                newGroupVo.registerVisitorInc((GroupVisitor)i.next());

        newGroupVo.init(); // reset any state.

        return newGroupVo;
    }
}
