/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.zimbra.five;

/**
 *
 * @author laullon
 */
public class Service {

    private String name, pidFile, log, process;

    public Service(String name, String pidFile, String log, String process) {
        this.name = name;
        this.pidFile = pidFile;
        this.log = log;
        this.process = process;
    }

    public Service(String name, String pidFile, String log) {
        this.name = name;
        this.pidFile = pidFile;
        this.log = log;
    }

    public Service(String name, String pidFile) {
        this.name = name;
        this.pidFile = pidFile;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the pidFile
     */
    public String getPidFile() {
        return pidFile;
    }

    /**
     * @return the log
     */
    public String getLog() {
        return log;
    }

    /**
     * @return the process
     */
    public String getProcess() {
        return process;
    }
}
