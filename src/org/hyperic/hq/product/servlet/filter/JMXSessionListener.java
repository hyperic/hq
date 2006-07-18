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

package org.hyperic.hq.product.servlet.filter;

import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSession;

public final class JMXSessionListener 
    implements HttpSessionListener
{
    private int created = 0;
    private int destroyed = 0;
    private boolean fresh = true;
    
    public JMXSessionListener() {
    }
    
    private void init(HttpSession session) {
        JMXFilterInitServlet.registerSessionListener(this);
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getDestroyed() {
        return destroyed;
    }

    public void setDestroyed(int destroyed) {
        this.destroyed = destroyed;
    }


    public void sessionCreated(HttpSessionEvent event) {
        if(fresh) init(event.getSession());
        created++;
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        destroyed++;
    }
}
