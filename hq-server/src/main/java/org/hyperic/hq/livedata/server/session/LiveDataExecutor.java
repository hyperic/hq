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

package org.hyperic.hq.livedata.server.session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.util.PluginLoader;



public class LiveDataExecutor extends ThreadPoolExecutor {

    private static Log _log = LogFactory.getLog(LiveDataExecutor.class);

    private static final int THREAD_MAX = 30;

    private List<LiveDataResult> _results;

    public LiveDataExecutor() {
        // Fixed sized threadpool.  The ThreadPoolExecutor will only spawn
        // the threads a necessary.
        super(THREAD_MAX, THREAD_MAX, 1, TimeUnit.SECONDS,
              new LinkedBlockingQueue());
        _results = new ArrayList<LiveDataResult>();
    }

    public void getData(LiveDataCommandsClient client, List<LiveDataExecutorCommand> commands) {
        execute(new LiveDataGatherer(client, commands));
    }

    public LiveDataResult[] getResult() {
        try {
            awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            _log.warn("Executor interrputed!");
        }

        _log.debug("Returning results for " + _results.size() + " elements");
        return (LiveDataResult[])_results.toArray(new LiveDataResult[0]);
    }
        
    private class LiveDataGatherer implements Runnable {

        private LiveDataCommandsClient _client;
        private List<LiveDataExecutorCommand> _commands;

        LiveDataGatherer(LiveDataCommandsClient client, List<LiveDataExecutorCommand> commands) {
            _client = client;
            _commands = commands;
        }

        public void run() {
            _log.debug("Starting gather thread...");
            for (LiveDataExecutorCommand cmd : _commands ) {
                _log.debug("Running cmd '" + cmd + "' in thread " +
                           Thread.currentThread().getName());
                boolean setClassLoader = false;
                if(cmd.getPlugin() != null) {
                    //We need to use the plugin's ClassLoader for serializing the XStream return value, 
                    //as it may require plugin-specific classes
                   setClassLoader = PluginLoader.setClassLoader(cmd.getPlugin());
                }
                LiveDataResult res;
                try {
                    res = _client.getData(cmd.getAppdefEntityID(),
                                                         cmd.getType(),
                                                         cmd.getCommand(),
                                                         cmd.getConfig());
                } catch (AgentRemoteException e) {
                    res = new LiveDataResult(cmd.getAppdefEntityID(), e, e.getMessage());
                }finally {
                    if (setClassLoader) {
                        PluginLoader.resetClassLoader(cmd.getPlugin());
                    }
                }
                
                _results.add(res);
            }
        }
    }
}
