/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2013], VMware, Inc.
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

package org.hyperic.hq.measurement.agent.server;

import java.util.concurrent.atomic.AtomicLong;


/**
 * This class holds the time offset between the agent and the server, used for
 * deducting the timestamp of metric values in case the agent and the server
 * clock are not synced - Should prevent the problem that agents are sending
 * metric data to the server but users don't see anything in the UI.
 * 
 */
class ServerTimeDiff {

    public static final int MIN_OFFSET_FOR_DEDUCTION = 30 * 1000;
    public static final String PROP_DEDUCT_SERVER_TIME_DIFF = "agent.deductServerTimeDiff";

    private static final ServerTimeDiff instance = new ServerTimeDiff();

    private final AtomicLong serverTimeDiff = new AtomicLong();
    private final AtomicLong lastSync = new AtomicLong();

    private ServerTimeDiff() {
    }

    public static ServerTimeDiff getInstance() {
        return instance;
    }

    public long getServerTimeDiff() {
        return serverTimeDiff.get();
    }

    public void setServerTimeDiff(long serverTimeDiff) {
        this.serverTimeDiff.set(serverTimeDiff);
    }

    public long getLastSync() {
        return lastSync.get();
    }

    public void setLastSync(long lastSync) {
        this.lastSync.set(lastSync);
    }
}