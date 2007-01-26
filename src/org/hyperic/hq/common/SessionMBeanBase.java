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
package org.hyperic.hq.common;

import java.util.Date;

import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;

/**
 * Since mbeans don't seem to be easily interceptable in JBoss, we can give 
 * them sessions by having them subclass this.
 */
public abstract class SessionMBeanBase { 
    public void hit(final Date d) {
        try {
            SessionManager.runInSession(new SessionRunner() {
                public String getName() {
                    return "SessionMBeanBase";
                }

                public void run() throws Exception {
                    hitInSession(d);
                }
            });
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    protected abstract void hitInSession(Date d);
}
