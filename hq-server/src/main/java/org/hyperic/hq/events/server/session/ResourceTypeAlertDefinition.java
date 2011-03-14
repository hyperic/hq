package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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

@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
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

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID", nullable = false)
    private Integer id;

    @OneToOne
    @Index(name = "ALERT_DEF_RES_TYPE_ID_IDX")
    private ResourceType resourceType;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceTypeAlertDefinition other = (ResourceTypeAlertDefinition) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

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

    public Integer getId() {
        return id;
    }

    public Resource getResource() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public ResourceType getResourceType() {
        return resourceType;
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

    public void setId(Integer id) {
        this.id = id;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
