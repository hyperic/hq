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

package org.hyperic.hq.install;

import java.io.File;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EarlyExitException;

public class AgentConfig extends BaseConfig {

    public AgentConfig () {
        super("agent");
    }

    public String getName () { return PRODUCT + " agent"; }

    protected ConfigSchema getInstallSchema (ConfigResponse previous,
                                             int iterationCount) 
        throws EarlyExitException {

        if ( iterationCount > 0 ) return null;

        return super.getInstallSchema(previous, iterationCount);
    }

    public static final String[] MARKER_FILES
        = { "hq-agent.sh", "hq-agent.bat" };

    protected String[] getMarkerFiles () {
        return MARKER_FILES;
    }

    public String getCompletionText (ConfigResponse config) {
        StringBuffer s = new StringBuffer();
        String sp = File.separator;
        String startup = getProductInstallDir(config);
        startup += "bin" + sp + PRODUCT.toLowerCase() + "-agent" + getScriptExtension();
        char nl = '\n';
        s.append("__ll__")
            .append(nl)
            .append(" You can now start your HQ agent by running this command:")
            .append(nl).append(nl).append("  ").append(startup).append(" start")
            .append(nl).append("__ll__");
        return s.toString();
    }
}
