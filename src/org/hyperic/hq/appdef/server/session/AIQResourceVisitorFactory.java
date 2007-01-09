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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AIQueueConstants;

public class AIQResourceVisitorFactory {

    private static final AIQResourceVisitor RV_DEFER    = new AIQRV_defer();
    private static final AIQResourceVisitor RV_APPROVE  = new AIQRV_approve();
    private static final AIQResourceVisitor RV_IGNORE   = new AIQRV_ignore();
    private static final AIQResourceVisitor RV_UNIGNORE = new AIQRV_unignore();
    private static final AIQResourceVisitor RV_PURGE    = new AIQRV_purge();

    public static AIQResourceVisitor getVisitor (int action) {
        switch (action) {
        case AIQueueConstants.Q_DECISION_DEFER:
            return RV_DEFER;
        case AIQueueConstants.Q_DECISION_APPROVE:
            return RV_APPROVE;
        case AIQueueConstants.Q_DECISION_IGNORE:
            return RV_IGNORE;
        case AIQueueConstants.Q_DECISION_UNIGNORE:
            return RV_UNIGNORE;
        case AIQueueConstants.Q_DECISION_PURGE:
            return RV_PURGE;
        default:
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }
}
