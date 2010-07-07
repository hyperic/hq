/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClientFactory;
import org.hyperic.hq.livedata.formatters.CpuInfoFormatter;
import org.hyperic.hq.livedata.formatters.CpuPercFormatter;
import org.hyperic.hq.livedata.formatters.DfFormatter;
import org.hyperic.hq.livedata.formatters.IfconfigFormatter;
import org.hyperic.hq.livedata.formatters.NetstatFormatter;
import org.hyperic.hq.livedata.formatters.ToStringFormatter;
import org.hyperic.hq.livedata.formatters.TopFormatter;
import org.hyperic.hq.livedata.formatters.WhoFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.livedata.shared.LiveDataException;
import org.hyperic.hq.livedata.shared.LiveDataManager;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class LiveDataManagerImpl implements LiveDataManager {

    private Log log = LogFactory.getLog(LiveDataManagerImpl.class);

    private LiveDataPluginManager manager;
    private Cache cache;

    private final String CACHENAME = "LiveData";
    private final long NO_CACHE = -1;

    private ProductManager productManager;

    private ConfigManager configManager;

    private LiveDataCommandsClientFactory liveDataCommandsClientFactory;

    @Autowired
    public LiveDataManagerImpl(ProductManager productManager, ConfigManager configManager,
                               LiveDataCommandsClientFactory liveDataCommandsClientFactory) {
        this.productManager = productManager;
        this.configManager = configManager;
        this.liveDataCommandsClientFactory = liveDataCommandsClientFactory;
    }

    @PostConstruct
    public void init() {
        // Initialize local objects
        try {
            manager = (LiveDataPluginManager) productManager
                .getPluginManager(ProductPlugin.TYPE_LIVE_DATA);
            cache = CacheManager.getInstance().getCache(CACHENAME);
        } catch (Exception e) {
            log.error("Unable to initialize LiveData manager", e);
        }
        registerFormatter(new ToStringFormatter());
        registerFormatter(new CpuPercFormatter());
        registerFormatter(new WhoFormatter());
        registerFormatter(new TopFormatter());
        registerFormatter(new CpuInfoFormatter());
        registerFormatter(new DfFormatter());
        registerFormatter(new IfconfigFormatter());
        registerFormatter(new NetstatFormatter());
    }

    /**
     * Live data subsystem uses measurement configs.
     */
    private ConfigResponse getConfig(AuthzSubject subject, LiveDataCommand command)
        throws LiveDataException {
        try {
            AppdefEntityID id = command.getAppdefEntityID();
            ConfigResponse config = command.getConfig();

            try {
                ConfigResponse mConfig = configManager.getMergedConfigResponse(subject,
                    ProductPlugin.TYPE_MEASUREMENT, id, true);
                mConfig.merge(config, false);
                return mConfig;
            } catch (ConfigFetchException e) {
                // No measurement config? No problem
                return config;
            }
        } catch (Exception e) {
            throw new LiveDataException(e);
        }
    }

    /**
     * Get the appdef type for a given entity id.
     */
    private AppdefResourceType getType(AuthzSubject subject, LiveDataCommand cmd)
        throws AppdefEntityNotFoundException, PermissionException {
        AppdefEntityID id = cmd.getAppdefEntityID();
        AppdefEntityValue val = new AppdefEntityValue(id, subject);

        AppdefResourceType typeVal = val.getAppdefResourceType();
        return typeVal;
    }

    private String getPlugin(AppdefResourceType resourceType) {
        String plugin = null;
        if (resourceType instanceof ServiceType) {
            plugin = ((ServiceType) resourceType).getPlugin();
        } else if (resourceType instanceof ServerType) {
            plugin = ((ServerType) resourceType).getPlugin();
        } else if (resourceType instanceof PlatformType) {
            plugin = ((PlatformType) resourceType).getPlugin();
        }
        return plugin;
    }

    private void putElement(LiveDataCommand cmd, LiveDataResult res) {
        putElement(new LiveDataCommand[] { cmd }, new LiveDataResult[] { res });
    }

    private void putElement(LiveDataCommand[] cmds, LiveDataResult[] res) {
        LiveDataCacheKey key = new LiveDataCacheKey(cmds);
        LiveDataCacheObject obj = new LiveDataCacheObject(res);
        Element e = new Element(key, obj);
        cache.put(e);
    }

    private LiveDataResult getElement(LiveDataCommand cmd, long timeout) {
        LiveDataResult[] res = getElement(new LiveDataCommand[] { cmd }, timeout);
        return res == null ? null : res[0];
    }

    private LiveDataResult[] getElement(LiveDataCommand[] cmds, long timeout) {
        LiveDataCacheKey key = new LiveDataCacheKey(cmds);
        Element e = cache.get(key);
        if (e == null) {
            return null;
        }

        LiveDataCacheObject obj = (LiveDataCacheObject) e.getObjectValue();

        if (System.currentTimeMillis() > obj.getCtime() + timeout) {
            // Object is expired
            cache.remove(key);
            return null;
        }

        log.info("Returning cached result " + StringUtil.arrayToString(obj.getResult()));
        return obj.getResult();
    }

    /**
     * Run the given live data command.
     * 
     * 
     */
    public LiveDataResult getData(AuthzSubject subject, LiveDataCommand cmd)
        throws AppdefEntityNotFoundException, PermissionException, AgentNotFoundException,
        LiveDataException {
        return getData(subject, cmd, NO_CACHE);
    }

    /**
     * Run the given live data command. If cached data is found that is not
     * older than the cachedTimeout the cached data will be returned.
     * 
     * @param cacheTimeout
     * 
     */
    public LiveDataResult getData(AuthzSubject subject, LiveDataCommand cmd, long cacheTimeout)
        throws PermissionException, AgentNotFoundException, AppdefEntityNotFoundException,
        LiveDataException {
        // Attempt load from cache
        LiveDataResult res;

        if (cacheTimeout != NO_CACHE) {
            res = getElement(cmd, cacheTimeout);
            if (res != null) {
                return res;
            }
        }

        AppdefEntityID id = cmd.getAppdefEntityID();

        LiveDataCommandsClient client = liveDataCommandsClientFactory.getClient(id);

        ConfigResponse config = getConfig(subject, cmd);

        AppdefResourceType resourceType = getType(subject, cmd);
        String type = resourceType.getName();
        String pluginName = getPlugin(resourceType);

        boolean setClassLoader = false;
        GenericPlugin productPlugin = null;
        if (pluginName != null) {
            // We need to use the plugin's ClassLoader for serializing the
            // XStream return value,
            // as it may require plugin-specific classes
            try {
                productPlugin = manager.getParent().getPlugin(pluginName);
                setClassLoader = PluginLoader.setClassLoader(productPlugin);
            } catch (PluginNotFoundException e) {
                throw new LiveDataException(e);
            }
        }

        try {
            res = client.getData(id, type, cmd.getCommand(), config);
        } catch (AgentRemoteException e) {
            res = new LiveDataResult(id, e, e.getMessage());
        } finally {
            if (setClassLoader) {
                PluginLoader.resetClassLoader(productPlugin);
            }
        }

        if (cacheTimeout != NO_CACHE) {
            putElement(cmd, res);
        }

        return res;
    }

    /**
     * Run a list of live data commands in batch.
     * 
     * 
     */
    public LiveDataResult[] getData(AuthzSubject subject, LiveDataCommand[] commands)
        throws AppdefEntityNotFoundException, PermissionException, AgentNotFoundException,
        LiveDataException {
        return getData(subject, commands, NO_CACHE);
    }

    /**
     * Run a list of live data commands in batch. If cached data is found that
     * is not older than the cacheTimeout the cached data will be returned.
     * 
     * @param cacheTimeout The cache timeout given in milliseconds.
     * 
     */
    public LiveDataResult[] getData(AuthzSubject subject, LiveDataCommand[] commands,
                                    long cacheTimeout) throws PermissionException,
        AppdefEntityNotFoundException, AgentNotFoundException, LiveDataException {
        // Attempt load from cache
        LiveDataResult[] res;
        if (cacheTimeout != NO_CACHE) {
            res = getElement(commands, cacheTimeout);
            if (res != null) {
                return res;
            }
        }

        HashMap<AppdefEntityID, List<LiveDataExecutorCommand>> buckets = new HashMap<AppdefEntityID, List<LiveDataExecutorCommand>>();
        for (int i = 0; i < commands.length; i++) {
            LiveDataCommand cmd = commands[i];
            AppdefEntityID id = cmd.getAppdefEntityID();

            ConfigResponse config = getConfig(subject, cmd);
            AppdefResourceType resourceType = getType(subject, cmd);
            String type = resourceType.getName();
            String pluginName = getPlugin(resourceType);
            GenericPlugin productPlugin = null;
            if (pluginName != null) {
                try {
                    productPlugin = manager.getParent().getPlugin(pluginName);
                } catch (PluginNotFoundException e) {
                    throw new LiveDataException(e);
                }
            }
            LiveDataExecutorCommand exec = new LiveDataExecutorCommand(id, type, cmd.getCommand(),
                config, productPlugin);

            List<LiveDataExecutorCommand> queue = buckets.get(id);
            if (queue == null) {
                queue = new ArrayList<LiveDataExecutorCommand>();
                queue.add(exec);
                buckets.put(id, queue);
            } else {
                queue.add(exec);
            }
        }

        LiveDataExecutor executor = new LiveDataExecutor();
        for (AppdefEntityID id : buckets.keySet()) {
            List<LiveDataExecutorCommand> cmds = buckets.get(id);

            LiveDataCommandsClient client = liveDataCommandsClientFactory.getClient(id);

            executor.getData(client, cmds);
        }

        executor.shutdown();

        res = executor.getResult();

        if (cacheTimeout != NO_CACHE) {
            putElement(commands, res);
        }

        return res;
    }

    /**
     * Get the available commands for a given resources.
     * 
     * 
     */
    public String[] getCommands(AuthzSubject subject, AppdefEntityID id) throws PluginException,
        PermissionException {
        try {
            AppdefEntityValue val = new AppdefEntityValue(id, subject);
            AppdefResourceType tVal = val.getAppdefResourceType();

            return manager.getCommands(tVal.getName());
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginNotFoundException("No plugin found for " + id, e);
        }
    }

    /**
     * 
     */
    public void registerFormatter(LiveDataFormatter f) {
        log.info("Registering formatter [" + f.getName() + "]: " + f.getDescription());
        FormatterRegistry.getInstance().registerFormatter(f);
    }

    /**
     * 
     */
    public void unregisterFormatter(LiveDataFormatter f) {
        log.info("Unregistering formatter [" + f.getName() + "]");
        FormatterRegistry.getInstance().unregisterFormatter(f);
    }

    /**
     * Gets a set of {@link LiveDataFormatter}s which are able to format the
     * passed command.
     * 
     * 
     */
    public Set<LiveDataFormatter> findFormatters(LiveDataCommand cmd, FormatType type) {
        return FormatterRegistry.getInstance().findFormatters(cmd, type);
    }

    /**
     * Find a formatter based on its 'id' property.
     * 
     */
    public LiveDataFormatter findFormatter(String id) {
        return FormatterRegistry.getInstance().findFormatter(id);
    }

    /**
     * Get the ConfigSchema for a given resource.
     * 
     * 
     */
    public ConfigSchema getConfigSchema(AuthzSubject subject, AppdefEntityID id, String command)
        throws PluginException, PermissionException {
        try {
            AppdefEntityValue val = new AppdefEntityValue(id, subject);
            AppdefResourceType tVal = val.getAppdefResourceType();

            return manager.getConfigSchema(tVal.getName(), command);
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginNotFoundException("No plugin found for " + id, e);
        }
    }

    /**
     * For testing without the agent
     * @param liveDataCommandsClientFactory An implementation of @{link
     *        {@link LiveDataCommandsClientFactory} to use for invoking live
     *        data commands
     */
    void setLiveDataCommandsClientFactory(
                                          LiveDataCommandsClientFactory liveDataCommandsClientFactory) {
        this.liveDataCommandsClientFactory = liveDataCommandsClientFactory;
    }
}
