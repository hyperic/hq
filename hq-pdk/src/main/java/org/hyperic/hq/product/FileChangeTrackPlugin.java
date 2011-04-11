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
import org.hyperic.cm.filemonitor.IChangeListener;
import org.hyperic.cm.filemonitor.IFileMonitor;
import org.hyperic.cm.filemonitor.data.EventActionsEnum;
import org.hyperic.cm.filemonitor.data.EventMessage;
import org.hyperic.cm.versioncontrol.dto.FolderDto;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class FileChangeTrackPlugin extends ConfigFileTrackPlugin{
    
    private Map<String, FolderDto> monitoredDirs = new HashMap<String, FolderDto>();
     
    protected static Log log =
        LogFactory.getLog(FileChangeTrackPlugin.class.getName());


    private EventHandler eventHandler = null;
    
//    private String _appdefEntityID;
        
    protected  class EventHandler  implements IChangeListener{
        public void onChange(EventMessage eventMsg) {
            // check if the event belongs to this plugin
            final String installpath =
                config.getValue(ProductPlugin.PROP_INSTALLPATH);
            
            final String pathToCheck = EventActionsEnum.RENAME.equals(eventMsg.getType()) 
                ? eventMsg.getOldFullPath() 
                : eventMsg.getFullPath();
                    
            if (installpath == null || pathToCheck == null){
                log.error("Unexpected null value - install path: "+installpath+", path to check: "+pathToCheck);
                return;
            }
            
            if (!pathToCheck.startsWith(installpath))
                return;
            
            TrackEvent event = 
                new TrackEvent(getName(),
                               System.currentTimeMillis(),
                               LogTrackPlugin.LOGLEVEL_INFO,
                               eventMsg.getType().protocolValue(),
                               eventMsg.getFullPath(),
                               eventMsg.getOldFullPath(), 
                               eventMsg.getDiff());
                                                  
            getManager().reportEvent(event);    
        }
    }

    /*** temporary implementation just so something should show up ***/
    protected String getDefaultConfigFile(TypeInfo info, ConfigResponse config) {
        final List<MonitoredFolderConfig> configs = data.getMonitoredFolders();
        if (configs == null || configs.size() <= 0)
            return "";

        final StringBuffer sb = new StringBuffer();
        for (final MonitoredFolderConfig folderConf: configs){
            if (sb.length() > 0)
                sb.append(",");
            sb.append(folderConf.getPath()+";"+folderConf.isRecursive()+";"+folderConf.getFilter());
        }
        return sb.toString();
    }

    
    /** currently a placeholder... **/
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        return super.getConfigSchema(info, config);
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        log.info("FileChangeTrackPlugin plugin configure()");

        this.config = config;

        final PluginData data = getPluginData();
        final List<MonitoredFolderConfig> configs = data.getMonitoredFolders();
        if (configs == null || configs.size() <= 0)
            return;
        
        final String installpath =
            config.getValue(ProductPlugin.PROP_INSTALLPATH);
        final Collection<FolderDto> folders = convert(installpath, configs);
        
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (eventHandler == null){
            eventHandler = new EventHandler();
            monitor.addListener(eventHandler);
        }

        monitor.addMonitoredDirs(folders);
      }

    public void shutdown()
        throws PluginException {
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (eventHandler != null){
            monitor.removeListener(eventHandler);
            eventHandler = null;
        }
        if (monitoredDirs != null && monitoredDirs.size() > 0){
            monitor.removeMonitoredDirs(monitoredDirs.values());
            monitoredDirs.clear();
        }
        super.shutdown();
    }
    
    private Collection<FolderDto> convert (final String installDir, final Collection<MonitoredFolderConfig> configs){
        if (configs == null || configs.size() <= 0)
            return null;
        
        for (final MonitoredFolderConfig config:configs){
            final FolderDto folder = convert(installDir, config);
            monitoredDirs.put(folder.getPath(), folder);
        }
          
        return monitoredDirs.values();
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
}
