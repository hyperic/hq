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

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
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
            throw new ObjectNotFoundException(tmps.toString(), Measurement.class.getName());
        }
        List<Measurement> hqMsmts = rscTohqMsmts.values().iterator().next();    // there should be only one list of measurements for one resource
        if (hqMsmts==null || hqMsmts.size()==0) {
            throw new ObjectNotFoundException(tmps.toString(), Measurement.class.getName());
        }
        return hqMsmts;
    }

    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest hqMsmtReq, 
            final String rscId, final Date begin, final Date end) 
                    throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, TimeframeSizeException {

        MeasurementResponse res = new MeasurementResponse();
        if (hqMsmtReq==null || rscId==null || "".equals(rscId) 
                || hqMsmtReq==null || hqMsmtReq.getMeasurementTemplateNames()==null || hqMsmtReq.getMeasurementTemplateNames().size()==0 
                || begin==null || end==null) {
            throw new UnsupportedOperationException("Resource ID= " +rscId
                    + hqMsmtReq!=null?", Measurements Names= "+ hqMsmtReq.getMeasurementTemplateNames():""
                            + ", Message Body= " + hqMsmtReq
                            + ", Begin= " + begin
                            + ", End= " + end);
        }
        if (begin.after(end) || end.after(Calendar.getInstance().getTime())) {
            throw new TimeframeBoundriesException("Begin= " +begin +", End=" +end);
        }
        AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();

        // extract all input measurement templates
        List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
        List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
        if (tmps==null || tmps.size()==0) {
            throw new ObjectNotFoundException(tmpNames.toString(), MeasurementTemplate.class.getName());
        }

        List<Measurement> hqMsmts = getMeasurements(rscId, tmps,authzSubject);
        // get metrics
        for (Measurement hqMsmt : hqMsmts) {
            org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurement(hqMsmt);
            List<HighLowMetricValue> hqMetrics = this.dataMgr.getHistoricalData(hqMsmt, begin.getTime(), end.getTime(), true, MAX_DTPS);
            if (hqMetrics!=null && hqMetrics.size()!=0) {
                List<org.hyperic.hq.api.model.measurements.Metric> metrics = this.mapper.toMetrics(hqMetrics);
                msmt.setMetrics(metrics);
            }
            res.add(msmt);
        }
        return res;
    }

    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext,
            ResourceMeasurementRequests hqMsmtReqs, Date begin, Date end) throws TimeframeBoundriesException, PermissionException, SQLException, UnsupportedOperationException, ObjectNotFoundException {
            ResourceMeasurementBatchResponse res = new ResourceMeasurementBatchResponse(this.errorHandler);
            if (hqMsmtReqs==null || hqMsmtReqs.getMeasurementRequests()==null || hqMsmtReqs.getMeasurementRequests().size()==0 || begin==null || end==null) {
                throw new UnsupportedOperationException("the request is missing some essential details");
            }
            if (begin.after(end) || end.after(Calendar.getInstance().getTime())) {
                throw new TimeframeBoundriesException("Begin= " +begin +", End=" +end);
            }

            AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
            // extract all input measurement templates
            Map<String,List<String>> tmpNameToRscs = new HashMap<String,List<String>>();
            for (ResourceMeasurementRequest hqMsmtReq : hqMsmtReqs.getMeasurementRequests()) {
                String rscId = hqMsmtReq.getRscId();
                try {
                    if (rscId==null || "".equals(rscId)) {
                        throw new ObjectNotFoundException("no resource ID supplied",Resource.class.getName());
                    }
                    List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
                    for (String tmpName : tmpNames) {
                        if (!tmpNameToRscs.containsKey(tmpName)) {
                            tmpNameToRscs.put(tmpName, new ArrayList<String>());
                        }
                        tmpNameToRscs.get(tmpName).add(rscId);
                    }
                }catch(Throwable t) { 
                    this.errorHandler.log(t) ; 
                    res.addFailedResource(t,rscId, null , null) ;
                }
            }                
            // extract tmp Ids per rsc        
            List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(new ArrayList<String>(tmpNameToRscs.keySet()));
            Map<Integer, List<Integer>> resIdsToTmpIds = new HashMap<Integer, List<Integer>>();
            for (MeasurementTemplate tmp : tmps) {
                List<String> rscIds = tmpNameToRscs.get(tmp.getName());
                if (rscIds==null) { continue;   }
                for (String rscId : rscIds) {
                    if (!resIdsToTmpIds.containsKey(rscId)) {
                        resIdsToTmpIds.put(new Integer(rscId), new ArrayList<Integer>());
                    }
                    resIdsToTmpIds.get(new Integer(rscId)).add(tmp.getId());
                }
            }
            // mark resources for which no measurements were found
            for (ResourceMeasurementRequest hqMsmtReq : hqMsmtReqs.getMeasurementRequests()) {
                // by now we know that all reqs are with valid rscs, o/w we wouldn't get here
                String rscId = hqMsmtReq.getRscId();
                // if the requested rsc is not in the map of rscs for which at least one template was found, mark it as a failed rsc
                if (!resIdsToTmpIds.keySet().contains(rscId)) {
                    res.addFailedResource(rscId,ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND.getErrorCode(),null,new Object[] {});
                }
            }

            Map<Resource, List<Measurement>> rscToHqMsmts = this.measurementMgr.findMeasurements(authzSubject, resIdsToTmpIds);
            if (rscToHqMsmts==null || rscToHqMsmts.size()==0 || rscToHqMsmts.values().isEmpty()) {
                throw new ObjectNotFoundException(tmps.toString(), Measurement.class.getName());
            }
            // validate that all rscs have msmts, and map msmt names to rscs
            Map<Integer,Resource> msmtIdToRsc = new HashMap<Integer,Resource>();
            Set<Measurement> allMsmts = new HashSet<Measurement>();
            for (Map.Entry<Resource,List<Measurement>> rscToHqMsmtsEntry : rscToHqMsmts.entrySet()) {
                Resource rsc = rscToHqMsmtsEntry.getKey();
                List<Measurement> msmts = rscToHqMsmtsEntry.getValue();
                if (msmts==null || msmts.size()==0) {
                    res.addFailedResource(String.valueOf(rsc.getId()),ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND.getErrorCode(),null,new Object[] {});
                } else {
                    for (Measurement msmt : msmts) {
                        msmtIdToRsc.put(msmt.getId(),rsc);
                    }
                    allMsmts.addAll(msmts);
                }
            }
            // sort msmts as per their IDs
            Map<Integer,Measurement> msmtIdToMsmt = new HashMap<Integer,Measurement>();
            for (Measurement msmt : allMsmts) {
                msmtIdToMsmt.put(msmt.getId(), msmt);
            }
            Map<Integer, double[]> msmtNamesToAgg = this.dataMgr.getAggregateDataAndAvailUpByMetric(new ArrayList<Measurement>(allMsmts), begin.getTime(), end.getTime());
            Map<Integer,ResourceMeasurementResponse> rscIdToRes = new HashMap<Integer,ResourceMeasurementResponse>();

            for (Map.Entry<Integer, double[]> msmtNameToAggEntry : msmtNamesToAgg.entrySet()) {
                Integer msmtId = msmtNameToAggEntry.getKey();
                Resource rsc = msmtIdToRsc.get(msmtId);
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

                if (!rscIdToRes.containsKey(rsc.getId())) {
                    rscIdToRes.put(rsc.getId(), new ResourceMeasurementResponse(String.valueOf(rsc.getId())));
                }
                rscIdToRes.get(rsc.getId()).add(msmt);
            }
            for (ResourceMeasurementResponse rscRes : rscIdToRes.values()) {
                res.addResponse(rscRes);
            }
            return res;
    }
}