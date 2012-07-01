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

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

public class TableControlPlugin extends ResourceControl {

    private static Log log = LogFactory.getLog(TableControlPlugin.class);

    @Override
    public void doAction(String action, String[] args) throws PluginException {
        String query = null;

        if (log.isDebugEnabled()) {
            log.debug("[doAction] action='" + action + "' args=" + Arrays.asList(args));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String opt = sb.toString().trim();

        if (action.equals("Analyze")) {
            query = "ANALYZE " + opt + " " + this.table;
        } else if (action.equals("Vacuum")) {
            query = "VACUUM " + opt + " " + this.table;
        } else if (action.equals("VacuumAnalyze")) {
            query = "VACUUM " + opt + " ANALYZE " + this.table;
        } else if (action.equals("Reindex")) {
            query = "REINDEX TABLE " + this.table + " " + opt;
        }

        if (query == null) {
            throw new PluginException("Action '" + action + "' not supported");
        }

        log.debug("[doAction] query=" + query);
        execute(query);
    }
}
