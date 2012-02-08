/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
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
 */

package org.hyperic.hq.product.server.session;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusDAO;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.appdef.server.session.AgentPluginSyncRestartThrottle;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.product.shared.PluginTypeEnum;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.timer.StopWatch;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
@Transactional(readOnly=true)
public class PluginManagerImpl implements PluginManager, ApplicationContextAware {
    private static final Log log = LogFactory.getLog(PluginManagerImpl.class);

    // this is hacky.  In a perfect world the plugin itself would define whether it is a "server"
    // plugin or not.  We shouldn't have to hardcode this :-(
    /** [HHQ-4776] a server plugin cannot be updated by the Plugin Manager UI */
    private static final Set<String> serverPlugins = new HashSet<String>();
    static {
        serverPlugins.add("system-plugin.jar");
        serverPlugins.add("netservices-plugin.jar");
        serverPlugins.add("netdevice-plugin.jar");
        serverPlugins.add("hqagent-plugin.jar");
    }
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String PLUGIN_DIR = "hq-plugins";
    private static final String AGENT_PLUGIN_DIR = "[/\\\\]pdk[/\\\\]plugins[/\\\\]";

    // used AtomicBoolean so that a groovy script may disable the mechanism live, no restarts
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    
    private final PermissionManager permissionManager;
    private final AgentSynchronizer agentSynchronizer;
    private final AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private final PluginDAO pluginDAO;
    private final AgentPluginStatusDAO agentPluginStatusDAO;
    private final MonitorableTypeDAO monitorableTypeDAO;
    private final ResourceManager resourceManager;
    private final AuthzSubjectManager authzSubjectManager;
    private final ZeventManager zeventManager;
    private final TransactionRetry transactionRetry;

    private ApplicationContext ctx;

    private File customPluginDir;


    @Autowired
    public PluginManagerImpl(PluginDAO pluginDAO, AgentPluginStatusDAO agentPluginStatusDAO,
                             MonitorableTypeDAO monitorableTypeDAO,
                             PermissionManager permissionManager,
                             ResourceManager resourceManager,
                             AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                             AgentSynchronizer agentSynchronizer,
                             AuthzSubjectManager authzSubjectManager,
                             ZeventManager zeventManager,
                             TransactionRetry transactionRetry) {
        this.pluginDAO = pluginDAO;
        this.agentPluginStatusDAO = agentPluginStatusDAO;
        this.monitorableTypeDAO = monitorableTypeDAO;
        this.permissionManager = permissionManager;
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.agentSynchronizer = agentSynchronizer;
        this.resourceManager = resourceManager;
        this.authzSubjectManager = authzSubjectManager;
        this.zeventManager = zeventManager;
        this.transactionRetry = transactionRetry;
    }

    @PostConstruct
    public void postConstruct() {
        zeventManager.addBufferedListener(PluginFileRemoveZevent.class,
            new ZeventListener<PluginFileRemoveZevent>() {
                public void processEvents(List<PluginFileRemoveZevent> events) {
                    for (final PluginFileRemoveZevent event : events) {
                        deletePluginFiles(event.getPluginFileNames());
                    }
                }
            }
        );
        zeventManager.addBufferedListener(PluginRemoveZevent.class,
            new ZeventListener<PluginRemoveZevent>() {
                public void processEvents(List<PluginRemoveZevent> events) {
                    for (final PluginRemoveZevent event : events) {
                        final Collection<String> pluginFileNames = event.getPluginFileNames();
                        final AuthzSubject subj = event.getAuthzSubject();
                        final PluginManager pluginManager = ctx.getBean(PluginManager.class);
                        final Runnable runner = new Runnable() {
                            public void run() {
                                try {
                                    pluginManager.removePlugins(subj, pluginFileNames);
                                } catch (PluginDeployException e) {
                                    log.error(e,e);
                                }
                            }
                        };
                        transactionRetry.runTransaction(runner, 3, 1000);
                    }
                }
            }
        );
    }
    
    public Plugin getByJarName(String jarName) {
        return pluginDAO.getByFilename(jarName);
    }

    @Transactional(readOnly=false)
    public void removePlugins(AuthzSubject subj, Collection<String> pluginFileNames)
    throws PluginDeployException {
        try {
            permissionManager.checkIsSuperUser(subj);
        } catch (PermissionException e) {
            throw new PluginDeployException("plugin.manager.deploy.super.user", e);
        }
        // [HHQ-4776] certain plugins should not be removed from HQ
        for (Iterator<String> it=pluginFileNames.iterator(); it.hasNext(); ) {
            String filename = it.next();
            if (serverPlugins.contains(filename)) {
                log.error("Attempt to remove plugin with filename=" + filename + " is being ignored" +
                          " since this is a plugin of type=" + PluginTypeEnum.SERVER_PLUGIN);
                it.remove();
            }
        }
        final Set<Agent> agents = new HashSet<Agent>();
        for (final String fileName : pluginFileNames) {
            agents.addAll(agentPluginStatusDAO.getPluginStatusByFileName(fileName).keySet());
        }
        final Map<String, Plugin> pluginMap = getPluginMap(pluginFileNames);
        removePluginsAndAssociatedResources(subj, new ArrayList<Plugin>(pluginMap.values()));
        final AgentPluginUpdater agentPluginUpdater =
            Bootstrap.getBean(AgentPluginUpdater.class);
        final Map<Integer, Collection<String>> toRemove =
            new HashMap<Integer, Collection<String>>(agents.size());
        for (final Agent agent : agents) {
            toRemove.put(agent.getId(), pluginFileNames);
        }
        agentPluginUpdater.queuePluginRemoval(toRemove);
        try {
            checkCanDeletePluginFiles(pluginFileNames);
        } catch (PluginDeployException e) {
            log.error(e,e);
        }
        removePluginsWithoutAssociatedStatuses(pluginFileNames, pluginMap);
        zeventManager.enqueueEventAfterCommit(new PluginFileRemoveZevent(pluginFileNames));
    }
    
    @Transactional(readOnly=false)
    public void removePluginsInBackground(AuthzSubject subj, Collection<String> pluginFileNames)
    throws PluginDeployException {
        try {
            permissionManager.checkIsSuperUser(subj);
        } catch (PermissionException e) {
            throw new PluginDeployException("plugin.manager.deploy.super.user", e);
        }

        final Collection<Plugin> plugins = pluginDAO.getPluginsByFileNames(pluginFileNames);
        for (final Plugin plugin : plugins) {
            plugin.setDeleted(true);
        }
        zeventManager.enqueueEventAfterCommit(new PluginRemoveZevent(subj, pluginFileNames));
    }
    
    private class PluginRemoveZevent extends Zevent {
        private final AuthzSubject subj;
        private final Collection<String> pluginFileNames;
        @SuppressWarnings("serial")
        public PluginRemoveZevent(AuthzSubject subj, Collection<String> pluginFileNames) {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
            this.subj = subj;
            this.pluginFileNames = pluginFileNames;
        }
        private AuthzSubject getAuthzSubject() {
            return subj;
        }
        private Collection<String> getPluginFileNames() {
            return pluginFileNames;
        }
    }

    @Transactional(readOnly=false, propagation=Propagation.REQUIRES_NEW)
    public void removeOrphanedPluginsInNewTran() throws PluginDeployException {
        final Collection<Plugin> plugins = agentPluginStatusDAO.getOrphanedPlugins();
        final boolean debug = log.isDebugEnabled();
        final Collection<String> pluginFileNames = new ArrayList<String>(plugins.size());
        for (final Plugin plugin : plugins) {
            if (debug) log.debug("removing orphaned plugin " + plugin);
            pluginFileNames.add(plugin.getPath());
        }
        final AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        removePlugins(overlord, pluginFileNames);
    }

    private void removePluginsWithoutAssociatedStatuses(Collection<String> pluginFileNames,
                                                        Map<String, Plugin> pluginMap) {
        final Map<String, Long> counts = agentPluginStatusDAO.getFileNameCounts(pluginFileNames);
        for (final String filename : pluginFileNames) {
            Long count = counts.get(filename);
            if (count == null || count <= 0) {
                final Plugin plugin = pluginMap.get(filename);
                pluginDAO.remove(plugin);
            }
        }
    }

    private Map<String, Plugin> getPluginMap(Collection<String> pluginFileNames) {
        final Collection<Plugin> plugins = pluginDAO.getPluginsByFileNames(pluginFileNames);
        final Map<String, Plugin> rtn = new HashMap<String, Plugin>(plugins.size());
        for (final Plugin plugin : plugins) {
            rtn.put(plugin.getPath(), plugin);
        }
        return rtn;
    }

    private void removePluginsAndAssociatedResources(AuthzSubject subj,
                                                     Collection<Plugin> plugins) {
        final long now = System.currentTimeMillis();
        for (final Plugin plugin : plugins) {
            if (plugin != null) {
                final Map<String, MonitorableType> map =
                    monitorableTypeDAO.findByPluginName(plugin.getName());
                resourceManager.removeResourcesAndTypes(subj, map.values());
                plugin.setDeleted(true);
                plugin.setModifiedTime(now);
            }
        }
    }
    
    public Collection<PluginTypeEnum> getPluginType(Plugin plugin) {
        final Collection<PluginTypeEnum> rtn = new HashSet<PluginTypeEnum>();
        final String pluginFile = plugin.getPath();
        final File customFile = new File(customPluginDir, pluginFile);
        final File defaultFile = new File(getServerPluginDir(), pluginFile);
        if (serverPlugins.contains(pluginFile)) {
            rtn.add(PluginTypeEnum.SERVER_PLUGIN);
        }
        if (customFile.exists()) {
            rtn.add(PluginTypeEnum.CUSTOM_PLUGIN);
        } else if (defaultFile.exists()) {
            rtn.add(PluginTypeEnum.DEFAULT_PLUGIN);
        }
        return rtn;
    }

    @Value(value="${server.custom.plugin.dir}")
    public void setCustomPluginDir(String customPluginDir) {
        if (this.customPluginDir != null) {
            return;
        }
        if (customPluginDir.trim().isEmpty()) {
            File wdParent = new File(System.getProperty("user.dir")).getParentFile();
            this.customPluginDir = new File(wdParent, PLUGIN_DIR);
        } else {
            final File file = new File(customPluginDir);
            if (!file.exists()) {
                final boolean success = file.mkdirs();
                if (!success) {
                    throw new SystemException("cannot create custom plugin dir, " + customPluginDir +
                                              ", as defined in hq-server.conf");
                }
            } else if (!file.isDirectory()) {
                throw new SystemException("custom plugin dir, " + customPluginDir +
                                          ", defined in hq-server.conf is not a directory");
            }
            this.customPluginDir = file;
        }
    }

    public File getCustomPluginDir() {
        return customPluginDir;
    }

    public File getServerPluginDir() {
        try {
            return ctx.getResource("WEB-INF/" + PLUGIN_DIR).getFile();
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }
    
    private void checkCanDeletePluginFiles(Collection<String> pluginFileNames)
    throws PluginDeployException {
        final File serverPluginDir = getServerPluginDir();
        final File customPluginDir = getCustomPluginDir();
        // Want this to be all or nothing, so first check if we can delete all the files
        for (final String filename : pluginFileNames) {
            final File customPlugin = new File(customPluginDir.getAbsolutePath() + "/" + filename);
            final File serverPlugin = new File(serverPluginDir.getAbsolutePath() + "/" + filename);
            if (!customPlugin.exists() && !serverPlugin.exists()) {
                String msg = "Could not remove plugin " + filename +
                             " from " + customPlugin.getAbsoluteFile() +
                             " or " + serverPlugin.getAbsoluteFile() + " file does not exist." +
                             "  Will ignore and continue with plugin removal";
                log.warn(msg);
            } else if (!canDelete(customPlugin) && !canDelete(serverPlugin)) {
                final String msg = "plugin.manager.delete.filesystem.perms";
                throw new PluginDeployException(
                    msg, filename, customPlugin.getAbsolutePath(), serverPlugin.getAbsolutePath());
            }
        }
    }

    private void deletePluginFiles(Collection<String> pluginFileNames) {
        final File serverPluginDir = getServerPluginDir();
        final File customPluginDir = getCustomPluginDir();
        for (final String filename : pluginFileNames) {
            final File customPlugin = new File(customPluginDir.getAbsolutePath() + "/" + filename);
            final File serverPlugin = new File(serverPluginDir.getAbsolutePath() + "/" + filename);
            customPlugin.delete();
            serverPlugin.delete();
        }
    }
    
    private boolean canDelete(File file) {
        if (!file.exists()) {
            return false;
        }
        // if a user does not have write perms to the dir or the file then they can't delete it
        if (!file.getParentFile().canWrite() && !file.canWrite()) {
            return false;
        }
        return true;
    }

    public Set<Integer> getAgentIdsInQueue() {
        final Set<Integer> rtn = new HashSet<Integer>();
        rtn.addAll(agentSynchronizer.getJobListByDescription(
            Arrays.asList(new String[]{AgentPluginUpdater.AGENT_PLUGIN_REMOVE,
                                       AgentPluginUpdater.AGENT_PLUGIN_TRANSFER})));
        rtn.addAll(agentPluginSyncRestartThrottle.getQueuedAgentIds());
        return rtn;
    }

    public Map<Integer, Long> getAgentIdsInRestartState() {
        return agentPluginSyncRestartThrottle.getAgentIdsInRestartState();
    }

    // XXX currently if one plugin validation fails all will fail.  Probably want to deploy the
    // plugins that are valid and return error status if any fail.
    public void deployPluginIfValid(AuthzSubject subj, Map<String, byte[]> pluginInfo)
    throws PluginDeployException {
        validatePluginFileNotInDeleteState(pluginInfo.keySet());
        final Collection<File> files = new ArrayList<File>();
        for (final Entry<String, byte[]> entry : pluginInfo.entrySet()) {
            final String filename = entry.getKey();
            final byte[] bytes = entry.getValue();
            File file = null;
            if (serverPlugins.contains(filename.toLowerCase())) {
                throw new PluginDeployException("plugin.cannot.deploy.server.type.plugin", filename);
            } else if (filename.toLowerCase().endsWith(".jar")) {
                file = getFileAndValidateJar(filename, bytes);
            } else if (filename.toLowerCase().endsWith(".xml")) {
                file = getFileAndValidateXML(filename, bytes);
            } else {
                throw new PluginDeployException("plugin.manager.bad.file.extension", filename);
            }
            files.add(file);
        }
        deployPlugins(files);
    }

    private void validatePluginFileNotInDeleteState(Collection<String> pluginFileNames)
    throws PluginDeployException {
        Collection<Plugin> plugins = pluginDAO.getPluginsByFileNames(pluginFileNames);
        for (Plugin plugin : plugins) {
            if (plugin == null) {
                continue;
            }
            if (plugin.isDeleted()) {
                throw new PluginDeployException("plugin.manager.plugin.is.deleted", plugin.getPath());
            }
        }
    }

    private File getFileAndValidateXML(String filename, byte[] bytes)
    throws PluginDeployException {
        FileWriter writer = null;
        File rtn = null;
        try {
            rtn = new File(TMP_DIR + File.separator + filename);
            final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            final Document doc = getDocument(new InputStreamReader(is), new HashMap<String, Reader>());
            final String name = doc.getRootElement().getName();
            if (!name.equals("plugin")) {
                throw new PluginDeployException("plugin.manager.invalid.xml", filename);
            }
            writer = new FileWriter(rtn);
            final String str = new String(bytes);
            writer.write(str);
            return rtn;
        } catch (JDOMException e) {
            if (rtn != null && rtn.exists()) {
                rtn.delete();
            }
            throw new PluginDeployException("plugin.manager.file.xml.wellformed.error", e, filename);
        } catch (IOException e) {
            if (rtn != null && rtn.exists()) {
                rtn.delete();
            }
            throw new PluginDeployException("plugin.manager.file.ioexception", e, filename);
        } finally {
            close(writer);
        }
    }

    private void deployPlugins(Collection<File> files) throws PluginDeployException {
        final File pluginDir = getCustomPluginDir();
        if (!pluginDir.exists() && !pluginDir.isDirectory() && !pluginDir.mkdir()) {
            throw new SystemException(pluginDir.getAbsolutePath() +
                " does not exist or is not a directory");
        }
        for (final File file : files) {
            final File dest = new File(pluginDir.getAbsolutePath() + File.separator + file.getName());
            if (!file.renameTo(dest) ) {
                // Rename sometimes fails on Windows for no apparent reason
                try {
                    FileUtil.copyFile(file, dest);
                } catch (FileNotFoundException e) {
                    throw new PluginDeployException("plugin.manager.file.notfound.exception", e);
                } catch (IOException e) {
                    throw new PluginDeployException("plugin.manager.file.ioexception", e, file.getName());
                } finally {
                    file.delete();
                }
            }
        }
    }

    private File getFileAndValidateJar(String filename, byte[] bytes) throws PluginDeployException {
        ByteArrayInputStream bais = null;
        JarInputStream jis = null;
        FileOutputStream fos = null;
        String file = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            jis = new JarInputStream(bais);
            final Manifest manifest = jis.getManifest();
            if (manifest == null) {
                throw new PluginDeployException("plugin.manager.jar.manifest.does.not.exist", filename);
            }
            file = TMP_DIR + File.separator + filename;
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            final File rtn = new File(file);
            final URL url = new URL("jar", "", "file:" + file + "!/");
            final JarURLConnection jarConn = (JarURLConnection) url.openConnection();
            final JarFile jarFile = jarConn.getJarFile();
            processJarEntries(jarFile, file, filename);
            return rtn;
        } catch (IOException e) {
            final File toRemove = new File(file);
            if (toRemove != null && toRemove.exists()) {
                toRemove.delete();
            }
            throw new PluginDeployException("plugin.manager.file.ioexception", e, filename);
        } finally {
            close(jis);
            close(fos);
        }
    }
    
    private void processJarEntries(JarFile jarFile, String jarFilename, String filename)
    throws PluginDeployException, IOException {
        final Map<String, JDOMException> xmlFailures = new HashMap<String, JDOMException>();
        boolean hasPluginRootElement = false;
        final Enumeration<JarEntry> entries = jarFile.entries();
        final Map<String, Reader> xmlReaders = getXmlReaderMap(jarFile);
        while (entries.hasMoreElements()) {
            Reader reader = null;
            String currXml = null;
            try {
                final JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (!entry.getName().toLowerCase().endsWith(".xml")) {
                    continue;
                }
                currXml = entry.getName();
                reader = xmlReaders.get(currXml);
                final Document doc = getDocument(reader, xmlReaders);
                if (doc.getRootElement().getName().toLowerCase().equals("plugin")) {
                    hasPluginRootElement = true;
                }
                currXml = null;
            } catch (JDOMException e) {
                log.debug(e,e);
                xmlFailures.put(currXml, e);
            }
        }
        if (!hasPluginRootElement) {
            final File toRemove = new File(jarFilename);
            if (toRemove != null && toRemove.exists()) {
                toRemove.delete();
            }
            if (!xmlFailures.isEmpty()) {
                for (final Entry<String, JDOMException> entry : xmlFailures.entrySet()) {
                    final String xml = entry.getKey();
                    JDOMException ex = entry.getValue();
                    log.error("could not parse " + xml, ex);
                }
                throw new PluginDeployException(
                    "plugin.manager.file.xml.wellformed.error", xmlFailures.keySet().toString());
            } else {
                throw new PluginDeployException("plugin.manager.no.plugin.root.element", filename);
            }
        }
    }

    private Map<String, Reader> getXmlReaderMap(JarFile jarFile) throws IOException {
        final Map<String, Reader> rtn = new HashMap<String, Reader>();
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (!entry.getName().toLowerCase().endsWith(".xml")) {
                continue;
            }
            InputStream is = null;
            try {
                is = jarFile.getInputStream(entry);
                final BufferedReader br = new BufferedReader(new InputStreamReader(is));
                final StringBuilder buf = new StringBuilder();
                String tmp;
                while (null != (tmp = br.readLine())) {
                    buf.append(tmp);
                }
                rtn.put(entry.getName(), new NoCloseStringReader(buf.toString()));
            } finally {
                close(is);
            }
        }
        return rtn;
    }

    private Document getDocument(Reader reader, final Map<String, Reader> xmlReaders)
    throws JDOMException, IOException {
        final SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(new EntityResolver() {
            // systemId = file:///pdk/plugins/process-metrics.xml
            public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
                final File entity = new File(systemId);
                for (final Entry<String, Reader> entry : xmlReaders.entrySet()) {
                    final String filename = entry.getKey();
                    if (entity.getAbsolutePath().contains(filename)) {
                        return new InputSource(entry.getValue());
                    }
                }
                final String filename = entity.getName().replaceAll(AGENT_PLUGIN_DIR, "");
                File file = new File(getCustomPluginDir(), filename);
                if (!file.exists()) {
                    file = new File(getServerPluginDir(), filename);
                }
                return (file.exists()) ?
                    new InputSource(file.toURI().toString()) :
                    new InputSource(xmlReaders.get(filename));
            }
        });
        return builder.build(reader);
    }
    
    private class NoCloseStringReader extends StringReader {
        private NoCloseStringReader(String s) {
            super(s);
        }
        @Override
        public void close() {}
    }

    public Map<Integer, Map<AgentPluginStatusEnum, Integer>> getPluginRollupStatus() {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        final Map<String, Plugin> pluginsByName = getAllPluginsByName();
        final List<Integer> statusIds = agentPluginStatusDAO.getAllIds();
        final Map<Integer, Map<AgentPluginStatusEnum, Integer>> rtn =
            new HashMap<Integer, Map<AgentPluginStatusEnum, Integer>>(statusIds.size());
        if (debug) watch.markTimeBegin("loop");
        for (final Integer statusId : statusIds) {
            if (debug) watch.markTimeBegin("get");
            final AgentPluginStatus status = agentPluginStatusDAO.get(statusId);
            if (debug) watch.markTimeEnd("get");
            if (status == null || status.getAgent().getPlatforms().isEmpty()) {
                continue;
            }
            final String name = status.getPluginName();
            final Plugin plugin = pluginsByName.get(name);
            if (plugin == null) {
                continue;
            }
            if (debug) watch.markTimeBegin("setPluginRollup");
            setPluginRollup(status, plugin.getId(), rtn);
            if (debug) watch.markTimeEnd("setPluginRollup");
        }
        if (debug) watch.markTimeEnd("loop");
        if (debug) log.debug(watch);
        return rtn;
    }
    
    private void setPluginRollup(AgentPluginStatus status, Integer pluginId,
                                 Map<Integer, Map<AgentPluginStatusEnum, Integer>> map) {
        Map<AgentPluginStatusEnum, Integer> tmp;
        if (null == (tmp = map.get(pluginId))) {
            tmp = new HashMap<AgentPluginStatusEnum, Integer>();
            tmp.put(AgentPluginStatusEnum.SYNC_FAILURE, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_SUCCESS, 0);
            map.put(pluginId, tmp);
        }
        final String lastSyncStatus = status.getLastSyncStatus();
        if (lastSyncStatus == null) {
            return;
        }
        final AgentPluginStatusEnum e = AgentPluginStatusEnum.valueOf(lastSyncStatus);
        tmp.put(e, tmp.get(e)+1);
    }
    
    public Plugin getPluginById(Integer id) {
        return pluginDAO.get(id);
    }
    
    @Transactional(readOnly=false)
    public void markDisabled(Collection<Integer> pluginIds) {
        final long now = System.currentTimeMillis();
        for (final Integer pluginId : pluginIds) {
            final Plugin plugin = pluginDAO.get(pluginId);
            if (plugin == null || plugin.isDeleted() || plugin.isDisabled()) {
                continue;
            }
            plugin.setDisabled(true);
            plugin.setModifiedTime(now);
        }
    }

    public Map<String, Integer> getAllPluginIdsByName() {
        final List<Plugin> plugins = pluginDAO.findAll();
        final Map<String, Integer> rtn = new HashMap<String, Integer>(plugins.size());
        for (final Plugin plugin : plugins) {
            rtn.put(plugin.getName(), plugin.getId());
        }
        return rtn;
    }

    private Map<String, Plugin> getAllPluginsByName() {
        final List<Plugin> plugins = pluginDAO.findAll();
        final Map<String, Plugin> rtn = new HashMap<String, Plugin>(plugins.size());
        for (final Plugin plugin : plugins) {
            rtn.put(plugin.getName(), plugin);
        }
        return rtn;
    }

    public Collection<AgentPluginStatus> getStatusesByPluginId(int pluginId, AgentPluginStatusEnum ... keys) {
        if (keys.length == 0) {
            return Collections.emptyList();
        }
        final Plugin plugin = pluginDAO.get(pluginId);
        if (plugin == null) {
            return Collections.emptyList();
        }
        return agentPluginStatusDAO.getPluginStatusByFileName(plugin.getPath(), Arrays.asList(keys));
    }
    
    public Map<Integer, AgentPluginStatus> getStatusesByAgentId(AgentPluginStatusEnum ... keys) {
        final Map<Integer, AgentPluginStatus> rtn = new HashMap<Integer, AgentPluginStatus>();
        final List<AgentPluginStatus> statuses = agentPluginStatusDAO.getPluginStatusByAgent(keys);
        for (final AgentPluginStatus status : statuses) {
            rtn.put(status.getAgent().getId(), status);
        }
        return rtn;
    }
    
    public boolean isPluginSyncEnabled() {
        return isEnabled.get();
    }
    
    @Value(value="${server.pluginsync.enabled}")
    public void setPluginSyncEnabled(boolean enabled) {
        isEnabled.set(enabled);
    }

    public Map<Plugin, Collection<AgentPluginStatus>> getOutOfSyncAgentsByPlugin() {
        return agentPluginStatusDAO.getOutOfSyncAgentsByPlugin();
    }

    public List<Plugin> getAllPlugins() {
        return pluginDAO.findAll();
    }

    public Collection<String> getOutOfSyncPluginNamesByAgentId(Integer agentId) {
        return agentPluginStatusDAO.getOutOfSyncPluginNamesByAgentId(agentId);
    }
     
    @Transactional(readOnly=false)
    public void updateAgentPluginSyncStatus(Integer agentId, AgentPluginStatusEnum from,
                                            AgentPluginStatusEnum to) {
        final Collection<Plugin> plugins = pluginDAO.findAll();
        final Map<String, AgentPluginStatus> statusMap =
            agentPluginStatusDAO.getStatusByAgentId(agentId);
        for (final Plugin plugin : plugins) {
            if (plugin == null || plugin.isDisabled()) {
                continue;
            }
            final AgentPluginStatus status = statusMap.get(plugin.getName());
            if (status == null || !status.getLastSyncStatus().equals(from.toString())) {
                continue;
            }
            status.setLastSyncStatus(to.toString());
        }
    }

    @Transactional(readOnly=false)
    public void updateAgentPluginSyncStatus(AgentPluginStatusEnum status,
                                            Map<Integer, Collection<Plugin>> agentToPlugins,
                                            Map<Integer, Collection<String>> agentToFileNames) {
        if (agentToPlugins == null) {
            agentToPlugins = Collections.emptyMap();
        }
        if (agentToFileNames == null) {
            agentToFileNames = Collections.emptyMap();
        }
        if (agentToPlugins.isEmpty() && agentToFileNames.isEmpty()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Set<Integer> agentIds = new HashSet<Integer>(agentToPlugins.keySet());
        agentIds.addAll(agentToFileNames.keySet());
        final Map<Integer, Map<String, AgentPluginStatus>> statusMap =
            agentPluginStatusDAO.getStatusByAgentIds(agentIds);
        for (final Entry<Integer, Collection<Plugin>> entry : agentToPlugins.entrySet()) {
            final Integer agentId = entry.getKey();
            final Map<String, AgentPluginStatus> map = statusMap.get(agentId);
            if (map == null) {
                continue;
            }
            final Collection<Plugin> plugins = entry.getValue();
            updateStatuses(agentId, plugins, map, now, status);
        }
        for (final Entry<Integer, Collection<String>> entry : agentToFileNames.entrySet()) {
            final Integer agentId = entry.getKey();
            final Map<String, AgentPluginStatus> map = statusMap.get(agentId);
            if (map == null) {
                continue;
            }
            final Collection<String> filenames = entry.getValue();
            final Collection<Plugin> plugins = pluginDAO.getPluginsByFileNames(filenames);
            updateStatuses(agentId, plugins, map, now, status);
        }
    }

    private void updateStatuses(Integer agentId, Collection<Plugin> plugins,
                                Map<String, AgentPluginStatus> map, long now,
                                AgentPluginStatusEnum s) {
        final String inProgress = AgentPluginStatusEnum.SYNC_IN_PROGRESS.toString();
        for (final Plugin plugin : plugins) {
            final AgentPluginStatus status = map.get(plugin.getName());
            if (status == null) {
                continue;
            }
            final String lastSyncStatus = status.getLastSyncStatus();
            if ((lastSyncStatus == null || !lastSyncStatus.equals(inProgress))
                    && s == AgentPluginStatusEnum.SYNC_IN_PROGRESS) {
                status.setLastSyncAttempt(now);
            }
            status.setLastSyncStatus(s.toString());
        }
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly=false)
    public void updateAgentPluginSyncStatusInNewTran(AgentPluginStatusEnum s, Integer agentId,
                                                     Collection<Plugin> plugins) {
        final String inProgress = AgentPluginStatusEnum.SYNC_IN_PROGRESS.toString();
        if (plugins == null) {
            plugins = pluginDAO.findAll();
        }
        if (plugins.isEmpty()) {
            return;
        }
        final Map<String, AgentPluginStatus> statusMap =
            agentPluginStatusDAO.getStatusByAgentId(agentId);
        final long now = System.currentTimeMillis();
        for (final Plugin plugin : plugins) {
            if (plugin == null || plugin.isDisabled()) {
                continue;
            }
            final AgentPluginStatus status = statusMap.get(plugin.getName());
            if (status == null) {
                continue;
            }
            // only setLastSyncAttempt if it changes from !"in progress" to "in progress"
            if (!status.getLastSyncStatus().equals(inProgress)
                    && s == AgentPluginStatusEnum.SYNC_IN_PROGRESS) {
                status.setLastSyncAttempt(now);
            }
            status.setLastSyncStatus(s.toString());
        }
    }

    private void close(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    private void close(Writer writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    private void close(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    @Transactional(readOnly=false)
    public void removeAgentPluginStatuses(Integer agentId, Collection<String> pluginFileNames) {
        agentPluginStatusDAO.removeAgentPluginStatuses(agentId, pluginFileNames);
    }

    @Transactional(readOnly=false)
    public void markDisabled(String pluginFileName) {
        final Plugin plugin = pluginDAO.getByFilename(pluginFileName);
        if (plugin == null || plugin.isDeleted()) {
            return;
        }
        if (!plugin.isDisabled()) {
            plugin.setDisabled(true);
            plugin.setModifiedTime(System.currentTimeMillis());
        }
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    @Transactional(readOnly=false)
    public void markPluginDisabledByName(String pluginName) {
        final Plugin plugin = pluginDAO.findByName(pluginName);
        if (plugin == null || plugin.isDeleted()) {
            return;
        }
        if (!plugin.isDisabled()) {
            plugin.setDisabled(true);
            plugin.setModifiedTime(System.currentTimeMillis());
        }
    }

    @Transactional(readOnly=false)
    public void markEnabled(String pluginName) {
        final Plugin plugin = pluginDAO.findByName(pluginName);
        if (plugin == null || plugin.isDeleted()) {
            return;
        }
        if (plugin.isDisabled()) {
            plugin.setDisabled(false);
            plugin.setModifiedTime(System.currentTimeMillis());
        }
    }
    
    private class PluginFileRemoveZevent extends Zevent {
        @SuppressWarnings("serial")
        private PluginFileRemoveZevent(Collection<String> pluginFileNames) {
            super(new ZeventSourceId() {}, new PluginFileRemovePayload(pluginFileNames));
        }
        private Collection<String> getPluginFileNames() {
            return ((PluginFileRemovePayload) getPayload()).getPluginFileNames();
        }
    }

    private class PluginFileRemovePayload implements ZeventPayload {
        private final Collection<String> pluginFileNames;
        private PluginFileRemovePayload(Collection<String> pluginFileNames) {
            this.pluginFileNames = pluginFileNames;
        }
        private Collection<String> getPluginFileNames() {
            return pluginFileNames;
        }
    }

}
