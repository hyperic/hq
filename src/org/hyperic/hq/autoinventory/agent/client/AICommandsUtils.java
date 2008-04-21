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

package org.hyperic.hq.autoinventory.agent.client;

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 * Utility class for AI Commands.
 */
public class AICommandsUtils {

    private AICommandsUtils() {
    }
    
    public static AgentRemoteValue createArgForRuntimeDiscoveryConfig(
            int type, int id, String typeName, String name, ConfigResponse response) {
        
        AgentRemoteValue arv = new AgentRemoteValue();
        arv.setValue(ConfigStorage.PROP_TYPE, String.valueOf(type));
        arv.setValue(ConfigStorage.PROP_ID, String.valueOf(id));
        if (typeName != null) {
            arv.setValue(ConfigStorage.PROP_TYPE_NAME, typeName);
        }

        if ( response == null ) { 
            arv.setValue("disable.rtad", "true");
        } else {
            if (name != null) {
                response.setValue(ProductPlugin.PROP_RESOURCE_NAME, name);
            }
            ConfigStorage.copy(ConfigStorage.NO_PREFIX,
                               response,
                               ConfigStorage.CONFIG_PREFIX,
                               arv);
        }
        return arv;
    }

}
