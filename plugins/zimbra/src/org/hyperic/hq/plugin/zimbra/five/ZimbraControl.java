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
