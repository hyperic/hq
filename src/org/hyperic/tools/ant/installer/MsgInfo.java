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

package org.hyperic.tools.ant.installer;

public class MsgInfo extends MsgBase {

    // This is used to preserve whitespace that would
    // otherwise be trimmed
    public static final String LITERAL = "__ll__";

    public String getPrefix () { return "INFO"; }

    public void beginMessage ( String message ) {
        message = processLiterals(message);
        logger.printMessage(message);
    }

    public void continueMessage ( String message ) {
        message = processLiterals(message);
        logger.printMessage(message);
    }

    private String processLiterals (String message) {
        if (message.startsWith(LITERAL)) {
            message = message.substring(LITERAL.length());
        }
        if (message.endsWith(LITERAL)) {
            message
                = message.substring(0, message.length() - LITERAL.length());
        }
        return message;
    }

    public void endMessage () {}
}
