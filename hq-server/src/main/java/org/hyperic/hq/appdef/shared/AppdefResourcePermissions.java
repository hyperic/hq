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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.authz.server.session.AuthzSubject;

/**
 * This object is used to represent a users permissions as they
 * relate to a specific appdef resource identified by an
 * AppdefEntityID. 
 */
public class AppdefResourcePermissions implements java.io.Serializable {

    private AuthzSubject subject;
    private AppdefEntityID entity;

    private boolean canCreateChild;
    private boolean canModify;
    private boolean canView;
    private boolean canRemove;
    private boolean canControl;
    private boolean canMeasure;
    private boolean canAlert;

    /**
     * @param subject - who
     * @param entityId - what
     * @param canView - can the user view the entity
     * @param canCreateChild - can the user create a child entity 
     * @param canModify - can the user modify the entity
     * @param canRemove - can the user remove the entity
     * @param canControl - can the user perform control actions on the entity
     * @param canMeasure - can the user look at measurement data for the entity
     */
    public AppdefResourcePermissions(AuthzSubject subject,
                                     AppdefEntityID eid,
                                     boolean canView,
                                     boolean canCreateChild,
                                     boolean canModify,
                                     boolean canRemove,
                                     boolean canControl,
                                     boolean canMeasure,
                                     boolean canAlert) {
        this.subject = subject;
        this.entity  = eid;
        this.canView = canView;
        this.canModify = canModify;
        this.canCreateChild = canCreateChild;
        this.canRemove = canRemove;
        this.canControl = canControl;
        this.canMeasure = canMeasure;
        this.canAlert = canAlert;
    }

    /**
     * Can the user view this resource
     */
    public boolean canView() {
        return canView;
    }

    /**
     * Can the user create a child object of this resource.
     * Children list:
     * Platform -> Servers
     * Servers -> Services
     * Note: Applications have no children. The addition of dependent
     * services is treated as a modification to the Application resource
     */
    public boolean canCreateChild() {
        return canCreateChild;
    }

    /**
     * Can the user modify the resource
     */
    public boolean canModify() {
        return canModify;
    }

    /**
     * Can the user remove this resource
     */
    public boolean canRemove() {
        return canRemove;
    }
    
    public boolean canAlert() {
        return canAlert;
    }
    
    /**
     * Can the user control this resource
     */
    public boolean canControl() {
        return canControl;
    }
    
    /**
     * Can the user monitor this resource
     */
    public boolean canMeasure() {
        return canMeasure;
    }
    
    public AuthzSubject getSubject() {
        return this.subject;
    }

    public AppdefEntityID getEntityId() {
        return this.entity;
    }

}
