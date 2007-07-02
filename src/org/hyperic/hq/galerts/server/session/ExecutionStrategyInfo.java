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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.util.config.ConfigResponse;

public class ExecutionStrategyInfo 
    extends PersistedObject
{ 
    private GalertDef                 _alertDef;
    private ExecutionStrategyTypeInfo _type;
    private Crispo                    _config;
    private GalertDefPartition        _partition;
    private List                      _triggers = new ArrayList();
    
    protected ExecutionStrategyInfo() {}

    ExecutionStrategyInfo(GalertDef alertDef, ExecutionStrategyTypeInfo type, 
                          Crispo config, GalertDefPartition partition) 
    {
        _alertDef  = alertDef;
        _type      = type;
        _config    = config;
        _partition = partition;
    }
    
    public GalertDef getAlertDef() {
        return _alertDef;
    }
    
    protected void setAlertDef(GalertDef def) {
        _alertDef = def;
    }
    
    public ExecutionStrategyTypeInfo getType() {
        return _type;
    }
    
    protected void setType(ExecutionStrategyTypeInfo type) {
        _type = type;
    }
    
    protected Crispo getConfigCrispo() {
        return _config;
    }
    
    protected void setConfigCrispo(Crispo config) {
        _config = config;
    }
    
    public ConfigResponse getConfig() {
        return _config.toResponse();
    }
    
    public ExecutionStrategy getStrategy() {
        return _type.getStrategy(_config.toResponse());
    }

    public GalertDefPartition getPartition() {
        return _partition;
    }
    
    protected int getPartitionEnum() {
        return _partition.getCode();
    }
    
    protected void setPartitionEnum(int partition) {
        _partition = GalertDefPartition.findByCode(partition);
    }
    
    
    public List getTriggers() {
        return Collections.unmodifiableList(_triggers);
    }
    
    protected List getTriggerList() {
        return _triggers;
    }
    
    GtriggerInfo addTrigger(GtriggerTypeInfo typeInfo, Crispo config, 
                            ResourceGroup group, GalertDefPartition style) 
    {
        GtriggerInfo trigger = new GtriggerInfo(typeInfo, this, config,
                                                getTriggerList().size());

        
        // Ensure that the trigger can process the config.  If it can't then
        // it'll probably throw an exception
        // TODO:  Would be good to actually validate against the schema
        GtriggerType type = typeInfo.getType();

        if (!type.validForGroup(group)) {
            throw new IllegalArgumentException("Trigger [" + 
                                               type.getClass().getName() + 
                                               "] cannot work with this " + 
                                               "group [" + group + "]");
        }
        
        ConfigResponse configResponse = config.toResponse();
        
        type.createTrigger(configResponse);
        
        getTriggerList().add(trigger);
        return trigger;
    }
    
    void clearTriggers() {
        getTriggerList().clear();
    }
    
    protected void setTriggerList(List triggers) {
        _triggers = triggers;
    }
    
    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getAlertDef().hashCode();
        hash = hash * 31 + getType().hashCode();
        hash = hash * 31 + getPartition().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof ExecutionStrategyInfo == false)
            return false;
        
        ExecutionStrategyInfo oe = (ExecutionStrategyInfo)o;
        
        return oe.getAlertDef().equals(getAlertDef()) &&
            oe.getType().equals(getType()) &&
            oe.getPartition().equals(getPartition());
    }
}
