package org.hyperic.hq.api.model;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "metricTemplate", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MetricTemplateType")
public class MetricTemplate {

 @XmlAttribute(name = "interval", required = true)
 protected long defaultInterval;
 @XmlAttribute(name = "category", required = true)
 protected String category;
 @XmlAttribute(name = "collectionType", required = true)
 protected String collectionType;
 @XmlAttribute(name = "enabled", required = true)
 protected boolean defaultOn;
 @XmlAttribute(name = "units", required = true)
 protected String units;
 @XmlAttribute(name = "alias", required = true)
 protected String alias;
 @XmlAttribute(name = "name", required = true)
 protected String name;
 @XmlAttribute(name = "id", required = true)
 protected int id;

 /**
  * Gets the value of the defaultInterval property.
  * 
  */
 public long getInterval() {
     return defaultInterval;
 }

 /**
  * Sets the value of the defaultInterval property.
  * 
  */
 public void setInterval(long value) {
     this.defaultInterval = value;
 }

 /**
  * Gets the value of the category property.
  * 
  * @return
  *     possible object is
  *     {@link String }
  *     
  */
 public String getCategory() {
     return category;
 }

 /**
  * Sets the value of the category property.
  * 
  * @param value
  *     allowed object is
  *     {@link String }
  *     
  */
 public void setCategory(String value) {
     this.category = value;
 }

 /**
  * Gets the value of the collectionType property.
  * 
  * @return
  *     possible object is
  *     {@link String }
  *     
  */
 public String getCollectionType() {
     return collectionType;
 }

 /**
  * Sets the value of the collectionType property.
  * 
  * @param value
  *     allowed object is
  *     {@link String }
  *     
  */
 public void setCollectionType(String value) {
     this.collectionType = value;
 }

 /**
  * Gets the value of the defaultOn property.
  * 
  */
 public boolean isEnabled() {
     return defaultOn;
 }

 /**
  * Sets the value of the defaultOn property.
  * 
  */
 public void setEnabled(boolean value) {
     this.defaultOn = value;
 }

 /**
  * Gets the value of the units property.
  * 
  * @return
  *     possible object is
  *     {@link String }
  *     
  */
 public String getUnits() {
     return units;
 }

 /**
  * Sets the value of the units property.
  * 
  * @param value
  *     allowed object is
  *     {@link String }
  *     
  */
 public void setUnits(String value) {
     this.units = value;
 }

 /**
  * Gets the value of the alias property.
  * 
  * @return
  *     possible object is
  *     {@link String }
  *     
  */
 public String getAlias() {
     return alias;
 }

 /**
  * Sets the value of the alias property.
  * 
  * @param value
  *     allowed object is
  *     {@link String }
  *     
  */
 public void setAlias(String value) {
     this.alias = value;
 }

 /**
  * Gets the value of the name property.
  * 
  * @return
  *     possible object is
  *     {@link String }
  *     
  */
 public String getName() {
     return name;
 }

 /**
  * Sets the value of the name property.
  * 
  * @param value
  *     allowed object is
  *     {@link String }
  *     
  */
 public void setName(String value) {
     this.name = value;
 }

 /**
  * Gets the value of the id property.
  * 
  */
 public int getId() {
     return id;
 }

 /**
  * Sets the value of the id property.
  * 
  */
 public void setId(int value) {
     this.id = value;
 }

}

