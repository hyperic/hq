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

//XXX s/com.sun/javax/g will not work with jdk 1.3 and jsse 1.0.3_01
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * The bogus trust manager allows for non-validated remote
 * SSL entities.  In the case of people using self-signed
 * certificates, this allows a connection, since it is unlikely
 * that they are using a CA, or that the CA will be in the client's 
 * cacerts of jssecerts files.
 */
public class BogusTrustManager 
    implements X509TrustManager 
{
    public void checkClientTrusted(X509Certificate[] chain, 
                                   String authType)
    {
    }
    
    public void checkServerTrusted(X509Certificate[] chain, 
                                   String authType)
    {
    }

    //required for jdk 1.3/jsse 1.0.3_01
    public boolean isClientTrusted(X509Certificate[] chain)
    {
        return true;
    }

    //required for jdk 1.3/jsse 1.0.3_01
    public boolean isServerTrusted(X509Certificate[] chain)
    {
        return true;
    }
    
    public X509Certificate[] getAcceptedIssuers(){
        return null;
    }
}
