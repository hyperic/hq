/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.transport.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

/**
 * Wraps a server socket factory, allowing different socket options to be 
 * set before a bound server socket is returned.
 */
public class ServerSocketFactoryWrapper extends ServerSocketFactory {

    private final ServerSocketFactory _wrapped;
    
    private boolean _reuseAddress;
    
    /**
     * Creates an instance wrapping a server socket factory.
     *
     * @param wrapped The wrapped server socket factory.
     * @throws NullPointerException if the wrapped server socket factory is <code>null</code>.
     */
    public ServerSocketFactoryWrapper(ServerSocketFactory wrapped) {
        if (wrapped == null) {
            throw new NullPointerException("server socket factory is null");
        }
        
        _wrapped = wrapped;
    }
    
    /**
     * Set all server sockets created by the factory to have their 
     * <code>SO_REUSEADDR</code> socket option set explicitly.
     * 
     * @param reuseAddress <code>true</code> to reuse the address; 
     *                     <code>false</code> otherwise.
     */
    public void setReuseAddress(boolean reuseAddress) {
        _reuseAddress = reuseAddress;
    }
    
    /**
     * @see javax.net.ServerSocketFactory#createServerSocket()
     */
    public ServerSocket createServerSocket() throws IOException {
        return createUnboundServerSocket(_reuseAddress);
    }

    /**
     * @see javax.net.ServerSocketFactory#createServerSocket(int)
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket serverSocket = createUnboundServerSocket(_reuseAddress);
        serverSocket.bind(new InetSocketAddress(port));
        return serverSocket;
    }

    /**
     * @see javax.net.ServerSocketFactory#createServerSocket(int, int)
     */
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        ServerSocket serverSocket = createUnboundServerSocket(_reuseAddress);
        serverSocket.bind(new InetSocketAddress(port), backlog);
        return serverSocket;
    }

    /**
     * @see javax.net.ServerSocketFactory#createServerSocket(int, int, java.net.InetAddress)
     */
    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
            throws IOException {
        ServerSocket serverSocket = createUnboundServerSocket(_reuseAddress);
        serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
        return serverSocket;
    }
    
    private ServerSocket createUnboundServerSocket(boolean reuseAddr) throws IOException {
        ServerSocket serverSocket = _wrapped.createServerSocket();
        
        serverSocket.setReuseAddress(reuseAddr);
        
        return serverSocket;
    }

}
