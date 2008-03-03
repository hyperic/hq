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

package org.hyperic.hq.galerts.server.session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;

public class GalertDef 
    extends PersistedObject 
    implements AlertDefinitionInterface, PerformsEscalations
{ 
    private String        _name;
    private String        _desc;
    private AlertSeverity _severity; 
    private boolean       _enabled;
    private ResourceGroup _group;
    private Escalation    _escalation;
    private Set           _strategies = new HashSet();
    private long          _ctime;
    private long          _mtime;
    private boolean       _deleted;
    private Long          _lastFired;
    
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
        _ctime      = System.currentTimeMillis();
        _mtime      = _ctime;
        _deleted    = false;
        _lastFired  = null;
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
    
    public boolean isEnabled() {
        return _enabled;
    }
    
    protected void setEnabled(boolean enabled) {
        _enabled = enabled;
    }
    
    protected int getSeverityEnum() {
        return _severity.getCode();
    }
    
    protected void setSeverityEnum(int code) {
        _severity = AlertSeverity.findByCode(code);
    }
    
    public AlertSeverity getSeverity() {
        return _severity;
    }

    void setSeverity(AlertSeverity severity) {
        setSeverityEnum(severity.getCode());
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
    
    protected void setEscalation(Escalation escalation) {
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

    public AppdefEntityID getAppdefID() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }
    
    public int getAppdefId() {
        return getGroup().getId().intValue();
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_GROUP;
    }

    public int getPriority() {
        return getSeverity().getCode();
    }

    public boolean isNotifyFiltered() {
        return false;
    }

    public long getCtime() {
        return _ctime;
    }

    protected void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getMtime() {
        return _mtime;
    }

    protected void setMtime(long mtime) {
        _mtime = mtime;
    }
    
    protected void setDeleted(boolean deleted) {
        _deleted = deleted;
    }
    
    public boolean isDeleted() {
        return _deleted;
    }
    
    protected void setLastFired(Long l) {
        _lastFired = l;
    }
    
    public Long getLastFired() {
        return _lastFired;
    }

    public EscalationAlertType getAlertType() {
        return GalertEscalationAlertType.GALERT;
    }

    public AlertDefinitionInterface getDefinitionInfo() {
        return this;
    }
    
    public boolean performsEscalations() {
        return true;
    }
    
    public String toString() {
        return getName();
    }

    public Resource getResource() {
        return getGroup().getResource();
    }
}
