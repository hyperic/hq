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

package org.hyperic.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InetPortPinger {
    private String  host;
    private int     port;
    private int     timeout;
    
    /**
     * Create a new InetPortPinger object.
     * 
     * @param host    Hostname to ping
     * @param port    Port # to ping
     * @param timeout Timeout (in ms) to wait for data, before aborting
     */
    public InetPortPinger(String host, int port, int timeout) {
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;
    }

    /**
     * Pings the remote host.  A connection is made to the TCP port
     * as specified in the constructor.  
     *
     * @return true if connection was successful, false otherwise.
     */
    public boolean check() {
        try {
            Socket s = new Socket();
            s.bind(new InetSocketAddress(0));
            s.connect(new InetSocketAddress(host, port), timeout);
            s.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public static void main(String[] args) 
        throws Exception 
    {
        InetPortPinger p =
            new InetPortPinger(args[0],
                               Integer.parseInt(args[1]),
                               Integer.parseInt(args[2]));
    
        System.out.println(p.check());
    }
}
