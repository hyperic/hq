package com.vmware.hyperic.model.relations;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Represents relation between two resources. Source resource is the parent element. Target resource is identified by
 * child resource element
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relation", propOrder = { "resource" })
public class Relation implements Serializable {
    private static final long serialVersionUID = 8844058749750721569L;

    @XmlElement(required = true)
    protected Resource resource;
    @XmlAttribute(name = "type", required = true)
    protected RelationType type;
    @XmlAttribute(name = "createIfNotExist")
    protected Boolean createIfNotExist;

    /**
     * Gets the value of the resource property.
     * 
     * @return possible object is {@link Resource }
     * 
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Sets the value of the resource property.
     * 
     * @param value allowed object is {@link Resource }
     * 
     */
    public void setResource(Resource value) {
        this.resource = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return possible object is {@link RelationType }
     * 
     */
    public RelationType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value allowed object is {@link RelationType }
     * 
     */
    public void setType(RelationType value) {
        this.type = value;
    }

    /**
     * Gets the value of the createIfNotExist property.
     * 
     * @return possible object is {@link Boolean }
     * 
     */
    public boolean isCreateIfNotExist() {
        if (createIfNotExist == null) {
            return false;
        } else {
            return createIfNotExist;
        }
    }

    /**
     * Sets the value of the createIfNotExist property.
     * 
     * @param value allowed object is {@link Boolean }
     * 
     */
    public void setCreateIfNotExist(Boolean value) {
        this.createIfNotExist = value;
    }

}
