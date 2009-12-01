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

package org.hyperic.hq.auth.shared;

import java.io.Serializable;

/**
 * The CertificateDTO class is a simple class for passing around Certificate
 * information from the server to the client.  For now this only contains the
 * PEM encoded certificate and key, but more entries can be added at a later
 * date
 */
public class CertificateDTO implements Serializable {

    /**
     * The PEM encoded certificate
     */
    private String certificate = null;

    /**
     * The PEM encoded key
     */
    private String privateKey  = null;


    /**
     * General constructor used for initializing the CertificateDTO object
     *
     * @param cert The PEM encoded certificate
     * @param key The PEM encoded key
     */
    public CertificateDTO(String cert, String key)
    {
        certificate = cert;
        privateKey = key;
    }

    /**
     * @return The PEM encoded certificate
     */
    public String getCertificate() {
        
        return certificate;
    }

    /**
     * @return The PEM encoded private key
     */
    public String getPrivateKey() {
        
        return privateKey;
    }
}
