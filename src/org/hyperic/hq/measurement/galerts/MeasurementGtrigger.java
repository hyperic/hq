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

package org.hyperic.hq.measurement.galerts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.Zevent;


/**
 * This is a simple trigger which will fire when the criteria is met, and
 * not-fire when the criteria is .. not met. 
 */
public class MeasurementGtrigger 
    extends Gtrigger
{
    private static final Log _log = 
        LogFactory.getLog(MeasurementGtrigger.class);

    private final SizeComparator     _sizeCompare;
    private final int                _numResources; // Num needed to match
    private final boolean            _isPercent;
    private final int                _templateId;
    private final ComparisonOperator _comparator;
    private final Float              _metricVal;
    private final Set                _interestedEvents;
    private final Map                _processedEvents;
    private       int                _groupSize;  // The total size of our group
    private       String             _metricName;
    
    MeasurementGtrigger(SizeComparator sizeCompare, int numResources,
                        boolean isPercent, int templateId,
                        ComparisonOperator comparator, float metricVal) 
    {
        _sizeCompare      = sizeCompare;
        _numResources     = numResources;
        _isPercent        = isPercent;
        _templateId       = templateId;
        _comparator       = comparator;
        _metricVal        = new Float(metricVal);
        _interestedEvents = new HashSet();
        _processedEvents  = new HashMap(_numResources);
        _groupSize        = 0; 
        _metricName       = "Unknown";
    }
    
    public Set getInterestedEvents() {
        return Collections.unmodifiableSet(_interestedEvents);
    }

    public void processEvent(Zevent event) {
        MeasurementZevent me = (MeasurementZevent)event;
        MeasurementZeventPayload val = (MeasurementZeventPayload)me.getPayload(); 
            
        _processedEvents.put(me.getSourceId(), val.getValue());
        tryToFire();
    }

    private void tryToFire() {
        int numMatched;
        
        // Can't process anything unless we have all the resources
        if (_processedEvents.size() != _groupSize)
            return;
        
        numMatched = 0;
        for (Iterator i=_processedEvents.values().iterator(); i.hasNext(); ) {
            MetricValue val = (MetricValue)i.next();
            
            if (_comparator.isTrue(new Float(val.getValue()), _metricVal)) { 
                numMatched++;
            }
        }
        
        String leftHandStr, numMatchStr;
        int leftHand;
        
        if (_isPercent) {
            leftHand    = numMatched * 100 / _groupSize;
            numMatchStr = leftHand + "%"; 
            leftHandStr = (_numResources * 100 / _groupSize) + "%";
        } else {
            leftHand    = numMatched;
            numMatchStr = leftHand + "";
            leftHandStr = "" + _numResources;  
        }
        
        _log.debug("Checking if " + _sizeCompare + " " + _numResources +
                   (_isPercent ? "%" : "") +
                   " of the resources reported " + _metricName + 
                   " " + _comparator + " " + _metricVal);
        _log.debug("Number of resources matching condition: " + numMatched);
        
        if (!_sizeCompare.isTrue(leftHand, _numResources)) {
            setNotFired();
            return;
        }
        
        StringBuffer sr = new StringBuffer();
        StringBuffer lr = new StringBuffer();
            
        sr.append(_sizeCompare)
            .append(" ")
            .append(leftHandStr)
            .append(" of the resources reported ")
            .append(_metricName)
            .append(" ")
            .append(_comparator)
            .append(" ")
            .append(_metricVal);
            
        lr.append(_sizeCompare)
            .append(" ")
            .append(leftHandStr)
            .append(" of the resources (")
            .append(numMatchStr)
            .append(") reported ")
            .append(_metricName)
            .append(" ")
            .append(_comparator)
            .append(" ")
            .append(_metricVal);

        setFired(new FireReason(sr.toString(), lr.toString(), 
                                formulateAuxLogs()));
    }

    private List formulateAuxLogs() {
        // Assemble the aux info
        List auxLogs = new ArrayList();
        DerivedMeasurementManagerLocal dmMan = getDMMan();
        AuthzSubjectValue overlord = 
            AuthzSubjectManagerEJBImpl.getOne().findOverlord();
        for (Iterator i=_processedEvents.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            MeasurementZeventSource src = (MeasurementZeventSource)ent.getKey();
            MetricValue val = (MetricValue)ent.getValue();
            SimpleAlertAuxLog baseLog; 
            DerivedMeasurement metric;
            AppdefEntityValue entVal;
            AppdefEntityID entId;
            String descr, entName, metricName;
            
            if (!_comparator.isTrue(new Float(val.getValue()), _metricVal)) 
                continue;
            
            metric = dmMan.getMeasurement(new Integer(src.getId()));
            entId  = new AppdefEntityID(metric.getAppdefType(), 
                                        metric.getInstanceId());
            entVal = new AppdefEntityValue(entId, overlord);
            try {
                entName = entVal.getName();
            } catch(Exception e) {
                entName = entId.toString();
            }
            metricName = metric.getTemplate().getName();
            descr = entName + " reported " + metric.getTemplate().getName() + 
                    " = " + val.getValue();
                      
            baseLog = new SimpleAlertAuxLog(descr, val.getTimestamp());
            baseLog.addChild(new MetricAuxLog(metricName + " chart", 
                                              val.getTimestamp(), metric));
                                              
            baseLog.addChild(new ResourceAuxLog(entName, val.getTimestamp(), 
                                                entId));
            auxLogs.add(baseLog);
        }
        
        return auxLogs;
    }
    
    private DerivedMeasurementManagerLocal getDMMan() {
        return DerivedMeasurementManagerEJBImpl.getOne();
    }
    
    public void setGroup(ResourceGroup rg) {
        _interestedEvents.clear();
        try {
            // Not sure this is the best way to do this.  It essentially ties us
            // to only having appdef groups come in here.
            AppdefGroupManagerLocal gMan = 
                AppdefGroupManagerUtil.getLocalHome().create();
            AuthzSubjectManagerLocal sMan = 
                AuthzSubjectManagerUtil.getLocalHome().create();
            DerivedMeasurementManagerLocal dMan = getDMMan();
            TemplateManagerLocal tMan = 
                TemplateManagerUtil.getLocalHome().create();
            
            AppdefGroupValue g = gMan.findGroup(sMan.getOverlord(), rg.getId());
            List instanceIds = new ArrayList();
            
            _groupSize = g.getTotalSize();
            for (Iterator i=g.getAppdefGroupEntries().iterator(); i.hasNext(); ) 
            {
                AppdefEntityID ent = (AppdefEntityID)i.next();
                
                instanceIds.add(ent.getId());
            }

            Integer[] iids = new Integer[instanceIds.size()];
            instanceIds.toArray(iids);
            Integer[] mIds = dMan.findMeasurementIds(sMan.getOverlord(), 
                                                     new Integer(_templateId), 
                                                     iids);
                                    
            for (int i = 0; i<mIds.length; i++) {
                int mid = mIds[i].intValue();
                _interestedEvents.add(new MeasurementZeventSource(mid));
            }
            
            if (_interestedEvents.size() != _groupSize) {
                _log.warn("Listening to different # events than resources. " +
                          " This probably means that not everyone in the " + 
                          " group is monitoring something");
            }
            
            _metricName = tMan.getTemplate(new Integer(_templateId)).getName();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
}
