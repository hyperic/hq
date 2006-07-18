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

package org.hyperic.hq.plugin.netservices;

import org.hyperic.util.ssh.SSHBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHCollector extends NetServicesCollector {

    private static Log log =
        LogFactory.getLog(SSHCollector.class.getName());

    public void collect() {
        String user = getUsername();
        String password = getPassword();
        String host = getHostAddress();
        int port = getPort();
        int timeout = getTimeoutMillis();

        if (user == null) {
            setErrorMessage("No username given, not performing check");
            setAvailability(false);
            return;
        }

        if (password == null) {
            setErrorMessage("No password given, not performing check");
            setAvailability(false);
            return;
        }

        SSHBase ssh = new SSHBase(user, password, host);
        ssh.setPort(port);
        ssh.setTimeout(timeout); //XXX: does not help with auth fail

        Session session = null;
        try {
            startTime();
            session = ssh.openSession();
            setAvailability(true);
            endTime();
        } catch (JSchException e) {
            log.error(e.getMessage(), e);
            setAvailability(false);
            endTime();
            setErrorMessage(e.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}

