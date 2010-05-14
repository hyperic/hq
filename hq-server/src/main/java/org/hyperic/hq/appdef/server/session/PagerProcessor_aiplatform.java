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

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.dao.AIPlatformDAO;
import org.hyperic.util.pager.PagerEventHandler;
import org.hyperic.util.pager.PagerProcessorExt;

public class PagerProcessor_aiplatform implements PagerProcessorExt {
    private AIPlatformDAO aPlatformDAO = Bootstrap.getBean(AIPlatformDAO.class);

    public PagerProcessor_aiplatform () {}

    public PagerEventHandler getEventHandler () {
        return new AIPlatformPagerEventHandler();
    }

    public boolean skipNulls () { return true; }

    // Unused, but required by the Processer "Ext" interface version.
    public Object processElement ( Object o1, Object o2) {
        return processElement(o1);
    }

    public Object processElement (Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof AIPlatform) {
            AIPlatform aiplatform = (AIPlatform) o;
            AIPlatformValue value = aiplatform.getAIPlatformValue();
            // XXX scottmf, this needs to be refactored to not pass Objects
            // around and hide the backend implementation of DAOs from the
            // front end.  Unfortunately it won't be in 3.x, definitely 4.0
            return AIQSynchronizer.getAIQPlatform(
                aPlatformDAO, value)
                .getAIPlatformValue();
        }
        return o;
    }

    public class AIPlatformPagerEventHandler implements PagerEventHandler {
        public void init () {
            // do nothing
        }

        public void cleanup () {
            // do nothing
        }
    }
}
