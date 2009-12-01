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

package org.hyperic.hq.plugin.websphere;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class ApplicationCollector extends WebsphereCollector {

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        this.name =
            newObjectNamePattern("j2eeType=J2EEApplication," +
                                 "name=" + getModuleName() + "," +
                                 getProcessAttributes());

        try {
            this.name = resolve(mServer, this.name);
        } catch (PluginException e) {
            //XXX 6.0 hack
            if (getModuleName().equals("adminconsole")) {
                this.name = null;
            }
            else {
                throw e;
            }
        }
    }

    public void collect() {
        if (this.name == null) {
            setAvailability(true);
            return;
        }
        Object state =
            getAttribute(getMBeanServer(), this.name, "state");

        if (state == null) {
            setAvailability(false);
        }
        else {
            setAvailability(true);
        }
    }
}
