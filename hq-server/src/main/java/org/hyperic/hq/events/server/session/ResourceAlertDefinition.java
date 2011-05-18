package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hq.appdef.shared.AppdefConverter;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.AlertDefinitionValue;

@PrimaryKeyJoinColumn(name = "DEF_ID", referencedColumnName = "ID")
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="RESOURCE_ALERT_DEFINITION")
public class ResourceAlertDefinition
    extends AlertDefinition {

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_ALERTDEF_ACTIONS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "ACTION_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Action> actionsBag = new ArrayList<Action>();

    @OneToOne(mappedBy = "alertDefinition", cascade = CascadeType.ALL)
    private AlertDefinitionState alertDefinitionState;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_ALERTDEF_CONDS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "COND_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OrderBy("id")
    private Collection<AlertCondition> conditionsBag = new ArrayList<AlertCondition>();

    @Column(name = "RESOURCE_ID")
    @Index(name = "ALERT_DEF_RES_ID_IDX")
    private Integer resource;

    @ManyToOne
    private ResourceTypeAlertDefinition resourceTypeAlertDefinition;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "alertDefinition")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<RegisteredTrigger> triggersBag = new ArrayList<RegisteredTrigger>();

    void addTrigger(RegisteredTrigger t) {
        triggersBag.add(t);
    }

    void clearTriggers() {
        for (AlertCondition cond : getConditions()) {
            cond.setTrigger(null);
        }
        // the triggersBag parent-child relationship is set to
        // cascade="all-delete-orphan", so clearing the triggers
        // collection will also delete the triggers from the db
        triggersBag.clear();
    }

    Collection<Action> getActionsBag() {
        return actionsBag;
    }

    public AlertDefinitionState getAlertDefinitionState() {
        return alertDefinitionState;
    }

    public AlertDefinitionValue getAlertDefinitionValue() {
        AlertDefinitionValue value = super.getAlertDefinitionValue();
        AppdefEntityID appdefId = Bootstrap.getBean(AppdefConverter.class).newAppdefEntityId(
            resource);
        value.setAppdefId(appdefId.getId());
        value.setAppdefType(appdefId.getType());
        value.removeAllTriggers();
        for (RegisteredTrigger t : getTriggers()) {
            value.addTrigger(t.getRegisteredTriggerValue());
        }
        value.cleanTrigger();
        if (resourceTypeAlertDefinition != null) {
            value.setParentId(resourceTypeAlertDefinition.getId());
        }
        return value;
    }

    public Collection<AlertCondition> getConditionsBag() {
        return conditionsBag;
    }

    /**
     * Get the time that the alert definition last fired.
     */
    public long getLastFired() {
        return getAlertDefinitionState().getLastFired();
    }

    public Integer getResource() {
        return resource;
    }

    public ResourceTypeAlertDefinition getResourceTypeAlertDefinition() {
        return resourceTypeAlertDefinition;
    }

    public Collection<RegisteredTrigger> getTriggers() {
        return Collections.unmodifiableCollection(triggersBag);
    }

    Collection<RegisteredTrigger> getTriggersBag() {
        return triggersBag;
    }

    void removeTrigger(RegisteredTrigger t) {
        triggersBag.remove(t);
    }

    void setActionsBag(Collection<Action> actions) {
        actionsBag = actions;
    }

    public void setAlertDefinitionState(AlertDefinitionState state) {
        this.alertDefinitionState = state;
    }

    void setConditionsBag(Collection<AlertCondition> conditions) {
        conditionsBag = conditions;
    }

    void setLastFired(long lastFired) {
        getAlertDefinitionState().setLastFired(lastFired);
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    public void setResourceTypeAlertDefinition(ResourceTypeAlertDefinition resourceTypeAlertDefinition) {
        this.resourceTypeAlertDefinition = resourceTypeAlertDefinition;
    }

    void setTriggersBag(Collection<RegisteredTrigger> triggers) {
        triggersBag = triggers;
    }

}
