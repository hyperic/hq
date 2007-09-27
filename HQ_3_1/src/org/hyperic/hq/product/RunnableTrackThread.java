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

package org.hyperic.hq.product;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RunnableTrackThread implements Runnable {

    private static Log log =
        LogFactory.getLog(RunnableTrackThread.class.getName());

    private Thread thread = null;
    private static RunnableTrackThread instance = null;
    private boolean shouldDie = false;
    private long interval = TrackEventPluginManager.DEFAULT_INTERVAL;
    private Set watchers =
        Collections.synchronizedSet(new HashSet());

    static synchronized RunnableTrackThread getInstance() {
        if (instance == null) {
            instance = new RunnableTrackThread();
        }

        return instance;
    }

    public synchronized void doStart() {
        if (this.thread != null) {
            return;
        }

        this.thread = new Thread(this, "RunnableLogThread");
        this.thread.setDaemon(true);
        this.thread.start();

        log.info(this.thread.getName() + " started");
    }

    public synchronized void doStop() {
        if (this.thread == null) {
            return;
        }
        die();
        this.thread.interrupt();
        log.info(this.thread.getName() + " stopped");
        this.thread = null;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getInterval() {
        return this.interval;
    }

    public void add(Runnable watcher) {
        this.watchers.add(watcher);
    }

    public void remove(Runnable watcher) {
        this.watchers.remove(watcher);
    }

    public void run() {
        while (!shouldDie) {
            check();
            try {
                Thread.sleep(this.interval);
            } catch (InterruptedException e) {
            }
        }
    }

    public void die() {
        this.shouldDie = true;
    }

    public void check() {
        synchronized (this.watchers) {
            for (Iterator it = this.watchers.iterator();
                 it.hasNext();)
            {
                Runnable watcher = (Runnable)it.next();
                try {
                    watcher.run();
                } catch (Exception e) {
                    log.error("Unexpected exception: " + e, e);
                }
            }
        }
    }
}
