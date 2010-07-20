/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

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
 *
 * @author administrator
 */
public class RabbitMQBrokerLog extends LogFileTailPlugin {

    Log log = LogFactory.getLog(RabbitMQBrokerLog.class);
    List levels = Arrays.asList(new String[]{"ERROR","WARN","INFO","DEBUG"}); // XXXX 
    Pattern levelPat = Pattern.compile("=+(\\w*)[^=]*=+");
    int level=3; // info by default.

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        log.debug("[configure] config=" + config);

        String[] files = getFiles(config);
        log.debug("[configure]Adding file watchers for files=" + Arrays.asList(files));

        super.configure(config);
    }

    @Override
    public TrackEvent processLine(FileInfo info, String line) {
        TrackEvent res = null;
        log.debug("[processLine] line=" + line);
        Matcher m = levelPat.matcher(line);
        if (m.find()) {
            level=levels.indexOf(m.group(1));
            if (log.isDebugEnabled()) {
                log.debug("[processLine] level=" + m.group(1) + " (" + level + ")");
            }
        }else if(line.length()>1){
            res=newTrackEvent(System.currentTimeMillis(),
                             LOGLEVEL_ERROR+level,
                             info.getName(),
                             line);
        }
        return res;
    }
}
