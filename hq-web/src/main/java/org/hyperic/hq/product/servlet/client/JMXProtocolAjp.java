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

package org.hyperic.hq.product.servlet.client;

import org.apache.ajp.Ajp13;
import org.apache.ajp.Ajp13Packet;
import org.apache.ajp.RequestHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Iterator;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Talk to eam jmx servlet over ajp.
 */
public class JMXProtocolAjp extends JMXProtocolRequest {
    private static Log log = LogFactory.getLog(JMXProtocolAjp.class);

    private static final int M_GET = 2;

    private HashMap sockets = new HashMap();

    private Socket getSocket(String host, int port)
        throws SocketException, UnknownHostException, IOException {

        String key = host + ":" + port;

        Socket s = (Socket)sockets.get(key);

        if ((s != null) && s.isClosed()) {
            s = null;
        }

        if (s == null) {
            log.debug("Connect " + host + " " + port );
            s = new Socket(host, port);
            sockets.put(key, s);
        }

        return s;
    }

    private void closeSocket(Socket s)
        throws SocketException, IOException {
        s.shutdownOutput();
        s.shutdownInput();
        s.close();
    }

    private boolean checkClosed(Socket s) throws IOException {
        int timeout = 0;

        try {
            InputStream is = s.getInputStream();
            timeout = s.getSoTimeout();

            s.setSoTimeout(3);

            is.mark(10);
            
            try {
                if (is.read() < 0) {
                    closeSocket(s);
                    return true;
                }
                else {
                    is.reset();
                }
            }
            catch (InterruptedIOException e) {
                return false;
            }
        }
        catch (SocketException e) {
            closeSocket(s);
            return true;
        }

        finally {
            try {
                if (!s.isClosed()) {
                    s.setSoTimeout(timeout);
                }
            }
            catch (SocketException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static InputStream get(Socket s,
                                     String path,
                                     String query)
        throws IOException, SocketException, ServerError {

        Ajp13Packet p = new Ajp13Packet(Ajp13.MAX_PACKET_SIZE);
        
        p.appendInt(Ajp13Packet.AJP13_WS_HEADER);
        p.appendInt(0);
        p.setByteOff(4);

        p.appendByte(RequestHandler.JK_AJP13_FORWARD_REQUEST);

        p.appendByte((byte)M_GET); //method
        p.appendString("http");    //protocol
        p.appendString(path);      //requestURI

        //XXX
        p.appendString("remote_addr"); //remoteAddr
        p.appendString("remote_host"); //remoteHost

        //XXX assuming these are used for rewriting redirects
        //ala ReverseProxy?
        p.appendString("server_name"); //serverName
        p.appendInt(80);               //serverPort

        p.appendBool(false); //isSSL
        p.appendInt(0);      //number of headers

        //attributes
        p.appendByte(RequestHandler.SC_A_QUERY_STRING);
        p.appendString(query);

        //end of attributes
        p.appendByte(RequestHandler.SC_A_ARE_DONE);

        int len = p.getByteOff() - 4;
        p.setByteOff(2);
        p.appendInt(len);

        OutputStream os = s.getOutputStream();
        os.write(p.getBuff(), 0, len + 4);

        InputStream is = s.getInputStream();

        String response = "", msg = "";
        int status = 0;

        boolean done = false;
        while (!done) {
            int b1, b2;

            //first 2 bytes should be AB
            b1 = is.read();
            b2 = is.read();

            if (!((b1 == 'A') && (b2 == 'B'))) {
                //XXX invalid header
                break;
            }
            
            //length
            b1 = is.read() & 0xFF;
            b2 = is.read() & 0xFF;
            
            int length = (b1 << 8) + b2;

            byte[] buf = new byte[length];
            int n = 0, offset = 0, total = 0;

            while ((n = is.read(buf, offset, length - offset)) != -1 &&
                   (length - offset != 0))
            {
                total += n;
                offset += n;
            }

            int code = (int)buf[0];

            switch (code) {
              case 3: //AJP13_SEND_BODY_CHUNK
                //length also skips trailing \0
                response += new String(buf, 3, total-4);
                break;
              case 4: //AJP13_SEND_HEADERS
                p = new Ajp13Packet(buf);
                p.getByte();
                if ((status = p.getInt()) != 200) {
                    msg = p.getString();
                }
                break;
              case 5: //AJP13_END_RESPONSE
                done = true;
                break;
              case 6: //AJP13_GET_BODY_CHUNK
                break;
              default:
                break;
            }
        }

        if ((status == 0) && (response.length() <= 0)) {
            //for whatever reason, first time here after endpoint
            //connection has been closed, i/o methods above do not
            //throw an exception.  wtf.
            throw new SocketException("no data received");
        }

        //could check that status == 200.  ers tomcat returns a 400
        //error, No Host matches server name server_name.

        return new java.io.ByteArrayInputStream(new byte[0]);
    }

    public void shutdown() {
        Iterator it = sockets.values().iterator();

        while (it.hasNext()) {
            try {
                closeSocket((Socket)it.next());
            } catch (Exception e) {
            }
        }

        sockets.clear();
    }

    public InputStream openStream(String host, int port, String user, 
                                     String pass,
                                     String path, String query)
        throws IOException,ServerError {

        Socket s = null;

        try {
            s = getSocket(host, port);

            return get(s, path, query);
        } catch (SocketException e) {
            if (s != null) {
                if (checkClosed(s)) {
                    //endpoint has been closed (e.g. tomcat restarted)
                    //attempt reconnect
                    s = getSocket(host, port);
                }
                return get(s, path, query);
            }
            throw e;
        }
    }
}
