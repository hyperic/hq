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

package org.hyperic.util.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SSHBase {

    private static final int DEFAULT_PORT = 22;
    private static final int DEFAULT_TIMEOUT = 30000;

    protected String user;
    protected String host;
    protected String password;

    protected int port = DEFAULT_PORT;
    protected int timeout = DEFAULT_TIMEOUT;

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public SSHBase(String user, String password, 
                   String host) {

        this.user = user;
        this.password = password;
        this.host = host;
    }

    public Session openSession() 
        throws JSchException
    {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        
        UserInfo ui = new SSHUserInfo(password);
        session.setUserInfo(ui);
        session.setTimeout(timeout);
        session.connect(timeout);
        return session;
    }
}
