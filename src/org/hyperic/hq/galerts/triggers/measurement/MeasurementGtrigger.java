package org.hyperic.hq.galerts.triggers.measurement;

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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
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
    private final int                _numResources;
    private final boolean            _isPercent;
    private final int                _templateId;
    private final ComparisonOperator _comparator;
    private final Float              _metricVal;
    private final Set                _interestedEvents;
    private final Map                _processedEvents;
    private       int                _groupSize;
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
        
        int leftHand;
        
        if (_isPercent)
            leftHand = numMatched * 100 / _groupSize;
        else
            leftHand = numMatched; 
        
        _log.info("Checking if " + _sizeCompare + " " + leftHand +
                  (_isPercent ? "%" : "") +
                  " of the resources reported " + _metricName + 
                  " " + _comparator + " " + _metricVal);
        
        if (_sizeCompare.isTrue(leftHand, _numResources)) {
            String shortReason, longReason;
            FireReason reason;
                
            shortReason = _sizeCompare + " " + leftHand + 
                (_isPercent ? "%" : "") + " of the resources reported " +
                _metricName + " " + _comparator + " " + _metricVal;
            longReason = shortReason;
            reason = new FireReason(shortReason, longReason);
            _log.info("Trigger firing");
            setFired(reason);
        } else {
            _log.info("Trigger not firing");
            setNotFired();
        }
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
            DerivedMeasurementManagerLocal dMan =
                DerivedMeasurementManagerUtil.getLocalHome().create();
            TemplateManagerLocal tMan = 
                TemplateManagerUtil.getLocalHome().create();
            
            AppdefGroupValue g = gMan.findGroup(sMan.getOverlord(), rg.getId());
            List instanceIds = new ArrayList();
            
            _groupSize = g.getTotalSize();
            for (Iterator i=g.getAppdefGroupEntries().iterator(); i.hasNext(); ) {
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
