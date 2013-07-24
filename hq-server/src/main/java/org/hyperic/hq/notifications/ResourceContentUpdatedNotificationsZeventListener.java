package org.hyperic.hq.notifications;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceContentChangedZevent;
import org.hyperic.hq.bizapp.shared.AllConfigDiff;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.ResourceChangedContentNotification;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

@Component("resourceContentUpdatedNotificationsZeventListener")
public class ResourceContentUpdatedNotificationsZeventListener extends InventoryNotificationsZeventListener<ResourceContentChangedZevent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(ResourceContentChangedZevent.class, (ZeventListener<ResourceContentChangedZevent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    public String getListenersBeanName() {
        return "resourceContentUpdatedNotificationsZeventListener";
    }
    @SuppressWarnings("unchecked")
    @Override
    protected ResourceChangedContentNotification createNotification(ResourceContentChangedZevent event) {
        Integer rid = event.getResourceID();
        Map<String,String> configValues = new HashMap<String,String>(); 
        
        String changedResourceName = event.getResourceName();
        if (changedResourceName!=null) {
            configValues.put(HQConstants.RESOURCE_NAME, changedResourceName);
        }
        
        AllConfigDiff allConfDiff = event.getAllConfigs();
        if (allConfDiff!=null) {
            String[] cfgTypes = ProductPlugin.CONFIGURABLE_TYPES;
            int numConfigs = cfgTypes.length;
            for (int type = 0 ; type<numConfigs ; type++) {
                //TODO~ handle creation/update props differently
                //TODO~ handle delete props
                AllConfigResponses allNewConf = allConfDiff.getNewAllConf();
                ConfigResponse newConf = allNewConf.getConfig(type);
                if (newConf != null) {
                    Map<String,String> newConfMap = newConf.getConfig();
                    configValues.putAll(newConfMap);
                }

                AllConfigResponses allChangedConf = allConfDiff.getChangedAllConf();
                ConfigResponse changedConf = allChangedConf.getConfig(type);
                if (changedConf != null) {
                    Map<String,String> changedConfMap = changedConf.getConfig();
                    configValues.putAll(changedConfMap);
                }
            }
        }

        Map<String,String> cprops = event.getCProps();
        if (cprops!=null) {
            for(Map.Entry<String,String> cprop:cprops.entrySet()) {
                configValues.put(cprop.getKey(),cprop.getValue());
            }
        }
        
        ResourceChangedContentNotification n = new ResourceChangedContentNotification(rid,configValues);
        return n;
    }
}
//