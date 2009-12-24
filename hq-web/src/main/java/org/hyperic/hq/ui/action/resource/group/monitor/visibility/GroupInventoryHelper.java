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

package org.hyperic.hq.ui.action.resource.group.monitor.visibility;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;

/**
 * A class that provides application-specific implementations of utility methods
 * for common monitoring tasks.
 */
public class GroupInventoryHelper
    extends InventoryHelper {

    public GroupInventoryHelper(AppdefEntityID entityId) {
        super(entityId);
    }

    // ---------------------------------------------------- Public Methods

    /**
     * Get the set of child resource types representing a resource's child
     * resources.
     * 
     * @param request the http request
     * @param ctx the servlet context
     * @param resource the resource
     */
    public List getChildResourceTypes(HttpServletRequest request, ServletContext ctx, AppdefResourceValue resource)
        throws Exception {
        // groups have no children
        return null;
    }

    /**
     * Get a child resource type from the Bizapp (returns <code>null</code>).
     * 
     * @param request the http request
     * @param ctx the servlet context
     * @param id the id of the server type
     */
    public AppdefResourceType getChildResourceType(HttpServletRequest request, ServletContext ctx, AppdefEntityTypeID id)
        throws Exception {
        // groups have no children
        return null;
    }

    /**
     * Get from the Bizapp the numbers of children of the given resource.
     * Returns <code>null</code>, since groups have no children.
     * 
     * @param request the http request
     * @param resource the appdef resource whose children we are counting
     */
    public Map getChildCounts(HttpServletRequest request, ServletContext ctx, AppdefResourceValue resource)
        throws Exception {
        // groups have no children
        return null;
    }

    /**
     * Return a boolean indicating that minisubtabs should not be shown.
     */
    public boolean showPerformanceSubtabs() {
        return false;
    }

}
