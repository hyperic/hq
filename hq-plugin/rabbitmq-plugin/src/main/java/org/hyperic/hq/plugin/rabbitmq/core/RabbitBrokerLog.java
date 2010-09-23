/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;
import org.hyperic.util.config.ConfigResponse;

/**
 * RabbitBrokerLog
 * @author German Laullon 
 */
public class RabbitBrokerLog extends LogFileTailPlugin {

    private static final Log log = LogFactory.getLog(RabbitBrokerLog.class);

    private static final List<String> levels = Arrays.asList("ERROR","WARN","INFO","DEBUG");

    private static final Pattern levelPat = Pattern.compile("=+(\\w*)[^=]*=+");

    /** info by default. */
    private static final int DEFAULT_LOG_LEVEL = 3;

    private static int level = DEFAULT_LOG_LEVEL;

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        log.debug("[configure] config=" + config);
        
        String[] files = getFiles(config);
        log.debug("[configure] Adding file watchers for files=" + Arrays.asList(files));

        super.configure(config);
    }

    @Override
    public TrackEvent processLine(FileInfo info, String line) {
        TrackEvent trackEvent = null;
        log.debug("[processLine] line=" + line);
        Matcher matcher = levelPat.matcher(line);

        if (matcher.find()) {
            level = levels.indexOf(matcher.group(1));
            if (log.isDebugEnabled()) {
                log.debug("[processLine] level=" + matcher.group(1) + " (" + level + ")");
            }
        }
        else if (line.length() > 1){
            trackEvent = newTrackEvent(System.currentTimeMillis(),
                             LOGLEVEL_ERROR + level,
                             info.getName(),
                             line);
        }
        return trackEvent;
    }
}
