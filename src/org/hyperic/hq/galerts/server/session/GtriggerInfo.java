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
