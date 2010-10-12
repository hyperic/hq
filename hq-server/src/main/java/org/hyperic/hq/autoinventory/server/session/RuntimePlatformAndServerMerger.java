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

package org.hyperic.hq.autoinventory.server.session;

import java.util.List;

import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;

public interface RuntimePlatformAndServerMerger {
    List<ServiceMergeInfo> mergePlatformsAndServers(String agentToken, CompositeRuntimeResourceReport crrr)
        throws ApplicationException, AutoinventoryException;

    void reportAIRuntimeReport(String agentToken, CompositeRuntimeResourceReport crrr) throws AutoinventoryException,
        PermissionException, ValidationException, ApplicationException;

    void schedulePlatformAndServerMerges(String agentToken, CompositeRuntimeResourceReport crrr);

}
