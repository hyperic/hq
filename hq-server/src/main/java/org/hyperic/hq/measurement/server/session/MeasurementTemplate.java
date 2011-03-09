/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc. 
 * This file is part of HQ.         
 *  
 * HQ is free software; you can redistribute it and/or modify 
 * it under the terms version 2 of the GNU General Public License as 
 * published by the Free Software Foundation. This program is distributed 
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details. 
 *                
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 
 * USA. 
 */

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.units.FormattedNumber;

@Entity
@Table(name = "EAM_MEASUREMENT_TEMPL")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MeasurementTemplate implements ContainerManagedTimestampTrackable, Serializable {
    @Column(name = "ALIAS", nullable = false, length = 100)
    private String alias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    @Index(name = "TEMPL_CATEGORY_IDX")
    private Category category;

    @Column(name = "COLLECTION_TYPE", nullable = false)
    private int collectionType;

    @Column(name = "CTIME", nullable = false)
    private long creationTime;

    @Column(name = "DEFAULT_INTERVAL", nullable = false)
    private long defaultInterval = 60000l;

    @Column(name = "DEFAULT_ON", nullable = false)
    private boolean defaultOn = false;

    @Column(name = "DESIGNATE", nullable = false)
    @Index(name = "TEMPL_DESIG_IDX")
    private boolean designate = false;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MTIME", nullable = false)
    private long modifiedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MONITORABLE_TYPE_ID", nullable = false)
    @Index(name = "TEMPL_MONITORABLE_TYPE_ID_IDX")
    private MonitorableType monitorableType;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "PLUGIN", nullable = false, length = 250)
    private String plugin;

    @Column(name = "TEMPLATE", nullable = false, length = 2048)
    private String template;

    @Column(name = "UNITS", nullable = false, length = 50)
    private String units;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public MeasurementTemplate() {
    }

    public MeasurementTemplate(String name, String alias, String units, int collectionType,
                               boolean defaultOn, long defaultInterval, boolean designate,
                               String template, MonitorableType type, Category category,
                               String plugin) {
        this.name = name;
        this.alias = alias;
        this.units = units;
        this.collectionType = collectionType;
        this.defaultOn = defaultOn;
        this.defaultInterval = defaultInterval;
        this.designate = designate;
        this.template = template;
        monitorableType = type;
        this.category = category;
        this.plugin = plugin;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MeasurementTemplate)) {
            return false;
        }
        Integer objId = ((MeasurementTemplate) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    /**
     * Format a metric values, based on the unites specified by this template.
     */
    public String formatValue(double val) {
        FormattedNumber th = UnitsConvert.convert(val, getUnits());
        return th.toString();
    }

    /**
     * Format a metric value, based on the units specified by this template
     */
    public String formatValue(MetricValue val) {
        if (val == null)
            return "";

        return formatValue(val.getValue());
    }

    public String getAlias() {
        return alias;
    }

    public Category getCategory() {
        return category;
    }

    public int getCollectionType() {
        return collectionType;
    }

    public long getCtime() {
        return creationTime;
    }

    public long getDefaultInterval() {
        return defaultInterval;
    }

    public Integer getId() {
        return id;
    }

    public MonitorableType getMonitorableType() {
        return monitorableType;
    }

    public long getMtime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getTemplate() {
        return template;
    }

    public String getUnits() {
        return units;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isAvailability() {
        return getAlias().toUpperCase().equals(MeasurementConstants.CAT_AVAILABILITY);
    }

    public boolean isDefaultOn() {
        return defaultOn;
    }

    public boolean isDesignate() {
        return designate;
    }

    void setAlias(String alias) {
        this.alias = alias;
    }

    void setCategory(Category category) {
        this.category = category;
    }

    void setCollectionType(int collectionType) {
        this.collectionType = collectionType;
    }

    void setCtime(long ctime) {
        creationTime = ctime;
    }

    void setDefaultInterval(long defaultInterval) {
        this.defaultInterval = defaultInterval;
    }

    void setDefaultOn(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }

    void setDesignate(boolean designate) {
        this.designate = designate;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    void setMonitorableType(MonitorableType monitorableType) {
        this.monitorableType = monitorableType;
    }

    void setMtime(long mtime) {
        modifiedTime = mtime;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    void setTemplate(String template) {
        this.template = template;
    }

    void setUnits(String units) {
        this.units = units;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
