/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
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

/**
 * @author Adar Margalit
 */

package org.hyperic.hq.product;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

import com.vmware.frantic.dao.versioncontrol.dto.FolderDto;
import com.vmware.frantic.fsmonitor.IChangeListener;
import com.vmware.frantic.fsmonitor.IFileMonitor;
import com.vmware.frantic.fsmonitor.data.EventActionsEnum;
import com.vmware.frantic.fsmonitor.data.EventMessage;

public class FileChangeTrackPlugin extends ConfigFileTrackPlugin{
    static final String PROP_FILES =
        ProductPlugin.TYPE_CONFIG_TRACK + ".files";
    
    private Map<String, FolderDto> _monitoredDirs = new HashMap<String, FolderDto>();
     
    protected static Log log =
        LogFactory.getLog(FileChangeTrackPlugin.class.getName());


    private EventHandler _eventHandler = null;
    
    private String _appdefEntityID;
        
    protected  class EventHandler  implements IChangeListener{
        public void onChange(EventMessage eventMsg) {
            // check if the event belongs to this plugin
            final String installDir = config.getValue(INSTALL_PATH);
            final String pathToCheck = EventActionsEnum.RENAME.equals(eventMsg.getType()) 
                ? eventMsg.getOldFullPath() 
                : eventMsg.getFullPath();
                    
            if (!pathToCheck.startsWith(installDir))
                return;
            
            final String id = getAppdefEntityID();
            if (id == null){
                log.error("Plugin id null, event not sent");
                return;
            }
            TrackEvent event = 
                new TrackEvent(id,
                               System.currentTimeMillis(),
                               LogTrackPlugin.LOGLEVEL_INFO,
                               eventMsg.getType().protocolValue(),
                               eventMsg.getFullPath(),
                               eventMsg.getOldFullPath(), 
                               eventMsg.getDiff());
                                                  
            getManager().reportEvent(event);    
        }
    }

    public void init(PluginManager manager)
        throws PluginException {
        super.init(manager);

    }
    
    /** currently a placeholder... **/
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        return super.getConfigSchema(info, config);
    }

    private static final String INSTALL_PATH = "config.installpath";

    public void configure(ConfigResponse config)
        throws PluginException {

        log.info("FileChangeTrackPlugin plugin configure()");

        this.config = config;
        setAppdefEntityID( config.getValue("entity.type")+":"+config.getValue("entity.id"));

        final PluginData data = getPluginData();
        final List<MonitoredFolderConfig> configs = data.getMonitoredFolders();
        if (configs == null || configs.size() <= 0)
            return;
        
        final String installDir = config.getValue(INSTALL_PATH);
        final Collection<FolderDto> folders = convert(installDir, configs);
        
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (_eventHandler == null){
            _eventHandler = new EventHandler();
            monitor.addListener(_eventHandler);
        }

        monitor.addMonitoredDirs(folders);
      }

    public void shutdown()
        throws PluginException {
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (_eventHandler != null){
            monitor.removeListener(_eventHandler);
            _eventHandler = null;
        }
        if (_monitoredDirs != null && _monitoredDirs.size() > 0){
            monitor.removeMonitoredDirs(_monitoredDirs.values());
            _monitoredDirs.clear();
        }
        super.shutdown();
    }
    
    private Collection<FolderDto> convert (final String installDir, final Collection<MonitoredFolderConfig> configs){
        if (configs == null || configs.size() <= 0)
            return null;
        
        for (final MonitoredFolderConfig config:configs){
            final FolderDto folder = convert(installDir, config);
            _monitoredDirs.put(folder.getPath(), folder);
        }
          
        return _monitoredDirs.values();
    }
    
    private FolderDto convert(final String installDir, final MonitoredFolderConfig folderConfig){
        if (folderConfig == null)
            return null;
        final FolderDto dto = new FolderDto();
        dto.setPath(installDir + File.separator +folderConfig.getPath());
        dto.setFilter(folderConfig.getFilter());
        dto.setRecursive(folderConfig.isRecursive());
        return dto;
    }

    protected String getAppdefEntityID() {
        return _appdefEntityID;
    }

    protected void setAppdefEntityID(String appdefEntityID) {
        _appdefEntityID = appdefEntityID;
    }

 
}
