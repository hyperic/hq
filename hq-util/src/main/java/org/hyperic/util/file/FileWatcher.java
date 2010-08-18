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

package org.hyperic.util.file;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.FileMonitoringRequest;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.file.monitor.polling.PollingFileMonitorService;

/**
 * Polls a set of files or directories for changes and notifies registered
 * listeners
 * @author jhickey
 * @author rladdad
 * 
 */
public class FileWatcher {
    private PollingFileMonitorService monitorService = new PollingFileMonitorService();
   

    private Set<FileOperation> ops = new HashSet<FileOperation>();

    private static final long DEFAULT_CHECK_INTERVAL = 1000;

    private long checkInterval = DEFAULT_CHECK_INTERVAL;
    
    private Timer monitorTimer;

    public FileWatcher() {
        ops.add(FileOperation.CREATED);
        ops.add(FileOperation.UPDATED);
        ops.add(FileOperation.DELETED);
    }

    private void addFileOrDir(String fileOrDir, boolean watchSubtrees) {
        File fileOrDirFile = new File(fileOrDir);
        MonitoringRequest request;

        if (fileOrDirFile.isDirectory()) {
            request = new DirectoryMonitoringRequest(fileOrDirFile, watchSubtrees, ops);
        } else {
            request = new FileMonitoringRequest(fileOrDirFile, ops);
        }
        monitorService.add(request);
    }

    /**
     * 
     * @param dir The directory to watch for changes (addition, removal, or update of files in directory)
     * @param watchSubtrees true if subdirectories should be monitored also
     */
    public void addDir(String dir, boolean watchSubtrees) {
        addFileOrDir(dir, watchSubtrees);
    }

    /**
     * 
     * @param file The file to watch for changes (update)
     */
    public void addFile(String file) {
        addFileOrDir(file, false);
    }

    /**
     * 
     * @param fileEventListener The listener to notify of changes
     */
    public void addFileEventListener(FileEventListener fileEventListener) {
        monitorService.addFileEventListener(fileEventListener);
    }

    /**
     * 
     * @param checkInterval The polling interval. This method has no effect if
     *        called after start
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void start() {
        this.monitorTimer = new Timer(true);
        monitorTimer.schedule(new MonitorTask(), 0, checkInterval);
    }
    
    public void stop() {
        this.monitorTimer.cancel();
    }

    private class MonitorTask
        extends TimerTask {

        @Override
        public void run() {
            monitorService.scanAll();
        }

    }
}
