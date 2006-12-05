package org.hyperic.hq.galerts.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.util.config.ConfigResponse;

public class ExecutionStrategyTypeInfo 
    extends PersistedObject
{
    private Class _typeClass;
    
    protected ExecutionStrategyTypeInfo() {}

    ExecutionStrategyTypeInfo(ExecutionStrategyType stratType) {
        _typeClass = stratType.getClass();
    }
    
    public Class getTypeClass() {
        return _typeClass;
    }
    
    protected void setTypeClass(Class typeClass) {
        _typeClass = typeClass;
    }
    
    ExecutionStrategyInfo createStrategyInfo(GalertDef def, Crispo config,
                                             GalertDefPartition partition) 
    {
        return new ExecutionStrategyInfo(def, this, config, partition);
    }
    
    public ExecutionStrategyType getType() {
        try {
            return (ExecutionStrategyType)_typeClass.newInstance();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public ExecutionStrategy getStrategy(ConfigResponse config) {
        return getType().createStrategy(config);
    }
}
