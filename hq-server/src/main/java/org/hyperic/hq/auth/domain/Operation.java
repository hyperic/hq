package org.hyperic.hq.auth.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;

@Table(name = "EAM_OPERATION")
@Entity
public class Operation implements Serializable {

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_OPERATION_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "RESOURCE_TYPE_ID")
    @Index(name = "OP_RES_TYPE_ID_IDX")
    private Integer resourceType;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj instanceof Operation == false)
            return false;

        Operation o = (Operation) obj;
        return getName().equals(o.getName());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getName().hashCode();
        return result;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
