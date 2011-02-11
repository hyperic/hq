package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("agentPluginUpdater")
@Transactional
public class AgentPluginUpdaterImpl implements AgentPluginUpdater {
    
    private static final int NUM_WORKERS = 1;
    private final ThreadPoolExecutor executor;
//    private AgentPluginStatusDAO agentPluginStatusDAO;
    private AuthzSubject overlord;
    private PluginDAO pluginDAO;
    private static final Log log = LogFactory.getLog(AgentPluginUpdater.class);
    
    @Autowired
    public AgentPluginUpdaterImpl(AgentPluginStatusDAO agentPluginStatusDAO,
                                  PluginDAO pluginDAO,
                                  AuthzSubjectManager authzSubjectManager) {
//        this.agentPluginStatusDAO = agentPluginStatusDAO;
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.pluginDAO = pluginDAO;
        final ThreadFactory factory = new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "AgentPluginSynchronizer" + i.getAndIncrement());
            }
        };
        executor = new ThreadPoolExecutor(NUM_WORKERS, NUM_WORKERS, 0, TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<Runnable>(), factory);
    }
    
    @PostConstruct
    public void initialize() {
//        Map<Agent, Collection<Plugin>> agents = agentPluginStatusDAO.getOutOfSyncPluginsByAgent();
//        queuePluginTransfer(agents);
    }
    
    public void queuePluginTransfer(final Map<Integer, Collection<Plugin>> map) {
        if (map.isEmpty()) {
            return;
        }
        executor.execute(new Runnable() {
            public void run() {
                final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
                for (final Entry<Integer, Collection<Plugin>> entry : map.entrySet()) {
                    final Integer agentId = entry.getKey();
                    final Collection<Plugin> plugins = entry.getValue();
                    final Collection<String> pluginNames = new ArrayList<String>(plugins.size());
                    for (final Plugin plugin : plugins) {
//                        Plugin plugin = pluginDAO.get(pluginId);
                        pluginNames.add(plugin.getPath());
                    }
                    try {
                        agentManager.transferAgentPlugins(overlord, agentId, pluginNames);
// XXX disabled for now
//                        agentManager.restartAgent(overlord, agentId);
                    } catch (Exception e) {
                        log.error(e,e);
                    }
                }
            }
        });
    }

}
