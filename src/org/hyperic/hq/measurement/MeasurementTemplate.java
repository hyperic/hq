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

package org.hyperic.hq.measurement;

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;

public class MeasurementTemplate extends PersistedObject
    implements java.io.Serializable {

    // Fields
    private Integer _cid;
    private String _name;
    private String _alias;
    private String _units;
    private Integer _collectionType;
    private boolean _defaultOn;
    private long _defaultInterval;
    private boolean _designate;
    private String _template;
    private String _plugin;
    private byte[] _expressionData;
    private long _ctime;
    private long _mtime;
    private MonitorableType _monitorableType;
    private Category _category;
    private Collection _measurements;
    private Collection _measurementArgs;

    // Constructors
    public MeasurementTemplate() {
    }

    public MeasurementTemplate(String name, String alias,
                               Integer collectionType, boolean defaultOn,
                               long defaultInterval, boolean designate,
                               String template, long ctime, long mtime) {
        _name = name;
        _alias = alias;
        _collectionType = collectionType;
        _defaultOn = defaultOn;
        _defaultInterval = defaultInterval;
        _designate = designate;
        _template = template;
        _ctime = ctime;
        _mtime = mtime;
    }

    public MeasurementTemplate(String name, String alias,
                               String units, Integer collectionType, 
                               boolean defaultOn, long defaultInterval, 
                               boolean designate, String template,
                               String plugin, byte[] expressionData, 
                               long ctime, long mtime, 
                               MonitorableType monitorableType, 
                               Category category, 
                               Collection measurements,
                               Collection measurementArgs) {
        _name = name;
        _alias = alias;
        _units = units;
        _collectionType = collectionType;
        _defaultOn = defaultOn;
        _defaultInterval = defaultInterval;
        _designate = designate;
        _template = template;
        _plugin = plugin;
        _expressionData = expressionData;
        _ctime = ctime;
        _mtime = mtime;
        _monitorableType = monitorableType;
        _category = category;
        _measurements = measurements;
        _measurementArgs = measurementArgs;
    }
   
    // Property accessors
    public Integer getCid() {
        return _cid;
    }

    public void setCid(Integer cid) {
        _cid = cid;
    }

    public String getName() {
        return _name;
    }
    
    public void setName(String name) {
        _name = name;
    }

    public String getAlias() {
        return _alias;
    }
    
    public void setAlias(String alias) {
        _alias = alias;
    }

    public String getUnits() {
        return _units;
    }
    
    public void setUnits(String units) {
        _units = units;
    }

    public Integer getCollectionType() {
        return _collectionType;
    }

    public void setCollectionType(Integer collectionType) {
        _collectionType = collectionType;
    }

    public boolean isDefaultOn() {
        return _defaultOn;
    }
    
    public void setDefaultOn(boolean defaultOn) {
        _defaultOn = defaultOn;
    }

    public long getDefaultInterval() {
        return _defaultInterval;
    }
    
    public void setDefaultInterval(long defaultInterval) {
        _defaultInterval = defaultInterval;
    }

    public boolean isDesignate() {
        return _designate;
    }
    
    public void setDesignate(boolean designate) {
        _designate = designate;
    }

    public String getTemplate() {
        return _template;
    }
    
    public void setTemplate(String template) {
        _template = template;
    }

    public String getPlugin() {
        return _plugin;
    }
    
    public void setPlugin(String plugin) {
        _plugin = plugin;
    }

    public byte[] getExpressionData() {
        return _expressionData;
    }
    
    public void setExpressionData(byte[] expressionData) {
        _expressionData = expressionData;
    }

    public long getCtime() {
        return _ctime;
    }
    
    public void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getMtime() {
        return _mtime;
    }
    
    public void setMtime(long mtime) {
        _mtime = mtime;
    }

    public MonitorableType getMonitorableType() {
        return _monitorableType;
    }
    
    public void setMonitorableType(MonitorableType monitorableType) {
        _monitorableType = monitorableType;
    }

    public Category getCategory() {
        return _category;
    }
    
    public void setCategory(Category category) {
        _category = category;
    }

    public Collection getMeasurements() {
        return _measurements;
    }
    
    public void setMeasurements(Collection measurements) {
        _measurements = measurements;
    }

    public Collection getMeasurementArgs() {
        return _measurementArgs;
    }
    
    public void setMeasurementArgs(Collection measurementArgs) {
        _measurementArgs = measurementArgs;
    }

    public boolean equals(Object other) {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof MeasurementTemplate)) return false;
        MeasurementTemplate castOther = (MeasurementTemplate) other; 
        
        return ((getId() == castOther.getId()) ||
                (getId() != null && castOther.getId() != null &&
                 getId().equals(castOther.getId())));
    }
   
    public int hashCode() {
        int result = 17;
        
        result = 37 * result + (getId() == null ? 0 : getId().hashCode());
        return result;
    }   

    public void addMeasurement(Measurement m)
    {
        Collection coll = getMeasurements();
        if (m != null && coll != null) {
            m.setTemplate(this);
            coll.add(m);
        }
    }
    
    public void addTemplateArg(MeasurementArg a)
    {
        Collection coll = getMeasurementArgs();
        if (a != null && coll != null) {
            a.setTemplateArg(this);
            coll.add(a);
        }
    }
}


