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

package org.hyperic.util.ntp;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NtpClient
{
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final int DEFAULT_PORT = 123;

    private static Log log =
        LogFactory.getLog(NtpClient.class.getName());

    private String hostname;
    private int port;
    private int timeout;

    public NtpClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.timeout = DEFAULT_TIMEOUT;
    }

    public NtpClient(String hostname) {
        this(hostname, DEFAULT_PORT);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Return the number of seconds since 1900
     */
    protected static double now() {
        return (double)(System.currentTimeMillis()/1000.0) + 2208988800.0;
    }

    public NtpResponse getResponse() 
        throws SocketException, UnknownHostException, IOException
    {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(this.timeout);

        InetAddress address = InetAddress.getByName(this.hostname);
        byte[] data = NtpResponse.getRequestBytes();
        DatagramPacket packet =
            new DatagramPacket(data, data.length, address, this.port);
        socket.send(packet);

        packet = new DatagramPacket(data, data.length);
        socket.receive(packet);
        
        NtpResponse response = NtpResponse.decodeResponse(now(), 
                                                          packet.getData());
        return response;
    }
}
