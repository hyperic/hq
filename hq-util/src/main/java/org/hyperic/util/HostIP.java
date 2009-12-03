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

import java.util.StringTokenizer;

public class HostIP {

    // Constants used in the convertIP methods
    public static final int IP_HOST      = 0;
    public static final int IP_MULTICAST = 1;
    public static final int IP_ANY       = 2;

    /**
     * Convert a string based IP address into the byte representation.
     * Same as the other convertIP method, except that ipAddr is assumed
     * to be a regular (i.e. host) address
     */
    public static byte[] convertIP(String ipAddr){
        return convertIP(ipAddr, IP_HOST);
    }

    /**
     * Convert a string based IP address into the byte representation.
     *
     * @param ipAddr The address in dotted decimal notation
     * @param addressType Determines which addresses are considered legal.
     * Can be IP_HOST, IP_MULTICAST, or IP_ANY
     * @return a byte array of size 4, containing the encoded address
     *         or null indicating the ip address could not be converted
     */
    public static byte[] convertIP(String ipAddr,
                                   int addressType){
        StringTokenizer st;
        int[] octets;

        octets = new int[4];        
        st     = new StringTokenizer(ipAddr, ".", true);
        if(st.countTokens() != 7){ // elements + delimiters
            return null;
        }
        
        for(int i=0; i<4; i++){
            if(i > 0 && !st.nextToken().equals(".")){
                return null;
            }
            
            try {
                octets[i] = Integer.parseInt(st.nextToken());
            } catch(NumberFormatException exc){
                return null;
            }
        }

        boolean isOK = false;
        switch (addressType) {
        case IP_HOST:
            isOK = (octets[0] > 0
                    && octets[0] < 255 
                    && octets[1] >= 0
                    && octets[1] < 255
                    && octets[2] >= 0
                    && octets[2] < 255
                    && octets[3] > 0
                    && octets[3] < 255);
            break;

        case IP_MULTICAST:
            isOK = (octets[0] > 0
                    && octets[0] >= 224
                    && octets[0] <= 239
                    && octets[1] >= 0
                    && octets[1] <= 255
                    && octets[2] >= 0
                    && octets[2] <= 255
                    && octets[3] > 0
                    && octets[3] <= 255);
            break;
            
        case IP_ANY:
            isOK = (octets[0] >= 0
                    && octets[0] <= 255 
                    && octets[1] >= 0
                    && octets[1] <= 255
                    && octets[2] >= 0
                    && octets[2] <= 255
                    && octets[3] >= 0
                    && octets[3] <= 255);
        default:
            throw new IllegalArgumentException("Unrecognized address type: "
                                               + addressType);
        }

        if (!isOK) return null;

        byte[] res = new byte[4];
        
        res[0] = (byte)octets[0];
        res[1] = (byte)octets[1];
        res[2] = (byte)octets[2];
        res[3] = (byte)octets[3];
        return res;
    }

    /**
     * Validate a String that represents an IP address.
     * @param ip The IP address to validate.
     * @return true if the <code>ip</code> is a valid IP 
     * address, false otherwise.
     */
    public static boolean isValidIP(String ip) {
        return convertIP(ip) != null;
    }

    /**
     * @return true if the String represents a valid multicast IP address
     */
    public static boolean isValidMulticastIP(String ip) {
    
        return convertIP(ip, IP_MULTICAST) != null;
    }
}
