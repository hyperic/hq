package com.vmware.hyperic.model.relations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a resource
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resource", propOrder = { "identifiers", "relations" })
public class Resource implements Serializable {
    private static final long serialVersionUID = 5213260486980310748L;

    @XmlElement(name = "identifier")
    protected Collection<Identifier> identifiers;
    @XmlElement(name = "relation")
    protected Collection<Relation> relations;

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "type")
    protected String type;
    @XmlAttribute(name = "tier")
    protected ResourceTier tier;
    @XmlAttribute(name = "subType")
    protected ResourceSubType subType;
    @XmlAttribute(name = "createIfNotExist")
    protected Boolean createIfNotExist;

    /**
     * Gets the value of the identifiers property.
     * 
     * @return collection of {@link Identifier } objects
     * 
     */
    public Collection<Identifier> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new ArrayList<Identifier>();
        }
        return identifiers;
    }

    /**
     * Sets the value of the identifiers property.
     * 
     * @param value allowed object is a Collection of {@link Identifier } objects
     * 
     */
    public void setIdentifiers(Collection<Identifier> value) {
        this.identifiers = value;
    }

    /**
     * Gets the value of the relations property.
     * 
     * @return possible object is {@link Relation } objects
     * 
     */
    public Collection<Relation> getRelations() {
        if (relations == null) {
            relations = new ArrayList<Relation>();
        }
        return relations;
    }

    /**
     * Sets the value of the relations property.
     * 
     * @param value allowed object is {@link Relation } objects
     * 
     */
    public void setRelations(Collection<Relation> value) {
        this.relations = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the tier property.
     * 
     * @return possible object is {@link ResourceTier }
     * 
     */
    public ResourceTier getTier() {
        if (tier == null) {
            return ResourceTier.SERVER;
        } else {
            return tier;
        }
    }

    /**
     * Sets the value of the tier property.
     * 
     * @param value allowed object is {@link ResourceTier }
     * 
     */
    public void setTier(ResourceTier value) {
        this.tier = value;
    }

    /**
     * Gets the value of the subType property.
     * 
     * @return possible object is {@link ResourceSubType }
     * 
     */
    public ResourceSubType getSubType() {
        return subType;
    }

    /**
     * Sets the value of the subType property.
     * 
     * @param value allowed object is {@link ResourceSubType }
     * 
     */
    public void setSubType(ResourceSubType value) {
        this.subType = value;
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
