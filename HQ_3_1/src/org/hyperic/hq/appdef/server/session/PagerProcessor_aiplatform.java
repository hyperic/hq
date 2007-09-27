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

import org.hyperic.util.pager.PagerProcessorExt;
import org.hyperic.util.pager.PagerEventHandler;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIQueueManagerUtil;
import org.hyperic.hq.autoinventory.AIPlatform;

public class PagerProcessor_aiplatform implements PagerProcessorExt {

    protected AIQueueManagerLocal aiqManagerLocal;

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

        if (o == null) return null;
        try {
            if (o instanceof AIPlatform) {
                // Resync to appdef
                AIPlatform aiplatform = (AIPlatform) o;
                AIPlatformValue value = aiplatform.getAIPlatformValue();
                value = aiqManagerLocal.syncQueue(value, false);
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Error converting to " +
                                            "AIPlatformValue: " + e);
        }
        return o;
    }

    public class AIPlatformPagerEventHandler implements PagerEventHandler {

        public AIPlatformPagerEventHandler () {}

        public void init () {
            try {
                aiqManagerLocal = AIQueueManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                IllegalStateException ise
                    = new IllegalStateException("Could not create " +
                                                "AIQManagerLocal:" + e);
                ise.initCause(e);
                throw ise;
            }
        }

        public void cleanup () {
            // do nothing
        }
    }
}
