package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceAlertDefinition extends AlertDefinition {
    
    @ManyToOne
    @JoinColumn(name="RESOURCE_ID")
    @Index(name="ALERT_DEF_RES_ID_IDX")
    private Resource resource;

    @OneToMany(cascade = CascadeType.ALL,orphanRemoval=true,mappedBy="alertDef")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<RegisteredTrigger> triggersBag = new ArrayList<RegisteredTrigger>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name="RESOURCE_ALERTDEF_ACTIONS",joinColumns = {@JoinColumn(name="ALERTDEF_ID")},
    inverseJoinColumns = {@JoinColumn(name="ACTION_ID")})
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Action> actionsBag = new ArrayList<Action>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name="RESOURCE_ALERTDEF_CONDS",joinColumns = {@JoinColumn(name="ALERTDEF_ID")},
    inverseJoinColumns = {@JoinColumn(name="COND_ID")})
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OrderBy("id")
    private Collection<AlertCondition> conditionsBag = new ArrayList<AlertCondition>();

    @OneToOne(mappedBy="alertDefinition")
    private AlertDefinitionState alertDefinitionState;

    @ManyToOne
    private ResourceTypeAlertDefinition resourceTypeAlertDefinition;
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID",nullable=false)
    private Integer id;

    @Column(name = "VERSION_COL")
    @Version
    private Long version;
    
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

    public Collection<RegisteredTrigger> getTriggers() {
        return Collections.unmodifiableCollection(triggersBag);
    }

    Collection getTriggersBag() {
        return triggersBag;
    }

    void setTriggersBag(Collection triggers) {
        triggersBag = triggers;
    }

    void clearTriggers() {
        for (Iterator it = getConditions().iterator(); it.hasNext();) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setTrigger(null);
        }
        // the triggersBag parent-child relationship is set to
        // cascade="all-delete-orphan", so clearing the triggers
        // collection will also delete the triggers from the db
        triggersBag.clear();
    }

    void addTrigger(RegisteredTrigger t) {
        triggersBag.add(t);
    }

    void removeTrigger(RegisteredTrigger t) {
        triggersBag.remove(t);
    }

    public Resource getResource() {
        return resource;
    }

    void setResource(Resource resource) {
        this.resource = resource;
    }

    public ResourceTypeAlertDefinition getResourceTypeAlertDefinition() {
        return resourceTypeAlertDefinition;
    }

    public void setResourceTypeAlertDefinition(ResourceTypeAlertDefinition resourceTypeAlertDefinition) {
        this.resourceTypeAlertDefinition = resourceTypeAlertDefinition;
    }

    // TODO really need either of these?
    public Integer getAppdefId() {
        return getResource().getId();
    }

    public int getAppdefType() {
        String rtName = getResource().getType().getName();
        AppdefEntityID resourceId = AppdefUtil.newAppdefEntityId(getResource());

        if (resourceId.isPlatform()) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (resourceId.isServer()) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (resourceId.isService()) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else if (resourceId.isApplication()) {
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        } else if (resourceId.isGroup()) {
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;
        } else {
            throw new IllegalArgumentException(rtName + " is not a valid Appdef Resource Type");
        }
    }

    public AppdefEntityID getAppdefEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }

    public AlertDefinitionValue getAlertDefinitionValue() {
        AlertDefinitionValue value = super.getAlertDefinitionValue();
        value.setAppdefId(getAppdefId());
        value.setAppdefType(getAppdefType());
        value.removeAllTriggers();
        for (Iterator i = getTriggers().iterator(); i.hasNext();) {
            RegisteredTrigger t = (RegisteredTrigger) i.next();
            value.addTrigger(t.getRegisteredTriggerValue());
        }
        value.cleanTrigger();
        value.setParentId(resourceTypeAlertDefinition.getId());
        return value;
    }

    Collection<Action> getActionsBag() {
        return actionsBag;
    }

    void setActionsBag(Collection<Action> actions) {
        actionsBag = actions;
    }
    
    Collection<AlertCondition> getConditionsBag() {
        return conditionsBag;
    }

    void setConditionsBag(Collection conditions) {
        conditionsBag = conditions;
    }

    public AlertDefinitionState getAlertDefinitionState() {
        return alertDefinitionState;
    }

    void setAlertDefinitionState(AlertDefinitionState state) {
        this.alertDefinitionState = state;
    }

    /**
     * Get the time that the alert definition last fired.
     */
    public long getLastFired() {
        return getAlertDefinitionState().getLastFired();
    }

    void setLastFired(long lastFired) {
        getAlertDefinitionState().setLastFired(lastFired);
    }
}
