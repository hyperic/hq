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
package org.hyperic.hq.plugin.postgresql;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.util.config.ConfigResponse;

public class ServerControl extends ServerControlPlugin {

    private static Log log = LogFactory.getLog(ServerControl.class);

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        log.info("[configure] config=" + config);
        final String program = config.getValue(PostgreSQL.PROP_PROGRAM);
        File programFile = new File(program);
        if (!programFile.isAbsolute()) {
            programFile = new File(config.getValue(ProductPlugin.PROP_INSTALLPATH), program);
        }
        if (!programFile.exists()) {
            throw new PluginException("Control error: program '" + program + "' not found");
        }

        config.setValue(PROP_PROGRAM, program);
        config.setValue(PROP_TIMEOUT, config.getValue(PostgreSQL.PROP_TIMEOUT));
        config.setValue(PROP_PROGRAMPREFIX, config.getValue(PostgreSQL.PROP_PREFIX));

        super.configure(config);
    }

    public void start() {
        log.info("[start]");
        String args[] = {"start"};
        doCommand(args);
    }

    public void stop() {
        log.info("[stop]");
        String args[] = {"stop"};
        doCommand(args);
    }
    
    public void restart() {
        log.info("[restart]");
        String args[] = {"restart"};
        doCommand(args);
    }
}
