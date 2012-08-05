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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequest;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementResponse;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.MeasurementMapper;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;

public class MeasurementTransferImpl implements MeasurementTransfer {
    private static final int MAX_DTPS = 400;
	
    private MeasurementManager measurementMgr;
    private TemplateManager tmpltMgr;
	private DataManager dataMgr; 
	private MeasurementMapper mapper;
    private ExceptionToErrorCodeMapper errorHandler ;

    @Autowired
    public MeasurementTransferImpl(MeasurementManager measurementMgr, TemplateManager tmpltMgr, DataManager dataMgr, 
            MeasurementMapper mapper, ExceptionToErrorCodeMapper errorHandler) {
        super();
        this.measurementMgr = measurementMgr;
        this.tmpltMgr = tmpltMgr;
        this.mapper=mapper;
        this.dataMgr = dataMgr;
        this.errorHandler = errorHandler;
    }
    
    protected long[] getTimeFrame(final String begin, final String end) throws ParseException {
        final DateTimeFormatter dateFormat = ISODateTimeFormat.dateTimeParser() ;
        long[] timeFrame = new long[2];
        timeFrame[0] = dateFormat.parseMillis(begin) ; 
        timeFrame[1] = dateFormat.parseMillis(end) ;
        if (timeFrame[0]>=timeFrame[1] || timeFrame[1]>new Date().getTime()) {
            throw new IllegalArgumentException();
        }
        return timeFrame;
    }
    
    protected List<Measurement> getMeasurements(final String rscId, final List<MeasurementTemplate> tmps, final AuthzSubject authzSubject) throws PermissionException {
        // get measurements
        Map<Integer, List<Integer>> resIdsToTmpIds = new HashMap<Integer, List<Integer>>();
        List<Integer> tmpIds = new ArrayList<Integer>();
        for (MeasurementTemplate tmp : tmps) {
            tmpIds.add(tmp.getId());
        }
        resIdsToTmpIds.put(new Integer(rscId), tmpIds);
        Map<Resource, List<Measurement>> rscTohqMsmts = null;
        rscTohqMsmts = this.measurementMgr.findMeasurements(authzSubject, resIdsToTmpIds);
        if (rscTohqMsmts==null || rscTohqMsmts.size()==0 || rscTohqMsmts.values().isEmpty()) {
            throw new ObjectNotFoundException("there are no measurements of the requested types on the requested resource", Measurement.class.getName());
        }
        List<Measurement> hqMsmts = rscTohqMsmts.values().iterator().next();    // there should be only one list of measurements for one resource
        if (hqMsmts==null || hqMsmts.size()==0) {
            throw new ObjectNotFoundException("there are no measurements of the requested types on the requested resource", Measurement.class.getName());
        }
        return hqMsmts;
    }
    
    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest hqMsmtReq, 
            final String rscId, final String begin, final String end) 
            throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException {

        MeasurementResponse res = new MeasurementResponse();
        if (hqMsmtReq==null || rscId==null || "".equals(rscId) || hqMsmtReq.getMeasurementTemplateNames()==null || hqMsmtReq.getMeasurementTemplateNames().size()==0 || begin==null || end==null || begin.length()<=0 || end.length()<=0) {
            throw new UnsupportedOperationException("the request is missing the resource ID, the measurement template names, the begining or end of the time frame");
        }
        AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        long[] timeFrame = getTimeFrame(begin, end);
        long beginMilli = timeFrame[0], endMilli = timeFrame[1]; 

        // extract all input measurement templates
        List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
        List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
        if (tmps==null || tmps.size()==0) {
            throw new ObjectNotFoundException("there are no measurement templates which carries the requested template names", MeasurementTemplate.class.getName());
        }

        List<Measurement> hqMsmts = getMeasurements(rscId, tmps,authzSubject);
        // get metrics
        for (Measurement hqMsmt : hqMsmts) {
            org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurement(hqMsmt);
            List<HighLowMetricValue> hqMetrics = this.dataMgr.getHistoricalData(hqMsmt, beginMilli, endMilli, true, MAX_DTPS);
            if (hqMetrics!=null && hqMetrics.size()!=0) {
                List<org.hyperic.hq.api.model.measurements.Metric> metrics = this.mapper.toMetrics(hqMetrics);
                msmt.setMetrics(metrics);
            }
            res.add(msmt);
        }
        return res;
    }
    
    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext,
            ResourceMeasurementRequests hqMsmtReqs, String begin, String end)
            throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException {
        ResourceMeasurementBatchResponse res = new ResourceMeasurementBatchResponse(this.errorHandler);
        if (hqMsmtReqs==null || hqMsmtReqs.getMeasurementRequests()==null || hqMsmtReqs.getMeasurementRequests().size()==0 || begin==null || end==null || begin.length()<=0 || end.length()<=0) {
            throw new UnsupportedOperationException("the request is missing some essential details");
        }
        AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        long[] timeFrame = getTimeFrame(begin, end);
        long beginMilli = timeFrame[0], endMilli = timeFrame[1];
        
        // extract all input measurement templates
        for (ResourceMeasurementRequest hqMsmtReq : hqMsmtReqs.getMeasurementRequests()) {
            String rscId = hqMsmtReq.getRscId();
            try {
            if (rscId==null || "".equals(rscId)) {
                throw new ObjectNotFoundException("no resource ID supplied",Resource.class.getName());
            }
            List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
            List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
            if (tmps==null || tmps.size()==0) {
                res.addFailedResource(rscId,ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND.getErrorCode(),null,new Object[] {});
                continue;
            }
            List<Measurement> hqMsmts = getMeasurements(rscId, tmps,authzSubject);
            // sort msmts as per their IDs
            Map<Integer,Measurement> msmtIdToMsmt = new HashMap<Integer,Measurement>();
            for (Measurement msmt : hqMsmts) {
                msmtIdToMsmt.put(msmt.getId(), msmt);
            }
            Map<Integer, double[]> msmtNamesToAgg = this.dataMgr.getAggregateDataAndAvailUpByMetric(hqMsmts, beginMilli, endMilli);
            
            ResourceMeasurementResponse msmtRes = new ResourceMeasurementResponse(rscId);
            for (Map.Entry<Integer, double[]> msmtNameToAggEntry : msmtNamesToAgg.entrySet()) {
                Integer msmtId = msmtNameToAggEntry.getKey();
                double[] agg = msmtNameToAggEntry.getValue();
                // no val for that msmt
                if (agg==null || agg.length<=MeasurementConstants.IND_AVG) {
                    continue;
                }
                double avg = agg[MeasurementConstants.IND_AVG];
                Measurement hqMsmt = msmtIdToMsmt.get(msmtId);
                // ignore tmps which were not requested (should not happen)
                if (hqMsmt==null) {
                    continue;
                }
                org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurement(hqMsmt,avg);
                msmtRes.add(msmt);
            }
            res.addResponse(msmtRes);
            }catch(Throwable t) { 
                this.errorHandler.log(t) ; 
                res.addFailedResource(t,rscId, null , null) ;
            }
        }
        return res;
    }
}