/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.api.transfer;
 
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricNotifications;
import org.hyperic.hq.api.model.measurements.MetricResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.notifications.UnregisteredException;
import org.hyperic.hq.notifications.filtering.MetricFilter;
import org.hyperic.hq.notifications.filtering.MetricFilterByResource;

/**
 * 
 * @author yakarn
 *
 */
public interface MeasurementTransfer {
	/**
	 * 
	 * @param measurementsRequests	measurement requests which are null or have no resource IDs will be ignored
	 * 								no measurements will be calculated for measurementTemplates which are unknown in the system
	 * 								
	 * @return
	 * @throws ParseException 
	 * @throws PermissionException 
	 * @throws TimeframeBoundriesException 
	 * @throws ObjectNotFoundException 
	 * @throws UnsupportedOperationException 
	 * @throws TimeframeSizeException 
	 * @throws IllegalArgumentException 
	 */
    public MetricResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest measurementRequest,
            final String rscId, final Date begin, final Date end) throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, TimeframeSizeException;
    
    public RegistrationID register(final MetricFilterRequest metricFilterReq);

    /**
     * unregister session data and all assigned filters
     * 
     * @param sessionId
     */
    public void unregister();

    /**
     * unregister the filters assigned to this user destination
     * 
     * @param sessionId
     * @param rscFilter
     * @param metricFilter
     */
    public void unregister(final MetricFilterRequest metricFilterReq);

    public MetricNotifications poll() throws UnregisteredException;
    
    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext, final ResourceMeasurementRequests hqMsmtReqs, 
            final Date begin, final Date end) 
            throws PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, SQLException;
    public ResourceMeasurementBatchResponse getMeasurements(ApiMessageContext apiMessageContext,BulkResourceMeasurementRequest msmtMetaReq);

}
