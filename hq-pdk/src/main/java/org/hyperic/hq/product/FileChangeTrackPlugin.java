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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.cm.filemonitor.IChangeListener;
import org.hyperic.cm.filemonitor.IFileMonitor;
import org.hyperic.cm.filemonitor.data.EventActionsEnum;
import org.hyperic.cm.filemonitor.data.EventMessage;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class FileChangeTrackPlugin extends ConfigFileTrackPlugin{
    
    private Collection<String> monitoredDirs = new ArrayList<String>();
     
    protected static Log log =
        LogFactory.getLog(FileChangeTrackPlugin.class.getName());


    private EventHandler eventHandler = null;
    
//    private String _appdefEntityID;
        
    protected  class EventHandler  implements IChangeListener{
        public void onChange(EventMessage eventMsg) {
            // check if the event belongs to this plugin
            
            final String pathToCheck = EventActionsEnum.RENAME.equals(eventMsg.getType()) 
                ? eventMsg.getOldFullPath() 
                : eventMsg.getFullPath();
                    
            if (pathToCheck == null){
                log.error("Unexpected null value path");
                return;
            }
            
            final String details;
            switch (eventMsg.getType()) {
                case CREATE:
                case DELETE:
                    if (eventMsg.getDiff() != null && eventMsg.getDiff().length() > 0)
                        details = eventMsg.getDiff();
                    else 
                        details = eventMsg.getFullPath();
                    break;
                case RENAME:
                    details =  eventMsg.getFullPath()+";"+eventMsg.getOldFullPath();
                    break;
                case MODIFY:
                    details = eventMsg.getDiff();
                    break;
                default:
                    details = "";
                    break;
            }
            
            TrackEvent event = 
                new TrackEvent(getName(),
                               System.currentTimeMillis(),
                               LogTrackPlugin.LOGLEVEL_INFO,
                               eventMsg.getType().protocolValue(),
                               details);
                                                  
            getManager().reportEvent(event);    
        }
    }

    /*** temporary implementation just so something should show up ***/
    protected String getDefaultConfigFile(TypeInfo info, ConfigResponse config) {
        final List<IMonitorConfig> configs = data.getMonitoredConfigs();
        if (configs == null || configs.size() <= 0)
            return "";

        final StringBuffer sb = new StringBuffer();
        for (final IMonitorConfig monitoredConf: configs){
            if (sb.length() > 0)
                sb.append(",");
            sb.append(monitoredConf.toString());
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
        final List<IMonitorConfig> configs = data.getMonitoredConfigs();
        if (configs == null || configs.size() <= 0)
            return;
        
        final String installpath =
            config.getValue(ProductPlugin.PROP_INSTALLPATH);

        final StringBuffer sb = new StringBuffer();
        sb.append("<templates><monitored>");
        for (final IMonitorConfig monitoredConf: configs){
            sb.append(monitoredConf.dumpXML());
        }
        sb.append("</monitored></templates>");
        
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (eventHandler == null){
            eventHandler = new EventHandler();
        }

        monitoredDirs = monitor.addMonitoredDirs(installpath, sb.toString(), eventHandler);
        
    }

    public void shutdown()
        throws PluginException {
        final IFileMonitor monitor = getManager().getFileMonitor();
        if (monitor !=null && monitoredDirs != null && monitoredDirs.size() > 0){
            monitor.removeMonitoredDirs(monitoredDirs);
            monitoredDirs.clear();
        }
        super.shutdown();
    }
    
}
