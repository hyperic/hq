package org.hyperic.hq.galerts.server.session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.server.session.Escalation;

public class GalertDef 
    extends PersistedObject
{ 
    private String        _name;
    private String        _desc;
    private AlertSeverity _severity; 
    private boolean       _enabled;
    private ResourceGroup _group;
    private Escalation    _escalation;
    private Set           _strategies = new HashSet();
    
    protected GalertDef() {}
    
    GalertDef(String name, String desc, AlertSeverity severity, boolean enabled,
              ResourceGroup group) 
    {
        _name       = name;
        _desc       = desc;
        _severity   = severity;
        _enabled    = enabled;
        _group      = group;
        _escalation = null;
    }
        
    ExecutionStrategyInfo 
        addPartition(GalertDefPartition partition, 
                     ExecutionStrategyTypeInfo stratType, Crispo stratConfig) 
    {
        ExecutionStrategyInfo strat;
            
        if (findStrategyByPartition(partition) != null) {
            throw new IllegalStateException("Partition[" + partition + "] " +
                                            "already created");
        }

        strat = stratType.createStrategyInfo(this, stratConfig, partition);
        getStrategySet().add(strat);
        return strat;
    }

    private ExecutionStrategyInfo findStrategyByPartition(GalertDefPartition p){
        for (Iterator i=getStrategies().iterator(); i.hasNext(); ) {
            ExecutionStrategyInfo strat = (ExecutionStrategyInfo)i.next();
            
            if (strat.getPartition().equals(p))
                return strat;
        }
        return null;
    }
    
    public String getName() {
        return _name;
    }
    
    protected void setName(String name){
        _name = name;
    }
    
    public String getDescription() {
        return _desc;
    }
    
    protected void setDescription(String desc) {
        _desc = desc;
    }
    
    public AlertSeverity getSeverity() {
        return _severity;
    }
    
    protected int getSeverityEnum() {
        return _severity.getCode();
    }
    
    public boolean isEnabled() {
        return _enabled;
    }
    
    protected void setEnabled(boolean enabled) {
        _enabled = enabled;
    }
    
    protected void setSeverityEnum(int code) {
        _severity = AlertSeverity.findByCode(code);
    }
    
    public ResourceGroup getGroup() {
        return _group;
    }
    
    protected void setGroup(ResourceGroup group) {
        _group = group;
    }
    
    public Escalation getEscalation() {
        return _escalation;
    }
    
    public void setEscalation(Escalation escalation) {
        _escalation = escalation;
    }
    
    protected Set getStrategySet() {
        return _strategies;
    }
    
    protected void setStrategySet(Set strategies) {
        _strategies = strategies;
    }
    
    public Set getStrategies() {
        return Collections.unmodifiableSet(_strategies);
    }
    
    public ExecutionStrategyInfo getStrategy(GalertDefPartition partition) {
        for (Iterator i=getStrategies().iterator(); i.hasNext(); ) {
            ExecutionStrategyInfo s = (ExecutionStrategyInfo)i.next();
            
            if (s.getPartition().equals(partition))
                return s;
        }
        return null;
    }

    /**
     * Return GalertDef like a "value" object, parallel to existing API.
     * This guarantees that the pojo has been loaded
     * @return this with the values loaded
     */
    GalertDef getGalertDefValue() {
        Hibernate.initialize(this);
        return this;
    }
}
