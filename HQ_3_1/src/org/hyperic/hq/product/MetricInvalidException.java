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

package org.hyperic.hq.product;

import org.hyperic.hq.common.SystemException;

/**
 * This exception is thrown from the translate & getValue 
 * methods in plugins when the plugin cannot map the DSN
 * to a template which it returned from getMeasurements
 */
public class MetricInvalidException extends SystemException {
    private String remoteMessage;
    
    public MetricInvalidException () {
        super();
    }

    public MetricInvalidException (String s) {
        super(s);
    }

    public MetricInvalidException (Throwable t) {
        super(t);
    }

    public MetricInvalidException (String s, Throwable t) {
        super(s, t);
    }

    public String getRemoteMessage() {
        return remoteMessage;
    }

    public void setRemoteMessage(String remoteMessage) {
        this.remoteMessage = remoteMessage;
    }
}
