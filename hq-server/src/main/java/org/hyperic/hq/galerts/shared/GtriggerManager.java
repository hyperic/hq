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
package org.hyperic.hq.galerts.shared;

import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;

/**
 * Local interface for GtriggerManager.
 */
public interface GtriggerManager {

    public GtriggerTypeInfo findTriggerType(GtriggerType type);

    /**
     * Register a trigger type.
     * @param triggerType Trigger type to register
     * @return the persisted metadata about the trigger type
     */
    public GtriggerTypeInfo registerTriggerType(GtriggerType triggerType);

    /**
     * Unregister a trigger type. This method will fail if any alert definitions
     * are using triggers of this type.
     * @param triggerType Trigger type to unregister
     */
    public void unregisterTriggerType(GtriggerType triggerType);

}
