/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

package org.hyperic.hq.common;

import java.io.File;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;

public class Log4jWatcher {
    
    private Log log = LogFactory.getLog(Log4jWatcher.class);

    public Log4jWatcher(String log4j) {
        URL url = getClass().getResource(log4j);
        if (url == null) {
            log.error("resource for log4j file " + log4j + " does not exist");
            return;
        }
        
    	File log4jFile = new File(url.getFile());
    	// File must exist, because the URL resource was located -- no need to decode
    	
        log.info("Configuring log4j watcher with path="+log4jFile.getAbsoluteFile());
        DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 5000);
    }

}
