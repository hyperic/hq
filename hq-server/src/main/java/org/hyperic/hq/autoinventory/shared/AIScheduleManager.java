/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.autoinventory.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AIScheduleManager .
 */
public interface AIScheduleManager {
    /**
     * Schedule an AI scan on an appdef entity (platform or group of platforms)
     */
    public void doScheduledScan(AuthzSubject subject, AppdefEntityID id, ScanConfigurationCore scanConfig,
                                String scanName, String scanDesc, ScheduleValue schedule)
        throws AutoinventoryException, DuplicateAIScanNameException, ScheduleWillNeverFireException;

    /**
     * Get a list of scheduled scans based on appdef id
     */
    public PageList<AIScheduleValue> findScheduledJobs(AuthzSubject subject, AppdefEntityID id, PageControl pc) throws NotFoundException;

    public AISchedule findScheduleByID(AuthzSubject subject, Integer id);

    /**
     * Get a job history based on appdef id
     */
    public PageList<AIHistory> findJobHistory(AuthzSubject subject, AppdefEntityID id, PageControl pc) throws NotFoundException;

    public void deleteAIJob(AuthzSubject subject, java.lang.Integer[] ids) throws AutoinventoryException;

}
