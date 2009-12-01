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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.data.IResourceTreeNode;
import org.hyperic.util.data.ITreeNode;

/**
 * Implementation of the resource tree node interface for rendering
 * the navigation map.
 *
 */
public class ResourceTreeNode extends TreeNode implements IResourceTreeNode {
    private int type;

    private AppdefEntityID rEntityId;
    private AppdefEntityID[] pEntityIds;
    private int ctype = NO_CTYPE;
    private boolean promotable;

    /**
     * Creates a promotable <code>ResourceTreeNode</code> instance for a
     * resource. The resource may be promoted to an auto-group with one
     * parent resource at a later time.
     *
     * @param name the name
     * @param desc the description
     * @param rEntityId the resource's entity id
     * @param pEntityId the Appdef parent entity id
     * @param ctype the ctype
     * @param type the type of this node
     */
    public ResourceTreeNode(String name, String desc, AppdefEntityID rEntityId,
                            AppdefEntityID pEntityId, int ctype ) {
        super(name, desc);
        this.pEntityIds = new AppdefEntityID[] { pEntityId };
        this.rEntityId = rEntityId;
        this.ctype = ctype;
        this.type = ResourceTreeNode.RESOURCE;
        this.promotable = true;
    }

    /**
     * Creates a promotable <code>ResourceTreeNode</code> instance for a 
     * resource. The resource may be promoted to an auto-group at a later
     * time.
     *
     * @param name the name
     * @param desc the description
     * @param rEntityId the resource's entity id
     * @param pEntityIds the Appdef parent entity ids
     * @param ctype the ctype
     */
    public ResourceTreeNode(String name, String desc, AppdefEntityID rEntityId,
                            AppdefEntityID[] pEntityIds, int ctype ) {
        super(name, desc);
        this.pEntityIds = pEntityIds;
        this.rEntityId = rEntityId;
        this.ctype = ctype;
        this.type = ResourceTreeNode.RESOURCE;
        this.promotable = true;
    }
    
    /**
     * Creates a new <code>ResourceTreeNode</code> instance for an
     * auto-group with one parent resource.
     *
     * @param name the name
     * @param desc the description
     * @param pEntityId the Appdef parent entity id
     * @param ctype the ctype
     * @param type the type of this node
     */
    public ResourceTreeNode(String name, String desc, AppdefEntityID pEntityId,
                            int ctype, int type) {
        super(name, desc);
        this.pEntityIds = new AppdefEntityID[] { pEntityId };
        this.rEntityId  = null;
        this.ctype = ctype;
        assertValidType(type);
        this.type = type;
        this.promotable = false;
    }

    /**
     * Creates a new <code>ResourceTreeNode</code> instance for an
     * auto-group with multiple parent resources. 
     *
     * @param name the name
     * @param desc the description
     * @param pEntityIds the Appdef parent entity ids
     * @param ctype the ctype
     * @param type the type of this node
     */
    public ResourceTreeNode(String name, String desc, AppdefEntityID[] pEntityIds, 
                            int ctype, int type) {
        super(name, desc);
        this.pEntityIds = pEntityIds;
        this.rEntityId  = null;
        this.ctype = ctype;
        assertValidType(type);
        this.type = type;
        this.promotable = false;
    }

    /**
     * Creates a new <code>ResourceTreeNode</code> instance for a
     * resource.
     *
     * @param name the name
     * @param desc the description
     * @param pEntityIds the Appdef parent entity ids
     * @param ctype the ctype
     * @param type the type of this node
     */
    public ResourceTreeNode(String name, String desc, AppdefEntityID rEntityId,
                            int type ) {
        super(name, desc);
        this.rEntityId  = rEntityId;
        this.pEntityIds = null;
        this.ctype = NO_CTYPE;
        assertValidType(type);
        this.type = type;
        this.promotable = false;
    }

    public static void autoGroupData (ResourceTreeNode[] data) {
        // only first node contains children
        ResourceTreeNode parent = data[0]; 
        // Process up and down children, promoting duplicate resources
        // into autogroups as applicable
        ITreeNode[] children = promoteResources(parent.getUpChildren());
        Arrays.sort(children,new TreeNodeAlphaComparator(true));
        for (int x=0;x<children.length;x++) {
            children[x].reset();
        }
        parent.replaceUpChildren(children);

        children = promoteResources(parent.getDownChildren());
        Arrays.sort(children,new TreeNodeAlphaComparator());
        for (int x=0;x<children.length;x++) {
            children[x].reset();
        }
        parent.replaceDownChildren(children);
    }

    public static void alphaSortNodes (ResourceTreeNode[] children) {
        alphaSortNodes(children,false);
    }

    public static void alphaSortNodes (ResourceTreeNode[] children, 
        boolean reverse) {
        Arrays.sort(children,new TreeNodeAlphaComparator(reverse));
    }

    private static ITreeNode[] promoteResources (ITreeNode[] children) {
        // Process downchildren
        Map typeMap;
        List childrenList;
        List newChildrenList;

        typeMap = new LinkedHashMap();
        newChildrenList = new ArrayList();
        if (children != null) {
            childrenList = java.util.Arrays.asList(children);
            for (Iterator i=childrenList.iterator();i.hasNext();) {
                ResourceTreeNode rtn = (ResourceTreeNode)i.next();
                if (rtn.isPromotable()) {
                    if (typeMap.containsKey(rtn.getDescription())) {
                        rtn.promote();
                    }
                    typeMap.put(rtn.getDescription(),rtn);
                } else {
                    newChildrenList.add(rtn);
                }
            }
            newChildrenList.addAll(typeMap.values());
            children = (ITreeNode[]) newChildrenList.toArray(new ITreeNode[0]);
        }
        /* debug 
        System.out.println ("promoteResources RETURNS "+children.length+" CHILDREN");
        for (int x=0;x<children.length;x++) {
            System.out.println ("child:"+children[x].toString());
        } */
        return children;
    }

    /**
     * If our type is a resource, then we return the resource ids.
     * If our type is an AUTO_GROUP then we return the parent ids.
     */
     public AppdefEntityID[] getEntityIds () {
         if (type == AUTO_GROUP) {
             return pEntityIds;
         } else {
             return new AppdefEntityID[] { rEntityId };
         }
     }

    /**
     * Return the type of this node.
     *
     * @return the node type
     */
    public int getType() {
        return type;
    }
    
    /**
     * Set the type of this node.
     *
     * @param type the node type
     */
    public void setType(int type) {
        this.type = type;
    }

   /**
     * Is this node promotable to an auto-group
     *
     * @return true if promotable, false if not
     */
    public boolean isPromotable () {
        return (type == RESOURCE && this.promotable);
    }
    
    /**
     * Overridden from <code>TreeNode</code> to ensure that the array
     * is of type IResourceTreeNode[].
     */
    public ITreeNode[] getUpChildren() {
        return (IResourceTreeNode[])
            upChildren.toArray(new IResourceTreeNode[0]);
    }
    
    /**
     * Overridden from <code>TreeNode</code> to ensure that the array
     * is of type IResourceTreeNode[].
     */
    public ITreeNode[] getDownChildren() {
        return (IResourceTreeNode[])
            downChildren.toArray(new IResourceTreeNode[0]);
    }
    
    /**
     * Get the resource's Appdef entity id.
     *
     * @return rEntityId Appdef entity id
     */
    public AppdefEntityID getREntityId() {
        return rEntityId;
    }

    /**
     * Get the parents' Appdef entity ids.
     *
     * @return pEntityIds the Appdef entity ids
     */
    public AppdefEntityID[] getPEntityIds() {
        return pEntityIds;
    }

    /**
     * Set the resource's Appdef entity id.
     *
     * @param pEntityIds the Appdef entity id
     */
    public void setREntityId(AppdefEntityID rEntityId) {
        this.rEntityId = rEntityId;
    }

    /**
     * Set the parents' Appdef entity ids.
     *
     * @param pEntityIds the Appdef entity ids
     */
    public void setPEntityIds(AppdefEntityID[] pEntityIds) {
        this.pEntityIds = pEntityIds;
    }

    /**
     * Get the ctype.
     *
     * @return the ctype
     */
    public int getCtype() {
        return ctype;
    }

    /**
     * Set the ctype.
     *
     * @param ctype the ctype
     */
    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

   /** 
    * Determines whether two objects are equal.
    *
    * @return true or false
    */
    public boolean equals (Object o) {
        if (!(o instanceof ResourceTreeNode)) {
            return false;
        }
        ResourceTreeNode other = (ResourceTreeNode)o;
        if ( super.equals((Object)other)         &&
             other.getCtype() == this.getCtype() &&
             other.getType() == this.getType()   &&
             other.getUpChildren().length   == this.getUpChildren().length   &&
             other.getDownChildren().length == this.getDownChildren().length ){
            return true;
        }
        return false;
    }

   /** 
    * Returns the hashcode of this object.
    *
    * @return hash code.
    */
   public int hashCode(){
      int result = 17 + super.hashCode();
      int pri = 37;
      result = pri * result + this.ctype;
      result = pri * result + this.type;
      result = pri * result + getUpChildren().length; 
      result = pri * result + getDownChildren().length; 
      if (getREntityId()!=null) {
          result = pri * result + getREntityId().getID();
          result = pri * result + getREntityId().getType();
      }
      if (getPEntityIds() != null) {
          result = pri * result + getPEntityIds().length;
          for (int x=0;(getPEntityIds()!=null && x<getPEntityIds().length);x++) {
              result = pri * result +  this.getPEntityIds()[x].hashCode();
          }
      }
      return result;
   }

    /**
     * Returns true if this node is an autogroup and has a ctype, 
     * false otherwise.
     *
     * @return true or false
     */
    public boolean hasCtype() {
        if (type == AUTO_GROUP) {
            return (ctype != NO_CTYPE);
        } else {
            return false;
        }
    }

    /**
     * Promote a resource node to an auto-group node.
     */
    public void promote () {
        if (isPromotable()) {
            setName(getDescription()); // an auto-group's name is same as desc
            this.type = AUTO_GROUP;    // change our type to AG
            this.promotable = false;
        }
    }

    /**
     * Return a string representation of this node.
     *
     * @return string representation of this node
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[")
            .append(" name=").append( getName() )
            .append(" desc=").append( getDescription() )
            .append(" ctype=").append( getCtype() )
            .append(" type=").append( getType() )
            .append(" rid=").append( getREntityId() )
            .append(" pids=").append( getPEntityIds() )
            .append(" up-children: ").append( getUpChildren().length )
            .append(" down-chilren: ").append( getDownChildren().length )
            .append(" ]");
        return buf.toString();
    }

    //------------------------------------------------------------------------------------
    //-- private helpers
    //------------------------------------------------------------------------------------
    private void assertValidType(int type) {
        if (! (type >= NONE && type <= CLUSTER) ) {
            throw new IllegalArgumentException("Invalid Type: " + type);
        }
    }
}

// EOF
