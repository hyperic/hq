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

package org.hyperic.hq.measurement.monitor;

import java.util.Vector;

import org.hyperic.util.NestedException;

/**
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class MonitorAgentException extends NestedException {

    /**
     * Constructor for MonitorAgentException.
     */
    public MonitorAgentException() {
        super();
    }

    /**
     * Constructor for MonitorAgentException.
     * @param arg0
     */
    public MonitorAgentException(String s) {
        super(s);
    }

    /**
     * Constructor for MonitorAgentException.
     * @param arg0
     */
    public MonitorAgentException(Throwable t) {
        super(t);
    }

    /**
     * Constructor for MonitorAgentException.
     * @param arg0
     * @param arg1
     */
    public MonitorAgentException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * Constructor for MonitorAgentException.
     * @param arg0
     * @param arg1
     */
    public MonitorAgentException(String s, Vector t) {
        super(s, t);
    }

}
