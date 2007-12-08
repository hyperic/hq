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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.hqu.rendit.RenditServer;
import org.hyperic.hq.product.server.session.PluginsDeployedCallback;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.file.DirWatcher;
import org.hyperic.util.file.DirWatcher.DirWatcherCallback;
import org.jboss.system.server.ServerConfigLocator;

public class UIStartupListener implements StartupListener {
    private static final Log _log = LogFactory.getLog(UIStartupListener.class);

    private static final Object INIT_LOCK = new Object();
    private static boolean INITIALIZED;

    public void hqStarted() {
        ZeventManager.getInstance().addListener(ResourceDeletedZevent.class,
                                                ResourceDeleteWatcher.getInstance());
        HQApp app = HQApp.getInstance();
        
        app.registerCallbackListener(PluginsDeployedCallback.class,
                                     new UIPluginInitializer());
    }

    private static class UIPluginInitializer 
        implements PluginsDeployedCallback
    {
        public void pluginsDeployed(List plugins) {
            initPlugins();
        }
    }
    
    private static void initPlugins() {
        synchronized(INIT_LOCK) {
            if (INITIALIZED)
                return;
        
            File homeDir   = ServerConfigLocator.locate().getServerHomeDir();
            File deployDir = new File(homeDir, "deploy");
            File earDir    = new File(deployDir, "hq.ear");
            File warDir    = new File(earDir, "hq.war");
            File pluginDir = new File(warDir, "hqu");
            File sysDir    = new File(earDir, "rendit_sys");
            RenditServer.getInstance().setSysDir(sysDir);

            _log.info("HQU SysDir = [" + sysDir.getAbsolutePath() + "]");
            _log.info("Watching for HQU plugins in [" + 
                      pluginDir.getAbsolutePath() + "]");

            DirWatcherCallback cb = new DirWatcherCallback() {
                public void fileAdded(File f) {
                    if (f.getName().equals("public"))
                        return;

                    try {
                        RenditServer.getInstance().addPluginDir(f);
                    } catch(Exception e) {
                        _log.warn("Unable to add plugin in [" +
                                  f.getAbsolutePath() + "]", e);
                    }
                }

                public void fileRemoved(File f) {
                    if (f.getName().equals("public"))
                        return;

                    RenditServer.getInstance().removePluginDir(f.getName());
                }
            };


            File[] plugins = pluginDir.listFiles();
            for (int i = 0; i < plugins.length; i++) {
                cb.fileAdded(plugins[i]);
            }

            // Watch for plugin updates
            DirWatcher _watcher = new DirWatcher(pluginDir, cb,
                                                 Arrays.asList(plugins));
            Thread _watcherThread = new Thread(_watcher);
            _watcherThread.setDaemon(true);
            _watcherThread.start();
                
            INITIALIZED = true;
        }
    }    
}
