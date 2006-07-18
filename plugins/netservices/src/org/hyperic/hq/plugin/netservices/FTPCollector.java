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

public class FTPCollector extends SocketChecker {

    private static final String ANONYMOUS_USER = "anonymous";
    private static final String ANONYMOUS_PASS = "hyperic-monitor@";

    private boolean isOK(String line) {
        return 
            line.startsWith("1") || // 100 response
            line.startsWith("2") || // 200 response
            line.startsWith("3");   // 300 response
    }

    private boolean isERR(String line) {
        return 
            line.startsWith("4") || // 400 response
            line.startsWith("5");   // 500 response
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
            boolean extendedResponse = false;
            while ((line = socket.readLine()) != null) {
                if (line.length() > 3 && line.charAt(3) == '-') {
                    extendedResponse = true;
                    continue;
                } else if (extendedResponse &&
                           line.length() > 1 &&
                           !Character.isDigit(line.charAt(0))) {
                    continue;
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            setErrorMessage("Reading " + cmd + " response", e);
            return false;
        }

        if (isOK(line)) {
            return true;
        }
        else if (isERR(line)){
            setErrorMessage(line);
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
            while ((line = socket.readLine()) != null) {
                setDebugMessage(line);
                // Loop through extended banners
                if (line.length() > 3 && line.charAt(3) == '-')
                    continue;
                else
                    break;
            }

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
        } else {
            // Use anonymous auth if no user/pass is given
            if (!sendCommand(socket, "USER", ANONYMOUS_USER)) {
                return false;
            }
            if (!sendCommand(socket, "PASS", ANONYMOUS_PASS)) {
                return false;
            }
        }            
        
        if (!sendCommand(socket, "QUIT")) {
            return false;
        }
        
        return true;
    }
}
