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

package org.hyperic.hq.plugin.websphere.wscp;

import java.io.File;
import java.util.ArrayList;

/**
 * Wrapper for the WebSphere wscp program.
 */

/*
 * XXX there is a WscpCommand class in the WebSphere api.
 * but there are downsides:
 * - it embeds a Tcl interpreter in to the process == bloat
 * - it hangs forever and ever when doing start or stop
 * - there is only 1 document i could find on it and it sucks:
 * http://www-3.ibm.com/software/webservers/appserv/doc/v40/ae/infocenter/was/060600020207.html
 * it also requires a system property to be set with the installpath.
 * we don't do control actions often, so it seems best over all to do
 * this out-of-process.  the program works on unix and win32 platforms.
 */

public class WebsphereController {
    static final boolean WIN32 =
        System.getProperty("os.name").startsWith("Windows");

    static final String WSCP_CMD = (WIN32 ? "wscp.bat" : "wscp.sh");
    static final String WSCP = "bin" + File.separator + WSCP_CMD;

    private String wscp;

    private String host;
    private String port;

    private String installpath;

    public WebsphereController(String installpath,
                               String host,
                               String port) {
        this.installpath = installpath;

        this.wscp = this.installpath + File.separator + WSCP;

        //XXX currently unused.  only way to configure wscp to use
        //something other than the defaults is with a -p foo.properties
        //or ~/.wscprc that contains wscp.hostPort=8880
        this.host = host;
        this.port = port;
    }

    public String getWscp() {
        return this.wscp;
    }

    public void setWscp(String value) {
        this.wscp = value;
    }

    public String commandToString(WebsphereCommand cmd, String action) {
        StringBuffer sb = new StringBuffer(cmd.getObjectName());
        sb.append(" ");
        sb.append(action);
        sb.append(" ");
        sb.append(cmd.toString());
        return sb.toString();
    }

    public ArrayList getArgs(WebsphereCommand cmd, String action) {
        ArrayList args = new ArrayList();
        args.add("-c");
        args.add(commandToString(cmd, action));
        return args;
    }
}
