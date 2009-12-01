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

package org.hyperic.hq.appdef.shared.pager;

import org.hyperic.util.pager.PagerEventHandler;
import org.hyperic.util.pager.PagerProcessorExt;

/** A pager processor which allows a customized set of 
 * filters to be applied to the paging process. This
 * allows for dynamic specification of inclusionary or
 * exclusionary paging criteria.
 */
public class AppdefPagerProc implements PagerProcessorExt {

    public AppdefPagerProc () {}

    /** Get the event handler associated with the processor.
     *  (NOT PRESENTLY USED)
     */
    public PagerEventHandler getEventHandler () {
        PagerEventHandler handler = null;
        return handler;
    }

    /** This processor always expects the pager to skip nulls
     * @return true always
     */
    public boolean skipNulls () {
        return true;
    }

    /** This method satisfies the interface but is essentially a "no-op".
     * @param inbound object
     * @return outbound object or null
     */
    public Object processElement ( Object objIn ) {
        return processElement (objIn,null);
    }

    /** Process the element according to our filter list and return the
     * object if not caught by the filter.
     * @param objIn - object to process
     * @param jobData - A upcasted instance of AppdefPagerFilter[].
     * @return non-filtered objects or null
     */
    public Object processElement ( Object objIn, Object jobData  ) {

        if (jobData == null) {
            return objIn;
        }
 
        AppdefPagerFilter[] filters = (AppdefPagerFilter[]) jobData;

        for (int i=0; i<filters.length; i++) {
            if ( filters[i].isCaught( objIn ) ) {
                return null;
            }
        }
        return objIn;
    }
}
