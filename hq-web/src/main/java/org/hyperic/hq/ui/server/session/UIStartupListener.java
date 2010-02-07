/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
package org.hyperic.hq.ui.server.session;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.file.DirWatcher;
import org.hyperic.util.file.DirWatcher.DirWatcherCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UIStartupListener implements StartupListener {
    private final Log log = LogFactory.getLog(UIStartupListener.class);

    private ZeventEnqueuer zeventManager;
    private RenditServer renditServer;
    private ResourceDeleteWatcher resourceDeleteWatcher;
    private HQApp hqApp;
    //TODO this is only injected to ensure that PPD.start() is called first to unpack ui plugins from product plugins.  Make this cleaner
    private ProductPluginDeployer productPluginDeployer;

    @Autowired
    public UIStartupListener(ZeventEnqueuer zeventManager, RenditServer renditServer,
                             ResourceDeleteWatcher resourceDeleteWatcher, HQApp hqApp, ProductPluginDeployer productPluginDeployer) {
        this.zeventManager = zeventManager;
        this.renditServer = renditServer;
        this.resourceDeleteWatcher = resourceDeleteWatcher;
        this.hqApp = hqApp;
        this.productPluginDeployer = productPluginDeployer;
    }

    @PostConstruct
    public void hqStarted() {
        HashSet<Class<ResourceDeletedZevent>> events = new HashSet<Class<ResourceDeletedZevent>>();
        events.add(ResourceDeletedZevent.class);
        zeventManager.addBufferedListener(events, resourceDeleteWatcher);
        initPlugins();
    }

    private void initPlugins() {
       
        File warDir = hqApp.getWebAccessibleDir();
        File pluginDir = new File(warDir, "hqu");
        File sysDir = new File(warDir + "/WEB-INF", "rendit_sys");
        renditServer.setSysDir(sysDir);

        log.info("HQU SysDir = [" + sysDir.getAbsolutePath() + "]");
        log.info("Watching for HQU plugins in [" + pluginDir.getAbsolutePath() + "]");

        DirWatcherCallback cb = new DirWatcherCallback() {
            public void fileAdded(File f) {
                if (f.getName().equals("public") || !f.isDirectory())
                    return;

                try {
                    renditServer.addPluginDir(f);
                } catch (Exception e) {
                    log.warn("Unable to add plugin in [" + f.getAbsolutePath() + "]", e);
                }
            }

            public void fileRemoved(File f) {
                if (f.getName().equals("public"))
                    return;

                renditServer.removePluginDir(f.getName());
            }
        };

        File[] plugins = pluginDir.listFiles();
        if (plugins == null) {
            return;
        }
        for (int i = 0; i < plugins.length; i++) {
            try {
                cb.fileAdded(plugins[i]);
            } catch (Throwable t) {
                log.error("Error loading plugin [" + plugins[i] + "]", t);
            }
        }

        // Watch for plugin updates
        DirWatcher _watcher = new DirWatcher(pluginDir, cb, Arrays.asList(plugins));
        Thread _watcherThread = new Thread(_watcher);
        _watcherThread.setDaemon(true);
        _watcherThread.start();

    }
}
