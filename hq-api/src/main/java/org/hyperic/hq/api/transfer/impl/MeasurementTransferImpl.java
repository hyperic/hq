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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.MeasurementMapper;
import org.hyperic.hq.api.transfer.mapping.ResourceMapper;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public MeasurementResponse getMetrics(final MeasurementRequest hqMsmtReq, final Calendar begin, final Calendar end) {
        MeasurementResponse res = new MeasurementResponse();
    	// extract all input measurement templates
    	List<String> tmpNames = hqMsmtReq.getMeasurementTemplateNames();
    	List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
		String rscId = hqMsmtReq.getResourceId();
		// get measurements
		Map<Integer, List<Integer>> resIdsToTmpIds = new HashMap<Integer, List<Integer>>();
		List<Integer> tmpIds = new ArrayList<Integer>();
		for (MeasurementTemplate tmp : tmps) {
		    tmpIds.add(tmp.getId());
        }
		resIdsToTmpIds.put(new Integer(rscId), tmpIds);
		AuthzSubject authSubject = this.getAuthzSubject();
		List<Measurement> hqMsmts = null;
		try {
            Map<Resource, List<Measurement>> rscTohqMsmts = this.measurementMgr.findMeasurements(authSubject, resIdsToTmpIds);
            hqMsmts = rscTohqMsmts.get(rscId);
        } catch (PermissionException e) {
            res.addFailedResource(e,rscId, null, null) ;
            return res;
        }
    	// get metrics
        for (Measurement hqMsmt : hqMsmts) {
            PageList<HighLowMetricValue> hqMetrics = this.dataMgr.getHistoricalData(hqMsmt, begin.getTimeInMillis(), end.getTimeInMillis(), PageControl.PAGE_ALL, true, MAX_DTPS);
            org.hyperic.hq.api.model.measurements.Measurement msmt = this.mapper.toMeasurement(hqMsmt);
            List<org.hyperic.hq.api.model.measurements.Metric> metrics = this.mapper.toMetrics(hqMetrics);
            msmt.setMetrics(metrics);
            res.add(msmt);
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
