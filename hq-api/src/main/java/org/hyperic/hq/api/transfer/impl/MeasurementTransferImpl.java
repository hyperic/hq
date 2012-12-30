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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricNotifications;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequest;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementResponse;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.MeasurementMapper;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.notifications.Q;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class MeasurementTransferImpl implements MeasurementTransfer {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    private static final int MAX_DTPS = 400;

    private ResourceManager resourceManager ; 
    private MeasurementManager measurementMgr;
    private TemplateManager tmpltMgr;
    private DataManager dataMgr; 
    private MeasurementMapper mapper;
    private ExceptionToErrorCodeMapper errorHandler ;
    private MetricDestinationEvaluator evaluator;
    private Q q;
    @javax.ws.rs.core.Context
    private SearchContext context ;
    
    @Autowired
    public MeasurementTransferImpl(ResourceManager resourceManager,MeasurementManager measurementMgr, TemplateManager tmpltMgr, DataManager dataMgr, 
            MeasurementMapper mapper, ExceptionToErrorCodeMapper errorHandler, MetricDestinationEvaluator evaluator, Q q) {
        super();
        this.resourceManager = resourceManager;
        this.measurementMgr = measurementMgr;
        this.tmpltMgr = tmpltMgr;
        this.mapper=mapper;
        this.dataMgr = dataMgr;
        this.errorHandler = errorHandler;
        this.evaluator = evaluator;
        this.q = q;
    }

    protected List<Measurement> getMeasurements(final String rscId, final List<MeasurementTemplate> tmps, final AuthzSubject authzSubject) throws PermissionException {
        // get measurements
        Map<Integer, List<Integer>> resIdsToTmpIds = new HashMap<Integer, List<Integer>>();
        List<Integer> tmpIds = new ArrayList<Integer>();
        for (MeasurementTemplate tmp : tmps) {
            tmpIds.add(tmp.getId());
        }
        resIdsToTmpIds.put(Integer.valueOf(rscId), tmpIds);
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
    
    protected Map<Integer,Destination> sessionToDestination = new HashMap<Integer,Destination>();
    
    public void register(Integer sessionId, final MetricFilterRequest metricFilterReq) {
        //TODO~ return failed/successful registration
        //TODO~ add schema to the xml's which automatically validates legal values (no null / empty name for instance)
        if (!MetricFilterRequest.validate(metricFilterReq)) {
            if (log.isDebugEnabled()) {
                log.debug("illegal request");
            }
            return;
        }
        List<Filter<MetricNotification,? extends FilteringCondition<?>>> userFilters = this.mapper.toMetricFilters(metricFilterReq); 
        // TODO~ init filters with needed managers to enable them to retrieve filter related data
        
        Destination dest = this.sessionToDestination.get(sessionId); 
        if (dest==null) {
            dest = new Destination() {};
            this.sessionToDestination.put(sessionId,dest);
            this.q.register(dest);
        }
        this.evaluator.register(dest,userFilters);
    }
    public void unregister(Integer sessionId) {
        Destination dest = this.sessionToDestination.get(sessionId); 
        if (dest!=null) {
            this.sessionToDestination.remove(sessionId);
            this.q.unregister(dest);
            this.evaluator.unregisterAll(dest);
        }        
    }
    public void unregister(final Integer sessionId, final MetricFilterRequest metricFilterReq) {
        //TODO~ return failed/successful registration
        if (!MetricFilterRequest.validate(metricFilterReq)) {
            if (log.isDebugEnabled()) {
                log.debug("illegal request");
            }
            return;
        }
        List<Filter<MetricNotification,? extends FilteringCondition<?>>> userFilters = this.mapper.toMetricFilters(metricFilterReq); 
        if (userFilters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be unregistered");
            }
            return;
        }
        Destination dest = this.sessionToDestination.get(sessionId); 
        if (dest==null) {
            if (log.isDebugEnabled()) {
                log.debug("no destination was previously registered with the current user session");
            }
            return;
        }
        this.evaluator.unregister(dest,userFilters);
    }
    
    public MetricNotifications poll(Integer sessionId) {
        //TODO~ return adequate response if not registered
        MetricNotifications res = new MetricNotifications();
        Destination dest = this.sessionToDestination.get(sessionId);
        if (dest==null) {
            log.error("the current session is not registered for notifications");
            this.errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
        }
        List<MetricNotification> mns = (List<MetricNotification>) this.q.poll(dest);
        if (mns.isEmpty()) {
            return res;
        }
        List<? extends RawMetric> metrics = this.mapper.toMetricsWithId(mns);
        res.setMetrics(metrics);
        return res;
    }
    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest hqMsmtReq, 
            final String rscId, final Date begin, final Date end) 
                    throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, TimeframeSizeException {

        MeasurementResponse res = new MeasurementResponse();
        if (hqMsmtReq==null || hqMsmtReq.getMeasurementTemplateNames()==null || hqMsmtReq.getMeasurementTemplateNames().size()==0) {
            throw new UnsupportedOperationException("message body is missing or corrupted"); 
        }
        validateTimeFrame(begin,end);
        if (rscId==null || "".equals(rscId)) {
            throw new UnsupportedOperationException("The request URL is missing the resource ID");
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
    protected void validateTimeFrame(Date begin, Date end) throws TimeframeBoundriesException {
        StringBuilder errorMsg = new StringBuilder();
        if (begin==null) {
            errorMsg.append("The request URL is missing the time frame begining");
        }
        if (end==null) {
            if (errorMsg.length()>0) {
                errorMsg.append(" and end");
            } else {
                errorMsg.append("The request URL is missing the time frame end");
            }
        }
        if (errorMsg.length()>0) {
            throw new TimeframeBoundriesException(errorMsg.toString());
        }
        if (begin.after(end)) {
                errorMsg.append("Time frame end time is before its start time");
        }
        if (end.after(Calendar.getInstance().getTime())) {
            errorMsg.append("Time frame ends in the future");
        }
        if (errorMsg.length()>0) {
            throw new TimeframeBoundriesException(errorMsg.toString());
        }
    }
    
    @Transactional(readOnly = true)
    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext,
            ResourceMeasurementRequests hqMsmtReqs, Date begin, Date end) throws TimeframeBoundriesException, PermissionException, SQLException, UnsupportedOperationException, ObjectNotFoundException {
            ResourceMeasurementBatchResponse res = new ResourceMeasurementBatchResponse(this.errorHandler);
            if (hqMsmtReqs==null || hqMsmtReqs.getMeasurementRequests()==null || hqMsmtReqs.getMeasurementRequests().size()==0) {
                throw new UnsupportedOperationException("message body is missing or corrupted"); 
            }
            validateTimeFrame(begin,end);
            AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
            // extract all input measurement templates
            Map<String,List<String>> tmpNameToRscs = new HashMap<String,List<String>>();
            List<String> tmpNames = null;
            String rscId = null;
            for (ResourceMeasurementRequest hqMsmtReq : hqMsmtReqs.getMeasurementRequests()) {
                rscId = hqMsmtReq.getRscId();
                if (rscId==null || "".equals(rscId)) {
                    throw new ObjectNotFoundException("no resource ID supplied",Resource.class.getName());
                }
                tmpNames = hqMsmtReq.getMeasurementTemplateNames();
                for (String tmpName : tmpNames) {
                    List<String> rscs = tmpNameToRscs.get(tmpName);
                    if (rscs==null) {
                        rscs = new ArrayList<String>(); 
                        tmpNameToRscs.put(tmpName, rscs);
                    }
                    rscs.add(rscId);
                }
            }
            // extract tmp Ids per rsc
            List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(new ArrayList<String>(tmpNameToRscs.keySet()));
            Map<Integer, List<Integer>> rscIdsToTmpIds = new HashMap<Integer, List<Integer>>(); // will contain all the resources for which at least one of the templates requested for them exists
            List<String> rscIds = null;
            for (MeasurementTemplate tmp : tmps) {
                rscIds = tmpNameToRscs.get(tmp.getAlias());
                if (rscIds==null) { continue;   }
                for (String _rscId : rscIds) {
                    Integer rscIdInt = Integer.valueOf(_rscId);
                    List<Integer> tmpIds = rscIdsToTmpIds.get(rscIdInt);
                    if (tmpIds==null) {
                        tmpIds = new ArrayList<Integer>(); 
                        rscIdsToTmpIds.put(rscIdInt,tmpIds );
                    }
                    tmpIds.add(tmp.getId());
                }
            }
            // mark resources for which no measurements were found
            final String TEMPLATE_NOT_FOUND_ERR_CODE = ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND.getErrorCode();
            rscId = null;
            for (ResourceMeasurementRequest hqMsmtReq : hqMsmtReqs.getMeasurementRequests()) {
                // by now we know that all reqs are with valid rscs, o/w we wouldn't get here
                rscId = hqMsmtReq.getRscId();
                // if the requested rsc is not in the map of rscs for which at least one template was found, mark it as a failed rsc
                if (!rscIdsToTmpIds.keySet().contains(Integer.valueOf(rscId))) {
                    res.addFailedResource(rscId,TEMPLATE_NOT_FOUND_ERR_CODE,null,new Object[] {""});
                }
            }
            Map<Integer,Exception> failedRscs = new HashMap<Integer,Exception>();
            Map<Resource, List<Measurement>> rscToHqMsmts = this.measurementMgr.findBulkMeasurements(authzSubject, rscIdsToTmpIds, failedRscs);
            if (rscToHqMsmts==null) {
                throw new ObjectNotFoundException(tmps.toString(), Measurement.class.getName());
            }
            final String MEASUREMENT_NOT_FOUND = ExceptionToErrorCodeMapper.ErrorCode.MEASUREMENT_NOT_FOUND.getErrorCode();
            for (Map.Entry<Integer,Exception> failedRscEntry : failedRscs.entrySet()) {
                Integer failedRscId = failedRscEntry.getKey();
                Exception e = failedRscEntry.getValue();
                if (e==null) {
                    res.addFailedResource(String.valueOf(failedRscId),MEASUREMENT_NOT_FOUND,null,new Object[] {""});
                } else {
                    res.addFailedResource(String.valueOf(failedRscId),e.getMessage(),null, new Object[] {""});
                }
            }
            // validate that all rscs have msmts, and map msmt names to rscs
            Map<Integer,Resource> msmtIdToRsc = new HashMap<Integer,Resource>();
            Set<Measurement> allMsmts = new HashSet<Measurement>();
            List<Measurement> msmts = null;
            for (Map.Entry<Resource,List<Measurement>> rscToHqMsmtsEntry : rscToHqMsmts.entrySet()) {
                Resource rsc = rscToHqMsmtsEntry.getKey();
                msmts = rscToHqMsmtsEntry.getValue();
                if (msmts==null || msmts.size()==0) {
                    res.addFailedResource(String.valueOf(rsc.getId()),TEMPLATE_NOT_FOUND_ERR_CODE,null,new Object[] {""});
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
            
            Map<Integer, double[]> msmtIdToAgg = this.dataMgr.getAggregateDataAndAvailUpByMetric(new ArrayList<Measurement>(allMsmts), begin.getTime(), end.getTime());
            Map<Integer,ResourceMeasurementResponse> rscIdToRes = new HashMap<Integer,ResourceMeasurementResponse>();
            Resource rsc = null;
            for (Map.Entry<Integer, double[]> msmtIdToAggEntry : msmtIdToAgg.entrySet()) {
                Integer msmtId = msmtIdToAggEntry.getKey();
                rsc = msmtIdToRsc.get(msmtId);
                double[] agg = msmtIdToAggEntry.getValue();
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
                
                ResourceMeasurementResponse rscRes =  rscIdToRes.get(rsc.getId());
                if (rscRes==null) {
                    rscRes = new ResourceMeasurementResponse(String.valueOf(rsc.getId()));
                    rscIdToRes.put(rsc.getId(), rscRes);
                    res.addResponse(rscRes);
                }
                rscRes.add(msmt);
            }
            return res;
    }
    
    @Transactional(readOnly = true)
    public ResourceMeasurementBatchResponse getMeasurements(ApiMessageContext apiMessageContext, BulkResourceMeasurementRequest rcsMsmtReq) {
        ResourceMeasurementBatchResponse res = new ResourceMeasurementBatchResponse();
        AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        List<ID> ids = rcsMsmtReq.getRids();
        List<Integer> rids = this.mapper.toIds(ids);
        for(Integer rid:rids) {
            Resource rsc = this.resourceManager.findResourceById(rid);
            if (rsc==null) {
                res.addFailedResource(String.valueOf(rid), ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID.getErrorCode(), null,new Object[] {""});
                log.error("resource not found for resource id - " + rid);
                continue;
            }
            ResourceMeasurementResponse rscRes = new ResourceMeasurementResponse();
            rscRes.setRscId(String.valueOf(rid));
            Collection<Measurement> hqMsmts = this.measurementMgr.findMeasurements(authzSubject, rsc);
            for(Measurement hqMsmt:hqMsmts) {
//                Integer tmplId = hqMsmt.getTemplate().getId();
                MeasurementTemplate hqTmpl = hqMsmt.getTemplate();//tmpltMgr.getTemplate(tmplId);
                org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurementExtendedData(hqMsmt,hqTmpl);
                rscRes.add(msmt);
            }
            res.addResponse(rscRes);
        }

        return res;
    }
}