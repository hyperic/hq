/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;

public class RegisteredTriggerStartupListener implements StartupListener {
    
    private static final Log _log =
        LogFactory.getLog(RegisteredTriggerStartupListener.class);
    private static Thread initTriggerRunner;

    public void hqStarted() {
        final SessionRunner runner = new SessionRunner() {
            public String getName() {
                return "TriggersInitializationStartup";
            }
            public void run() throws Exception {
                RegisteredTriggers.getAndInitialize();
            }
        };
        initTriggerRunner = new Thread("Trigger Init") {
            public void run() {
                try {
                    final long start = System.currentTimeMillis();
                    _log.info("Starting Trigger Initialization");
                    SessionManager.runInSession(runner);
                    final long end = System.currentTimeMillis();
                    _log.info("Finished Trigger Initialization, took " +
                              (end-start)/1000 + " seconds");
                } catch (Exception e) {
                    _log.error(e, e);
                }
            }
        };
        initTriggerRunner.start();
    }
    
    public static Thread getRunnerThread() {
        return initTriggerRunner;
    }

}
