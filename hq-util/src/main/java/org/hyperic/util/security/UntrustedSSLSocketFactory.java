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

package org.hyperic.util.security;

import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class UntrustedSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;

    public UntrustedSSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance( "TLS");
            sslcontext.init(null,
                            new X509TrustManager[] { new BogusTrustManager() },
                            null);
            factory = (SSLSocketFactory) sslcontext.getSocketFactory();

        } catch(NoSuchAlgorithmException exc){
            throw new IllegalStateException("Unable to get SSL context: "+
                                            exc.getMessage());
        } catch(KeyManagementException exc){
            throw new IllegalStateException("Unable to initialize ctx " +
                                            "with BogusTrustManager: " +
                                            exc.getMessage());
        }
    }

    public static SocketFactory getDefault()
    {
        return new UntrustedSSLSocketFactory();
    }

    public Socket createSocket(Socket socket, String s, int i, boolean flag)
        throws IOException
    {
        return factory.createSocket(socket, s, i, flag);
    }

    public Socket createSocket(InetAddress inaddr, int i,
                               InetAddress inaddr1, int j)
        throws IOException
    {
        return factory.createSocket( inaddr, i, inaddr1, j);
    }

    public Socket createSocket(InetAddress inaddr, int i) throws 
        IOException
    {
        return factory.createSocket(inaddr, i);
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j)
        throws IOException
    {
        return factory.createSocket( s, i, inaddr, j);
    }

    public Socket createSocket(String s, int i)
        throws IOException
    {
        return factory.createSocket( s, i);
    }

    public String[] getDefaultCipherSuites()
    {
        return factory.getSupportedCipherSuites();
    }

    public String[] getSupportedCipherSuites()
    {
        return factory.getSupportedCipherSuites();
    }
}
