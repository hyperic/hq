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
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

public class Measurement extends PersistedObject
    implements ContainerManagedTimestampTrackable, Serializable
{
    private Integer             _instanceId;
    private MeasurementTemplate _template;
    private long                _mtime;
    private boolean             _enabled = true;
    private long                _interval;
    private String              _formula;
    private Collection          _baselines = new ArrayList();
    private Resource            _resource;

    public Measurement() {
    }

    public Measurement(Integer instanceId, MeasurementTemplate template) {
        _instanceId = instanceId;
        _template   = template;
    }

    public Measurement(Integer instanceId, MeasurementTemplate template,
                       long interval)
    {
        this(instanceId, template);
        _interval = interval;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>false</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return false;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    public Integer getInstanceId() {
        return _instanceId;
    }

    protected void setInstanceId(Integer instanceId) {
        _instanceId = instanceId;
    }

    public MeasurementTemplate getTemplate() {
        return _template;
    }

    protected void setTemplate(MeasurementTemplate template) {
        _template = template;
    }

    public long getMtime() {
        return _mtime;
    }

    protected void setMtime(long mtime) {
        _mtime = mtime;
    }

    public Resource getResource() {
        return _resource;
    }

    void setResource(Resource resource) {
        _resource = resource;
    }

    public boolean isEnabled() {
        return _enabled;
    }

    protected void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public long getInterval() {
        return _interval;
    }

    protected void setInterval(long interval) {
        _interval = interval;
    }

    public String getDsn() {
        return _formula;
    }

    protected void setDsn(String formula) {
        _formula = formula;
    }

    public AppdefEntityID getEntityId() {
        return new AppdefEntityID(getAppdefType(), getInstanceId());
    }

    public int getAppdefType() {
        return getTemplate().getMonitorableType().getAppdefType();
    }

    protected void setBaselinesBag(Collection baselines) {
        _baselines = baselines;
    }

    protected Collection getBaselinesBag() {
        return _baselines;
    }

    public Collection getBaselines() {
        return Collections.unmodifiableCollection(_baselines);
    }

    void setBaseline(Baseline b) {
        clearBaseline();
        getBaselinesBag().add(b);
    }

    public Baseline getBaseline() {
        if (getBaselinesBag().isEmpty())
            return null;
        else
            return (Baseline)getBaselinesBag().iterator().next();
    }

    void clearBaseline() {
        getBaselinesBag().clear();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Measurement) || !super.equals(obj)) {
            return false;
        }
        Measurement o = (Measurement)obj;
        return ((_instanceId == o.getInstanceId() ||
                 (_instanceId!=null && o.getInstanceId()!=null &&
                  _instanceId.equals(o.getInstanceId()))))
               &&
               ((_template == o.getTemplate() ||
                 (_template!=null && o.getTemplate()!=null &&
                  _template.equals(o.getTemplate()))));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_instanceId != null ? _instanceId.hashCode(): 0);
        result = 37*result + (_template != null ? _template.hashCode(): 0);
        return result;
    }
}
