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
package org.hyperic.hq.ha.agent.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;

public class HAServer implements AgentServerHandler, Runnable {

    private Log log = LogFactory.getLog(HAServer.class);
    private static final AtomicBoolean STOP = new AtomicBoolean(false);
    private static File flag;

    public String[] getCommandSet() {
        return new String[]{};
    }

    public AgentAPIInfo getAPIInfo() {
        return null;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
            InputStream inStream,
            OutputStream outStream)
            throws AgentRemoteException {
        return null;
    }

    public void startup(AgentDaemon agent) throws AgentStartException {
        log.info("startup()");
        String data = agent.getBootConfig().getBootProperties().getProperty(AgentConfig.PROP_DATADIR[0]);
        flag = new File(data, "stopHeartbeat");
        if (flag.exists()) {
            flag.delete();
        }
        try {
            log.info("flag = " + flag.getCanonicalPath());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        new Thread(this).start();
    }

    public void shutdown() {
        log.info("shutdown()");
        STOP.set(true);
    }

    public void run() {
        log.info("run()");
        while (!STOP.get()) {
            try {
                if (!flag.exists()) {
                    log.error("PING....");
                } else {
                    log.error("pong....");
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException ex) {
                    STOP.set(true);
                    log.error(ex.getMessage(), ex);
                }
            } catch (Throwable ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
