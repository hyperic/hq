/*
 * 'SNMPTrapReceiverPlugin.java'
 *
 *
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007, 2008, 2009], Hyperic, Inc.
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


package org.hyperic.hq.plugin.netdevice;

import java.io.IOException;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.util.config.ConfigResponse;

public class SNMPTrapReceiverPlugin
    extends LogTrackPlugin
{
    private SNMPTrapReceiver getReceiver() throws IOException {
        SNMPTrapReceiver receiver = SNMPTrapReceiver.getInstance(getManager().getProperties());

        receiver.setPluginManager(getManager());

        return receiver;
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);

        if (SNMPTrapReceiver.hasInstance()) {
            return;
        }

        String listen = manager.getProperty(SNMPTrapReceiver.PROP_LISTEN_ADDRESS);

        if (listen == null) {
            return;
        }

        getLog().debug("Configuring default listener: " + listen);

        try {
            getReceiver();
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        try {
            getReceiver().add(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown() throws PluginException {
        super.shutdown();

        SNMPTrapReceiver.remove(this);
    }
}
