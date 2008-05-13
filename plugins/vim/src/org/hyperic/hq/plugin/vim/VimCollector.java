/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.vim;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

public abstract class VimCollector extends Collector {

    protected abstract void collect(VimServiceConnection conn)
        throws Exception;

    protected void init() throws PluginException {
        super.init();
        setSource(VimUtil.getURL(getProperties()));
    }

    public void collect() {
        VimServiceConnection conn = null;

        try {
            conn = VimUtil.getServiceConnection(getProperties());
            setAvailability(conn.isConnected());
            collect(conn);
        } catch (Exception e) {
            setAvailability(false);
            setErrorMessage(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) { }
            }
        }
    }
}
