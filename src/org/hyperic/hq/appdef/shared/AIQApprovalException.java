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

import org.hyperic.hq.common.ApplicationException;

/**
 * Defines application-level exceptions that can occur when approving AI
 * data.  For example:
 * <ol>
 *   <li> A platform is added to the AI Q as "changed" because some of
 *        its autodiscovered attributes differ from the existing matching
 *        platform in appdef.
 *   <li> Before these changes are approved and merged into appdef,
 *        the platform is manually removed from appdef by a user.
 *   <li> A user then tries to approve the changes in the AI Q.
 *        This will generate an AIQApprovalException because we
 *        can't update something that's not there anymore.
 * </ol>
 */
public class AIQApprovalException extends ApplicationException {


    /** User tried to approve a resource that was marked 
        "new" in the AIQ, but it was already added to appdef */
    public static final int ERR_ADDED_TO_APPDEF = 0;

    /** User tried to approve a resource that was marked 
        "changed" in the AIQ, but it was removed from appdef */
    public static final int ERR_REMOVED_FROM_APPDEF = 1;

    /** User tried to approve a resource that was marked 
        "new" in the AIQ, but it's parent object had not 
        yet been approved, so it cannot be approved. */
    public static final int ERR_PARENT_NOT_APPROVED = 2;

    /** User tried to approve a resource that was marked 
        "changed" in the AIQ, but it's parent object has
        been removed, so it cannot be approved. */
    public static final int ERR_PARENT_REMOVED = 3;

    public static final int ERR_APPLICATION = 8;

    private AppdefEntityID appdefEntityID;
    
    /** This is one of the ERR_XXX constants defined in this class */
    private int reason;

    public AIQApprovalException (AppdefEntityID id, int reason) {
        super();
        this.appdefEntityID = id;
        this.reason = reason;
    }

    public AIQApprovalException(AppdefEntityID id, String reason) {
        super(reason);
        this.appdefEntityID = id;
        this.reason = ERR_APPLICATION;
    }

    /**
     * Find out what resource we were trying to approve that caused
     * the error.
     * @return The appdef entity ID of the resource that caused the error.
     */
    public AppdefEntityID getAppdefEntityID () { return appdefEntityID; }

    /**
     * Returns the the ERR_XXX constant that indicates why the approval failed.
     * @return One of the ERR_XXX constants indicating why the approval failed.
     */
    public int getReason () { return reason; }
}
