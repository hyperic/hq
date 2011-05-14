package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hyperic.hq.events.shared.AlertDefinitionValue;

@PrimaryKeyJoinColumn(name = "DEF_ID", referencedColumnName = "ID")
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceTypeAlertDefinition
    extends AlertDefinition {

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_TYPE_ALERTDEF_ACTIONS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "ACTION_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Action> actions = new ArrayList<Action>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_TYPE_ALERTDEF_CONDS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "COND_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OrderBy("id")
    private Collection<AlertCondition> conditions = new ArrayList<AlertCondition>();

    @Column
    @Index(name = "ALERT_DEF_RES_TYPE_ID_IDX")
    private Integer resourceType;
    
    @OneToMany(mappedBy = "resourceTypeAlertDefinition")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<ResourceAlertDefinition> resourceAlertDefs = new ArrayList<ResourceAlertDefinition>();

    @Override
    Collection<Action> getActionsBag() {
        return actions;
    }

    @Override
    public AlertDefinitionValue getAlertDefinitionValue() {
        // TODO set appdefId and appdefType?
        return super.getAlertDefinitionValue();
    }

    @Override
    Collection<AlertCondition> getConditionsBag() {
        return conditions;
    }

    public Collection<ResourceAlertDefinition> getResourceAlertDefs() {
        return resourceAlertDefs;
    }

    public Integer getResource() {
        // TODO this method should be moved from AlertDefinition to ResourceAlertDef.  N/A here.
        throw new UnsupportedOperationException();
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

}
