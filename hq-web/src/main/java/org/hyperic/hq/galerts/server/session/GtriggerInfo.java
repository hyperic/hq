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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.util.config.ConfigResponse;


public class GtriggerInfo 
    extends PersistedObject
{
    private GtriggerTypeInfo      _typeInfo;
    private ExecutionStrategyInfo _strategy;
    private Crispo                _config;
    private int                   _listIndex;
    
    protected GtriggerInfo() {}

    GtriggerInfo(GtriggerTypeInfo typeInfo, ExecutionStrategyInfo strategy, 
                 Crispo config, int listIndex) 
    {
        _typeInfo  = typeInfo;
        _strategy  = strategy;
        _config    = config;
        _listIndex = listIndex;
    }
    
    protected GtriggerTypeInfo getTypeInfo() {
        return _typeInfo;
    }
    
    protected void setTypeInfo(GtriggerTypeInfo typeInfo) {
        _typeInfo = typeInfo;
    }
    
    public Gtrigger getTrigger() {
        return _typeInfo.getType().createTrigger(getConfig());
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
    
    public ExecutionStrategyInfo getStrategy() {
        return _strategy;
    }
    
    protected void setStrategy(ExecutionStrategyInfo strategy) {
        _strategy = strategy;
    }
    
    protected void setListIndex(int listIndex) {
        _listIndex = listIndex;
    }
    
    protected int getListIndex() {
        return _listIndex;
    }
    
    /**
     * Return GtriggerInfo like a "value" object, parallel to existing
     * API.  This guarantees that the pojo values have been loaded.
     * @return this with the values loaded
     */
    GtriggerInfo getGtriggerInfoValue() {
        getConfig();
        getStrategy();
        getTypeInfo();
        return this;
    }
}
