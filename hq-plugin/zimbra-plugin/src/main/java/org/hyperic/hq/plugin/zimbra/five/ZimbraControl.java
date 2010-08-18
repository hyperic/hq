/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.zimbra.five;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.util.config.ConfigResponse;

public class ZimbraControl extends ServerControlPlugin {
	private static Log log = LogFactory.getLog(ZimbraControl.class);

	private static final String[] actions = { "start", "stop","maintenance","status" };

	private static final List commands = Arrays.asList(actions);

    protected Log getLog() {
        return log;
    }
    
	public List getActions() {
		return commands;
	}

	public void start() {
		log.debug("start()");
		_exec("start");
	}
	
	public void status() {
		log.debug("status()");
		_exec("status");
	}
	
	public void stop() {
		log.debug("stop()");
		_exec("stop");
	}
	
	public void maintenance() {
		log.debug("maintenance()");
		_exec("maintenance");
	}

	private void _exec(String cmd) {
		log.debug("config = "+config);
		setControlProgram(new File(config.getValue("installpath"),"bin/zmcontrol").getAbsolutePath());
		super.doCommand(cmd);
	}
}
