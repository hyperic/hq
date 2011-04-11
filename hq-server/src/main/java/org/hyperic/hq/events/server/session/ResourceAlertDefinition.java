package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;

@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceAlertDefinition
    extends AlertDefinition {

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_ALERTDEF_ACTIONS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "ACTION_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Action> actionsBag = new ArrayList<Action>();

    @OneToOne(mappedBy = "alertDefinition",cascade=CascadeType.ALL)
    private AlertDefinitionState alertDefinitionState;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_ALERTDEF_CONDS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "COND_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OrderBy("id")
    private Collection<AlertCondition> conditionsBag = new ArrayList<AlertCondition>();

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID", nullable = false)
    private Integer id;

    @Column(name = "RESOURCE_ID")
    @Index(name = "ALERT_DEF_RES_ID_IDX")
    private Integer resource;

    @ManyToOne
    private ResourceTypeAlertDefinition resourceTypeAlertDefinition;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "alertDefinition")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<RegisteredTrigger> triggersBag = new ArrayList<RegisteredTrigger>();

    @Column(name = "VERSION_COL")
    @Version
    private Long version;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceAlertDefinition other = (ResourceAlertDefinition) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    Collection<Action> getActionsBag() {
        return actionsBag;
    }

    public AlertDefinitionState getAlertDefinitionState() {
        return alertDefinitionState;
    }

    public AlertDefinitionValue getAlertDefinitionValue() {
        AlertDefinitionValue value = super.getAlertDefinitionValue();
        AppdefEntityID appdefId = AppdefUtil.newAppdefEntityId(resource);
        value.setAppdefId(appdefId.getId());
        value.setAppdefType(appdefId.getType());
        value.removeAllTriggers();
        for (RegisteredTrigger t : getTriggers()) {
            value.addTrigger(t.getRegisteredTriggerValue());
        }
        value.cleanTrigger();
        if(resourceTypeAlertDefinition != null) {
            value.setParentId(resourceTypeAlertDefinition.getId());
        }
        return value;
    }

    public Collection<AlertCondition> getConditionsBag() {
        return conditionsBag;
    }

    public Integer getId() {
        return id;
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

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
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

    public void setId(Integer id) {
        this.id = id;
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

    public void setVersion(Long version) {
        this.version = version;
    }

}
