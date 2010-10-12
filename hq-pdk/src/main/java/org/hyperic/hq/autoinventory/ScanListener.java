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

package org.hyperic.hq.autoinventory;

import org.hyperic.hq.common.SystemException;

/**
 * An interface that lets you listen in on various events 
 * that occur during an autoinventory scan.
 */
public interface ScanListener {

    /**
     * The scanner calls this method when the scan has completed.
     * This method is called whenever the scan completes, for ANY
     * reason (including partial errors and fatal errors).  It
     * is the responsbility of the class that implements this
     * interface to investigate the Scanner's ScanState object
     * to determine how the scan completed and take the appropriate
     * course of action.
     * @param state The final state of the scan.  This object is
     * provided for convenience, as the class implementing
     * this interface will likely already have a way to get to it.
     */
    public void scanComplete (ScanState state) 
        throws AutoinventoryException, SystemException;
}
