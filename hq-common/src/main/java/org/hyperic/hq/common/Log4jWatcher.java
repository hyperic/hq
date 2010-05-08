package org.hyperic.hq.common;

import java.io.File;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;

public class Log4jWatcher {
    
    private Log log = LogFactory.getLog(Log4jWatcher.class);
    private final File log4jFile;

    public Log4jWatcher(String log4j) {
        URL url = getClass().getResource(log4j);
        log4jFile = new File(url.getFile());
        if (!log4jFile.exists()) {
            log.error("log4j file " + log4jFile.getAbsolutePath() + " does not exist", new Throwable());
            return;
        }
        log.info("Configuring log4j watcher with path="+log4jFile.getAbsoluteFile());
        DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 5000);
    }

}
