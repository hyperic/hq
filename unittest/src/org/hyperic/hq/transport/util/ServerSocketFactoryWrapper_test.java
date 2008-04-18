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
import java.net.ServerSocket;
import java.net.SocketException;

import javax.net.ServerSocketFactory;

import junit.framework.TestCase;

import org.jboss.remoting.transport.PortUtil;

/**
 * Tests the ServerSocketFactoryWrapper class.
 */
public class ServerSocketFactoryWrapper_test extends TestCase {
    
    private static final String LOCALHOST = "localhost";
    
    private static final String WILDCARD_ADDR = "0.0.0.0";

    /**
     * Creates an instance.
     *
     * @param name
     */
    public ServerSocketFactoryWrapper_test(String name) {
        super(name);
    }
    
    /**
     * Expect a NullPointerException.
     */
    public void testNullWrappedServerSocketFactory() throws Exception {
        try {
            new ServerSocketFactoryWrapper(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected NullPointerException instead of: "+e);
        }
    }
    
    public void testCreateUnboundServerSocket() throws Exception {
        ServerSocketFactory wrapped = ServerSocketFactory.getDefault();
        
        ServerSocket serverSocket = null;
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(false);
            
            serverSocket = wrapper.createServerSocket();
            
            assertFalse(serverSocket.isBound());
            
            assertReuseAddr(serverSocket, false);
        } finally {
            close(serverSocket);
        }
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(true);
            
            serverSocket = wrapper.createServerSocket();
            
            assertFalse(serverSocket.isBound());
            
            assertReuseAddr(serverSocket, true);
        } finally {
            close(serverSocket);
        }        
    }
    
    public void testCreateServerSocketBoundToPortWildCardAddress() throws Exception {
        ServerSocketFactory wrapped = ServerSocketFactory.getDefault();
        
        ServerSocket serverSocket = null;
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(false);
            
            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(InetAddress.getByName(WILDCARD_ADDR), serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, false);
        } finally {
            close(serverSocket);
        }
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(true);

            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(InetAddress.getByName(WILDCARD_ADDR), serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, true);
        } finally {
            close(serverSocket);
        }        
    }
    
    public void testCreateServerSocketBoundToPortWildCardAddressWithBackLog() throws Exception {
        ServerSocketFactory wrapped = ServerSocketFactory.getDefault();
        
        ServerSocket serverSocket = null;
        int backlog = 10;
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(false);
            
            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port, backlog);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(InetAddress.getByName(WILDCARD_ADDR), serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, false);
        } finally {
            close(serverSocket);
        }
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(true);

            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port, backlog);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(InetAddress.getByName(WILDCARD_ADDR), serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, true);
        } finally {
            close(serverSocket);
        }        
    }
    
    public void testCreateServerSocketBoundToPortSpecificInterface() throws Exception {
        ServerSocketFactory wrapped = ServerSocketFactory.getDefault();
        
        ServerSocket serverSocket = null;
        int backlog = 10;
        InetAddress localhost = InetAddress.getLocalHost();
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(false);
            
            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port, backlog, localhost);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(localhost, serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, false);
        } finally {
            close(serverSocket);
        }
        
        try {
            ServerSocketFactoryWrapper wrapper = new ServerSocketFactoryWrapper(wrapped);
            wrapper.setReuseAddress(true);

            int port = PortUtil.findFreePort(LOCALHOST);
            
            serverSocket = wrapper.createServerSocket(port, backlog, localhost);
            
            assertTrue(serverSocket.isBound());
            assertEquals(port, serverSocket.getLocalPort());
            assertEquals(localhost, serverSocket.getInetAddress());
            
            assertReuseAddr(serverSocket, true);
        } finally {
            close(serverSocket);
        }        
    }

    private void assertReuseAddr(ServerSocket serverSocket, boolean assertTrue) 
        throws SocketException {
        
        assertEquals("Expected different SO_REUSEADDR", 
                     assertTrue, 
                     serverSocket.getReuseAddress());
    }
    
    
    private void close(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

}
