package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceTypeAlertDefinition
    extends AlertDefinition {

    @OneToOne
    @Index(name="ALERT_DEF_RES_TYPE_ID_IDX")
    private ResourceType resourceType;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL",nullable=false)
    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_TYPE_ALERTDEF_ACTIONS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "ACTION_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Action> actions = new ArrayList<Action>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RESOURCE_TYPE_ALERTDEF_CONDS", joinColumns = { @JoinColumn(name = "ALERTDEF_ID") }, inverseJoinColumns = { @JoinColumn(name = "COND_ID") })
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OrderBy("id")
    private Collection<AlertCondition> conditions = new ArrayList<AlertCondition>();

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

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public AlertDefinitionValue getAlertDefinitionValue() {
        //TODO set appdefId and appdefType?
        return super.getAlertDefinitionValue();
    }

    public Resource getResource() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    Collection<Action> getActionsBag() {
        return actions;
    }

    @Override
    Collection<AlertCondition> getConditionsBag() {
        return conditions;
    }
}
