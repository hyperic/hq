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

import java.io.IOException;

public class POP3Collector extends SocketChecker {

    private static final String OK  = "+OK";
    private static final String ERR = "-ERR";

    private boolean isOK(String line) {
        return line.startsWith(OK);
    }

    private boolean isERR(String line) {
        return line.startsWith(ERR);
    }

    private String getERR(String line) {
        return line.substring(ERR.length());
    }

    private boolean sendCommand(SocketWrapper socket, String cmd)
        throws IOException {

        return sendCommand(socket, cmd, null);
    }
    
    private boolean sendCommand(SocketWrapper socket,
                                String cmd, String val)
        throws IOException {

        if (val != null) {
            cmd += " " + val;
        }
        socket.writeLine(cmd);

        String line;
        try {
            line = socket.readLine();
        } catch (IOException e) {
            setErrorMessage("Reading " + cmd + " response", e);
            return false;
        }

        if (isOK(line)) {
            return true;
        }
        else if (isERR(line)){
            setErrorMessage(getERR(line));
            return false;
        }
        else {
            setErrorMessage("Unexpected " + cmd + " response: " + line);
            return false;
        }
    }
    
    protected boolean check(SocketWrapper socket) throws IOException {
        String line;
        try {
            line = socket.readLine();
            setDebugMessage(line);
        } catch (IOException e) {
            setErrorMessage("Failed to read welcome banner", e);
            throw e;
        }

        if (!isOK(line)) {
            setErrorMessage("Unexpected welcome response: " + line);
            return false;
        }

        if (hasCredentials()) {
            if (!sendCommand(socket, "USER", getUsername())) {
                return false;
            }
            if (!sendCommand(socket, "PASS", getPassword())) {
                return false;
            }

            //XXX should only do this if the metric is enabled
            if (!sendCommand(socket, "LIST")) {
                return false;
            }

            String msgs = null;
            while ((line = socket.readLine()) != null) {
                if (line.startsWith(".")) {
                    break;
                }
                msgs = line;
            }

            int num = 0; 
            if (msgs != null) {
                int ix = msgs.indexOf(' ');
                if (ix != -1) {
                    msgs = msgs.substring(0, ix);
                    num = Integer.parseInt(msgs);
                }
            }
            setValue("NumberOfMessages", num);
        }
        
        if (!sendCommand(socket, "QUIT")) {
            return false;
        }
        
        return true;
    }
}
