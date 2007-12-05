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

package org.hyperic.hq.events.server.session;

/**
 * A singleton that schedules the deletion of expired events for the event 
 * tracker. It is expected that the event tracker will check with the 
 * scheduler each time it is possible that expired events may be deleted.
 */
class ExpiredEventsDeletionScheduler {
    
    private static final ExpiredEventsDeletionScheduler INSTANCE = 
        new ExpiredEventsDeletionScheduler();
    
    private static final int DELETION_FREQUENCY = 100;

    private final Object lock = new Object();
    
    private int counter;
    
    
    /**
     * Return the singleton instance.
     * 
     * @return The instance.
     */
    public static ExpiredEventsDeletionScheduler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Singletons should have a private constructor.
     */
    private ExpiredEventsDeletionScheduler() {
    }
    
    /**
     * Check if expired events should be deleted.
     * 
     * @return <code>true</code> if expired events should be deleted; 
     *         <code>false</code> to check again next time.
     */
    public boolean shouldDeleteExpiredEvents() {
        synchronized (lock) {
           if (++counter == DELETION_FREQUENCY) {
               counter = 0;
               return true;
           } else {
               return false;
           }
        }
    }

}
