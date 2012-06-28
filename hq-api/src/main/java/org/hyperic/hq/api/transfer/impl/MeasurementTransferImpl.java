/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
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
package org.hyperic.hq.api.transfer.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.mapping.MeasurementMapper;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

//@Component
public class MeasurementTransferImpl implements MeasurementTransfer {
	private static final int MAX_DTPS = 400;
	
	@Autowired
    private MeasurementManager measurementMgr;
	@Autowired
    private TemplateManager tmpltMgr;
	@Autowired
	private DataManager dataMgr; 
	@Autowired
	private MeasurementMapper mapper;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    public MeasurementResponse getMetrics(final MeasurementRequest hqMsmtReq, final String begin, final String end) {
        MeasurementResponse res = new MeasurementResponse();
        String rscId = hqMsmtReq.getResourceId();
        try {
            final DateFormat dateFormat = new SimpleDateFormat() ;
            Date beginDate = null, endDate = null ; 
            try {
                beginDate = dateFormat.parse(begin) ; 
                endDate = dateFormat.parse(end) ;
            } catch (ParseException e) {
                throw new IllegalArgumentException("cannot parse the begin/end time frame arguments",e);
            }
            AuthzSubject authSubject = this.getAuthzSubject();
            // extract all input measurement templates
            List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
            if (tmpNames==null || tmpNames.size()==0) {
                throw new UnsupportedOperationException("request must contain at least one measurement template name");
            }
            List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
            if (tmps==null || tmps.size()==0) {
                throw new ObjectNotFoundException("there are no measurement templates which carries the requested template names", MeasurementTemplate.class.getName());
            }
            // get measurements
            Map<Integer, List<Integer>> resIdsToTmpIds = new HashMap<Integer, List<Integer>>();
            List<Integer> tmpIds = new ArrayList<Integer>();
            for (MeasurementTemplate tmp : tmps) {
                tmpIds.add(tmp.getId());
            }
            resIdsToTmpIds.put(new Integer(rscId), tmpIds);
            Map<Resource, List<Measurement>> rscTohqMsmts = this.measurementMgr.findMeasurements(authSubject, resIdsToTmpIds);
            
            if (rscTohqMsmts==null || rscTohqMsmts.size()==0 || rscTohqMsmts.values().isEmpty()) {
                throw new ObjectNotFoundException("there are no measurements of the requested templates types on the requested resource", Measurement.class.getName());
            }
            List<Measurement> hqMsmts = rscTohqMsmts.values().iterator().next();    // there should be only one list of measurements for one resource
            if (hqMsmts==null || hqMsmts.size()==0) {
                throw new ObjectNotFoundException("there are no measurements of the requested templates types on the requested resource", Measurement.class.getName());
            }

            // get metrics
            for (Measurement hqMsmt : hqMsmts) {
                org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurement(hqMsmt);
                List<HighLowMetricValue> hqMetrics = this.dataMgr.getHistoricalData(hqMsmt, beginDate.getTime(), endDate.getTime(), true, MAX_DTPS);
                if (hqMetrics!=null && hqMetrics.size()!=0) {
                    List<org.hyperic.hq.api.model.measurements.Metric> metrics = this.mapper.toMetrics(hqMetrics);
                    msmt.setMetrics(metrics);
                }
                res.add(msmt);
            }
        } catch (Throwable e) {
            res.addFailedResource(e,rscId, null, null) ;
            return res;
        }
        return res;
    }
    
    private final AuthzSubject getAuthzSubject() {
        //TODO: replace with actual subject once security layer is implemented 
        //return authzSubjectManager.getOverlordPojo();
        AuthzSubject subject = authzSubjectManager.findSubjectByName("hqadmin") ;
        return (subject != null ? subject : authzSubjectManager.getOverlordPojo()) ; 
    }
} 
