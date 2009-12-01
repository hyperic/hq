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

/**
 * Java representation of an NTP response as described in RFC 2030
 *
 * http://www.faqs.org/rfcs/rfc2030.html
 *
 * Packet format:
 *                          1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |LI | VN  |Mode |    Stratum    |     Poll      |   Precision   |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                          Root Delay                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                       Root Dispersion                         |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                     Reference Identifier                      |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     |                   Reference Timestamp (64)                    |
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     |                   Originate Timestamp (64)                    |
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     |                    Receive Timestamp (64)                     |
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     |                    Transmit Timestamp (64)                    |
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                 Key Identifier (optional) (32)                |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     |                                                               |
 *     |                 Message Digest (optional) (128)               |
 *     |                                                               |
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class NtpResponse
{
    protected byte leapIndicator; 
    protected byte version; 
    protected byte mode; 
    protected short stratum; 
    protected byte interval; 
    protected byte precision; 
    protected double rootDelay; 
    protected double rootDispersion; 
    protected byte[] referenceIdentifier = new byte[4];
    protected double referenceTimestamp;
    protected double originateTimestamp;
    protected double receiveTimestamp;
    protected double transmitTimestamp;

    // These are not actually in the NTP response, but calculated
    // when the repsonse is returned
    protected double roundTripDelay;
    protected double localClockOffset;

    // Private constructor for decodeResponse
    private NtpResponse() {}

    /**
     * Generate the NTP request
     */
    public static byte[] getRequestBytes() {
        // Initialize packet with all fields set to 0
        byte[] packet = new byte[48];
        
        packet[0] = 27; // version = 3, mode = 3
        // Set transmit time
        encode(packet, 40, NtpClient.now());

        return packet;
    }

    /**
     * Decode the NTP server response
     */
    public static NtpResponse decodeResponse(double ts, byte[] data) {
        NtpResponse response = new NtpResponse();

        response.leapIndicator = (byte)((data[0] >> 6) & 0x3);
        response.version = (byte)((data[0] >> 3) & 0x7);
        response.mode = (byte)(data[0] & 0x7);
        response.stratum = unsignedByteToShort(data[1]);
        response.interval = data[2];
        response.precision = data[3];
        
        response.rootDelay = 
            (data[4] * 256.0) + 
            unsignedByteToShort(data[5]) +
            (unsignedByteToShort(data[6]) / 256.0) +
            (unsignedByteToShort(data[7]) / 65536.0);
        
        response.rootDispersion = 
            (unsignedByteToShort(data[8]) * 256.0) + 
            unsignedByteToShort(data[9]) +
            (unsignedByteToShort(data[10]) / 256.0) +
            (unsignedByteToShort(data[11]) / 65536.0);
        
        response.referenceIdentifier[0] = data[12];
        response.referenceIdentifier[1] = data[13];
        response.referenceIdentifier[2] = data[14];
        response.referenceIdentifier[3] = data[15];
        
        response.referenceTimestamp = decode(data, 16);
        response.originateTimestamp = decode(data, 24);
        response.receiveTimestamp = decode(data, 32);
        response.transmitTimestamp = decode(data, 40);

        response.roundTripDelay =
            (ts - response.originateTimestamp) -
            (response.transmitTimestamp - response.receiveTimestamp);
            
        response.localClockOffset =
            ((response.receiveTimestamp - response.originateTimestamp) +
             (response.transmitTimestamp - ts)) / 2;

        return response;
    }
        
    // Java's byte type is signed, while NTP uses unsigned
    private static short unsignedByteToShort(byte b) {
        if ((b & 0x80) == 0x80)
            return (short) (128 + (b & 0x7f));
        else return (short) b;
    }

    // Decode byte[8] -> double
    private static double decode(byte[] array, int start) {
        double ts = 0;
        
        for (int i=0; i<8; i++) {
            ts += unsignedByteToShort(array[start+i]) * Math.pow(2, (3-i)*8);
        }
        
        return ts;
    }
    
    // Encode double -> byte[8]
    private static void encode(byte[] array, int start, double timestamp)
    {
        for (int i=0; i<8; i++) {
            double base = Math.pow(2, (3-i)*8);
            
            array[start+i] = (byte)(timestamp/base);
            timestamp = timestamp - 
                (double) (unsignedByteToShort(array[start+i]) * base);
        }
    }

    public double getLocalClockOffset() {
        return Math.abs(this.localClockOffset);
    }

    public double getRoundTripDelay() {
        return this.roundTripDelay;
    }

    public double getRootDelay() {
        return this.rootDelay;
    }

    public double getRootDispersion() {
        return this.rootDispersion;
    }
}   
