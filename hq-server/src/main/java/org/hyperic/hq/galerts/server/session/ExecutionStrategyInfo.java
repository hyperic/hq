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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.util.config.ConfigResponse;

@Entity
@Table(name="EAM_EXEC_STRATEGIES")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ExecutionStrategyInfo implements Serializable
{ 
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="DEF_ID",nullable=false)
    @Index(name="EXEC_STRATEGIES_DEF_ID_IDX")
    private GalertDef                 alertDef;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TYPE_ID",nullable=false)
    @Index(name="EXEC_STRATEGIES_TYPE_ID_IDX")
    private ExecutionStrategyTypeInfo type;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CONFIG_ID",nullable=false)
    @Index(name="EXEC_STRATEGIES_CONFIG_ID_IDX")
    private Crispo                    config;
    
    @SuppressWarnings("unused")
    @Column(name="PARTITION",nullable=false)
    private int partitionEnum;
    
    private transient GalertDefPartition        partition;
    
    @OneToMany(mappedBy="strategy",cascade=CascadeType.ALL,orphanRemoval=true)
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    @OrderColumn(name="LIDX",nullable=false)
    private List<GtriggerInfo>        triggers = new ArrayList<GtriggerInfo>();
    
    protected ExecutionStrategyInfo() {}

    ExecutionStrategyInfo(GalertDef alertDef, ExecutionStrategyTypeInfo type, 
                          Crispo config, GalertDefPartition partition) 
    {
        this.alertDef  = alertDef;
        this.type      = type;
        this.config    = config;
        this.partition = partition;
    }
    
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public GalertDef getAlertDef() {
        return alertDef;
    }
    
    protected void setAlertDef(GalertDef def) {
        alertDef = def;
    }
    
    public ExecutionStrategyTypeInfo getType() {
        return type;
    }
    
    protected void setType(ExecutionStrategyTypeInfo type) {
        this.type = type;
    }
    
    protected Crispo getConfigCrispo() {
        return config;
    }
    
    protected void setConfigCrispo(Crispo config) {
        this.config = config;
    }
    
    public ConfigResponse getConfig() {
        return config.toResponse();
    }
    
    public ExecutionStrategy getStrategy() {
        return type.getStrategy(config.toResponse());
    }

    public GalertDefPartition getPartition() {
        return partition;
    }
    
    protected int getPartitionEnum() {
        return partition.getCode();
    }
    
    protected void setPartitionEnum(int partition) {
        this.partition = GalertDefPartition.findByCode(partition);
    }
    
    
    public List<GtriggerInfo> getTriggers() {
        return Collections.unmodifiableList(triggers);
    }
    
    protected List<GtriggerInfo> getTriggerList() {
        return triggers;
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
    
    protected void setTriggerList(List<GtriggerInfo> triggers) {
        this.triggers = triggers;
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
