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

package org.hyperic.hq.plugin.system;

import java.io.File;

import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigResponse;

public class FileControlPlugin extends ServerControlPlugin {

    private File file;

    private static final List ACTIONS =
        Arrays.asList(new String[] { "run" });

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        this.file = new File(config.getValue(SystemPlugin.PROP_PATH));
        setControlProgram(this.file.getAbsolutePath());
    }

    public List getActions() {
        return ACTIONS;
    }

    public void doAction(String action, String[] args)
        throws PluginException
    {
        if (this.file.isDirectory()) {
            throw new PluginException(this.file + " is a directory.");
        }

        doCommand(args);
    }

    //ServerControlPlugin defaults to servers only
    protected boolean useConfigSchema(TypeInfo info) {
        return true;
    }
}
