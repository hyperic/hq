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

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.units.FormattedNumber;

public class MeasurementTemplate 
    extends PersistedObject
    implements ContainerManagedTimestampTrackable, Serializable 
{
    private String  _name;
    private String  _alias;
    private String  _units;
    private int     _collectionType;
    private boolean _defaultOn = false;
    private long    _defaultInterval;
    private boolean _designate = false;
    private String  _template;
    private String  _plugin;
    private long    _ctime;
    private long    _mtime;

    private MonitorableType _monitorableType;
    private Category        _category;

    public MeasurementTemplate() {
    }

    public MeasurementTemplate(String name, String alias, String units,
                               int collectionType, boolean defaultOn,
                               long defaultInterval, boolean designate,
                               String template, MonitorableType type,
                               Category category, String plugin)
    {
        _name = name;
        _alias = alias;
        _units = units;
        _collectionType = collectionType;
        _defaultOn = defaultOn;
        _defaultInterval = defaultInterval;
        _designate = designate;
        _template = template;
        _monitorableType = type;
        _category = category;
        _plugin = plugin;
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

    public String getName() {
        return _name;
    }
    
    void setName(String name) {
        _name = name;
    }

    public String getAlias() {
        return _alias;
    }
    
    void setAlias(String alias) {
        _alias = alias;
    }

    public String getUnits() {
        return _units;
    }
    
    void setUnits(String units) {
        _units = units;
    }

    public int getCollectionType() {
        return _collectionType;
    }

    void setCollectionType(int collectionType) {
        _collectionType = collectionType;
    }

    public boolean isDefaultOn() {
        return _defaultOn;
    }
    
    void setDefaultOn(boolean defaultOn) {
        _defaultOn = defaultOn;
    }

    public long getDefaultInterval() {
        return _defaultInterval;
    }
    
    void setDefaultInterval(long defaultInterval) {
        _defaultInterval = defaultInterval;
    }

    public boolean isDesignate() {
        return _designate;
    }
    
    void setDesignate(boolean designate) {
        _designate = designate;
    }

    public String getTemplate() {
        return _template;
    }
    
    void setTemplate(String template) {
        _template = template;
    }

    public String getPlugin() {
        return _plugin;
    }
    
    void setPlugin(String plugin) {
        _plugin = plugin;
    }

    public long getCtime() {
        return _ctime;
    }
    
    void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getMtime() {
        return _mtime;
    }
    
    void setMtime(long mtime) {
        _mtime = mtime;
    }

    public MonitorableType getMonitorableType() {
        return _monitorableType;
    }
    
    void setMonitorableType(MonitorableType monitorableType) {
        _monitorableType = monitorableType;
    }

    public Category getCategory() {
        return _category;
    }
    
    void setCategory(Category category) {
        _category = category;
    }

    /**
     * Format a metric value, based on the units specified by this template
     */
    public String formatValue(MetricValue val) {
        if (val == null)
            return "";
        
        FormattedNumber th = UnitsConvert.convert(val.getValue(), getUnits());
        return th.toString();
    }
}
