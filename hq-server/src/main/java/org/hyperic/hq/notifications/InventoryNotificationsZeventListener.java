package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//@Component("InventoryNotificationsZeventListener")
public class InventoryNotificationsZeventListener {//extends BaseNotificationsZeventListener<ResourceZevent,InventoryNotification> {
//    private final Log log = LogFactory.getLog(InventoryNotificationsZeventListener.class);
//    protected ConcurrentStatsCollector concurrentStatsCollector;
//    protected ZeventEnqueuer zEventManager;
//    protected Q q;
//
//    @Autowired
//    InventoryNotificationsZeventListener(Q q,ZeventEnqueuer zEventManager, ResourceDestinationEvaluator evaluator,ConcurrentStatsCollector concurrentStatsCollector) {
//        this.zEventManager=zEventManager;
//        this.evaluator = evaluator;
//        this.concurrentStatsCollector=concurrentStatsCollector;
//        this.q = q;
//    }
//    
//    protected List<InventoryNotification> extract(List<ResourceZevent> events) {
//        List<InventoryNotification> ns = new ArrayList<InventoryNotification>();
////        for(ResourceZevent event:events) {
////            MeasurementZeventSource zEventSource = (MeasurementZeventSource) measurementZevent.getSourceId(); 
////            MeasurementZeventPayload zEventPayload = (MeasurementZeventPayload) measurementZevent.getPayload();
////            
////            int measurementId = 0;
////            if (zEventSource!=null) {
////                measurementId = zEventSource.getId();
////            }
////            MetricValue metricVal = null;
////            if (zEventPayload!=null) {
////                metricVal = zEventPayload.getValue();
////            }
////            Integer mid = Integer.valueOf(measurementId);
////            
////            Resource rsc = msmt.getResource();
////            InventoryNotification n = new MetricNotification(rsc.getId(),mid,metricVal);
////            ns.add(n);
////        }
//        return ns;
//    } 
//    
//    @Override
//    protected String getListenersBeanName() {
//        return "InventoryNotificationsZeventListener";
//    }
//
//    @Override
//    protected String getConcurrentStatsCollectorType() {
//        return ConcurrentStatsCollector.INVENTORY_NOTIFICATION_FILTERING_TIME;
//    }
//
//    @Override
//    @Autowired
//    protected void setEvaluator(DestinationEvaluator<InventoryNotification> evaluator) {
//        this.evaluator=evaluator;
//    }
}
