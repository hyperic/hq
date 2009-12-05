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

package org.hyperic.hq.plugin.netservices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.util.Random;
import java.util.StringTokenizer;

//not a full-blown client, just enough to do dhcp discover
//http://www.faqs.org/rfcs/rfc2131.html

public class DHCPClient {

    private static final Random random = new Random();
    private static final int MESSAGE_TYPE = 53;
    private int port;
    private InetAddress address;
    private DatagramSocket socket;

    public DHCPClient(InetAddress address, int port)
        throws SocketException {
        this.socket = new DatagramSocket(68); //default client port
        this.address = address;
        this.port = port;
    }

    public void setTimeout(int millis) {
        try {
            this.socket.setSoTimeout(millis);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        this.socket.close();
    }

    public void send(Packet packet)
        throws IOException {

        byte[] data = packet.encode();

        DatagramPacket request = 
            new DatagramPacket(data,
                               data.length,
                               this.address,
                               this.port);

        this.socket.send(request);
    }

    public Packet receive()
        throws IOException {

        byte[] data = new byte[1024];
        DatagramPacket response = 
            new DatagramPacket(data, data.length);

        this.socket.receive(response);

        Packet packet = new Packet();
        packet.decode(response.getData());
        return packet;
    }

    private static String addrToText(byte src[]) {
        return
            (src[0] & 0xff) + "." +
            (src[1] & 0xff) + "." +
            (src[2] & 0xff) + "." +
            (src[3] & 0xff);
    }

    public static byte[] decodeHwaddr(String hwaddr) {
        StringTokenizer token =
            new StringTokenizer(hwaddr, ":");
        byte[] data = new byte[Packet.CHADDR_LEN];

        for (int i=0; i<Packet.HWADDR_LEN; i++) {
            data[i] =
                (byte)Integer.parseInt(token.nextToken(), 16);
        }

        return data;
    }
    
    public class Packet {
        private static final int ADDR_LEN    = 4;
        private static final int CHADDR_LEN  = 16;
        private static final int SNAME_LEN   = 64;
        private static final int FILE_LEN    = 128;
        private static final int OPTIONS_LEN = 312;
        private static final int HWADDR_LEN  = 6;
        
        public byte   op      = (byte)1; //BOOTREQUEST
        public byte   htype   = (byte)1; //ETHERNET
        public byte   hlen    = (byte)HWADDR_LEN;
        public byte   hops    = (byte)0;
        public int    xid;
        public short  secs    = (short)0;
        public short  flags   = (short)0;
        public byte[] ciaddr  = new byte[ADDR_LEN];
        public byte[] yiaddr  = new byte[ADDR_LEN];
        public byte[] siaddr  = new byte[ADDR_LEN];
        public byte[] giaddr  = new byte[ADDR_LEN];
        public byte[] chaddr  = new byte[CHADDR_LEN];
        public byte[] sname   = new byte[SNAME_LEN];
        public byte[] file    = new byte[FILE_LEN];
        public byte[] options = new byte[OPTIONS_LEN];

        public String getYaddress() {
            return addrToText(this.yiaddr);
        }
        
        public String getSaddress() {
            return addrToText(this.siaddr);
        }
        
        public byte[] encode()
            throws IOException {

            ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();
            DataOutputStream stream =
                new DataOutputStream(bytes);

            stream.writeByte(this.op);
            stream.writeByte(this.htype);
            stream.writeByte(this.hlen);
            stream.writeByte(this.hops);
            stream.writeInt(this.xid);
            stream.writeShort(this.secs);
            stream.writeShort(this.flags);
            stream.write(this.ciaddr, 0, ADDR_LEN);
            stream.write(this.yiaddr, 0, ADDR_LEN);
            stream.write(this.siaddr, 0, ADDR_LEN);
            stream.write(this.giaddr, 0, ADDR_LEN);
            stream.write(this.chaddr, 0, CHADDR_LEN);
            stream.write(this.sname, 0, SNAME_LEN);
            stream.write(this.file, 0, FILE_LEN);
            stream.write(this.options, 0, OPTIONS_LEN);

            return bytes.toByteArray();
        }

        public void decode(byte[] data)
            throws IOException {

            ByteArrayInputStream bytes =
                new ByteArrayInputStream(data, 0, data.length);
            DataInputStream stream = new DataInputStream(bytes);

            this.op = stream.readByte();
            this.htype = stream.readByte();
            this.hlen = stream.readByte();
            this.hops = stream.readByte();
            this.xid = stream.readInt();
            this.secs = stream.readShort();
            this.flags = stream.readShort();
            stream.readFully(this.ciaddr, 0, ADDR_LEN);
            stream.readFully(this.yiaddr, 0, ADDR_LEN);
            stream.readFully(this.siaddr, 0, ADDR_LEN);
            stream.readFully(this.giaddr, 0, ADDR_LEN);
            stream.readFully(this.chaddr, 0, CHADDR_LEN);
            stream.readFully(this.sname, 0, SNAME_LEN);
            stream.readFully(this.file, 0, FILE_LEN);
            stream.readFully(this.options, 0, OPTIONS_LEN);
        }
    }

    public Packet getDiscoverPacket(byte[] hwaddr) {
        Packet packet = new Packet();
        packet.xid = random.nextInt();            
        packet.chaddr = hwaddr;

        byte[] options = packet.options;
        int ix = 0;
        //vendor cookie as per rfc
        options[ix++] = (byte)99;
        options[ix++] = (byte)130;
        options[ix++] = (byte)83;
        options[ix++] = (byte)99;

        options[ix++] = (byte)MESSAGE_TYPE;
        options[ix++] = (byte)1; //message option length
        options[ix++] = (byte)1; //DHCPDISCOVER

        options[ix++] = (byte)255; //terminator

        return packet;
    }
}
