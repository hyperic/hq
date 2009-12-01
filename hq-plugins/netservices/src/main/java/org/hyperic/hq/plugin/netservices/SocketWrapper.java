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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SocketWrapper {
    private static Log log =
        LogFactory.getLog(SocketWrapper.class.getName());

    boolean isDebug = log.isDebugEnabled();

    public static final String CRLF = "\r\n";

    Socket socket;
    BufferedReader reader = null;
    BufferedWriter writer = null;
    
    public SocketWrapper(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void close() {
        close(this.socket);
    }

    public static void close(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
        }        
    }

    public BufferedReader getReader() throws IOException {
        if (this.reader == null) {
            this.reader = getReader(this.socket);
        }
        return this.reader;
    }
    
    public BufferedWriter getWriter() throws IOException {
        if (this.writer == null) {
            this.writer = getWriter(this.socket);
        }
        return this.writer;
    }

    public String readLine() throws IOException {
        String line = getReader().readLine();
        if (isDebug) {
            log.debug("<<< " + line);
        }
        if (line == null) {
            throw new IOException("readLine failed");
        }
        return line;
    }

    public void writeLine(String line) throws IOException {
        getWriter();
        if (isDebug) {
            log.debug(">>> " + line);
        }
        this.writer.write(line);
        this.writer.write(CRLF);
        this.writer.flush();
    }

    public static BufferedReader getReader(Socket socket)
        throws IOException {

        InputStreamReader is =
            new InputStreamReader(socket.getInputStream());
        return new BufferedReader(is);
    }

    public static BufferedWriter getWriter(Socket socket)
        throws IOException {
    
        OutputStreamWriter os =
            new OutputStreamWriter(socket.getOutputStream());
        return new BufferedWriter(os);
    }
}
