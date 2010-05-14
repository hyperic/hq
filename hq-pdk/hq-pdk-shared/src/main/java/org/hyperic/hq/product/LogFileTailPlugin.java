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

package org.hyperic.hq.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LogFileTailPlugin
    extends LogFileTrackPlugin {

    private static Sigar sigar = null;

    private static Log log =
        LogFactory.getLog(LogFileTailPlugin.class.getName());

    private FileTail watcher = null;

    static void cleanup() {
        if (sigar != null) {
            sigar.close();
        }
    }

    public TrackEvent processLine(FileInfo info, String line) {
        return newTrackEvent(System.currentTimeMillis(),
                             LOGLEVEL_ERROR,
                             info.getName(),
                             line);
    }
    
    private FileTail getFileWatcher() {
        if (this.watcher == null) {
            log.debug("init file tail");

            if (sigar == null) {
                sigar = new Sigar();
            }
            
            this.watcher = new FileTail(sigar) {
                public void tail(FileInfo info, Reader reader) {
                    String line;
                    BufferedReader buffer =
                        new BufferedReader(reader);

                    try {
                        while ((line = buffer.readLine()) != null) {
                            TrackEvent event = processLine(info, line);
                            if (event != null) {
                                getManager().reportEvent(event);
                            }
                        }
                    } catch (IOException e) {
                        log.error(info.getName() + ": " + e.getMessage());
                    }
                }
            };

            getManager().addFileWatcher(this.watcher);
        }

        return this.watcher;
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        String[] files = getFiles(config);
        
        if (debugLogging) {
            debugLog("Adding file watchers for files=" +
                     Arrays.asList(files));
        }
        
        try {
            getFileWatcher().add(files);
        } catch (SigarException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown()
        throws PluginException {

        if (this.watcher != null) {
            debugLog("Removing file watcher");
            getManager().removeFileWatcher(this.watcher);
            this.watcher = null;
        }

        super.shutdown();
    }
}
