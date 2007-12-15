package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * Merges in services which have been discovered via runtime AI.  
 * 
 * This class also has the responsibility of keeping state about which  
 * services are in the queue, waiting to be processed, and notifying the
 * agent that it still needs to get a runtime service scan.
 */
class ServiceMerger implements ZeventListener {
    private static final Log _log = LogFactory.getLog(ServiceMerger.class);
    
    /**
     * Mapping of (String)agentToken onto (Integer) of # of events in the
     * zevent queue.
     */
    private static final Cache _workingCache =
        CacheManager.getInstance().getCache("ServiceMergerWorking");
    
    
    public void processEvents(List events) {
        AutoinventoryManagerLocal aiMan = AutoinventoryManagerEJBImpl.getOne(); 
        
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            MergeServiceReportZevent evt = (MergeServiceReportZevent)i.next();
            ServiceMergeInfo sInfo = evt.getMergeInfo();
        
            try {
                aiMan.mergeService(sInfo);
            } catch(Exception e) {
                _log.warn("Error merging service", e);
            }
            
            decrementWorkingCache(sInfo.agentToken);
        }
    }

    private static void decrementWorkingCache(String agentToken) {
        synchronized (_workingCache) {
            Element e = _workingCache.get(agentToken);
            
            if (e == null) {
                _log.error("Expected to find element in working cache");
            } else {
                Integer ival = (Integer)e.getValue();
                int val = ival.intValue();
                
                if (val == 1) {
                    _log.debug("Last event processed for agent [" + 
                               agentToken + " ] removing");
                    _workingCache.remove(agentToken);
                    AutoinventoryManagerEJBImpl.getOne()
                        .markServiceClean(agentToken);
                } else {
                    val--;
                    e = new Element(agentToken, new Integer(val));
                    _log.debug("Processed service for agent [" + 
                               agentToken + " ] numLeft= " + val);
                    _workingCache.put(e);
                }
            }
        }
    }
    
    private static void incrementWorkingCache(String agentToken, int num) {
        _log.debug("Adding " + num + " to agent [" + agentToken + 
                   "]'s merge queue"); 
                   
        synchronized (_workingCache) {
            Element e = _workingCache.get(agentToken);

            if (e == null) {
                e = new Element(agentToken, new Integer(num));
                _log.debug("Agent [" + agentToken + "] now has " +
                           num+ " elements in queue");
            } else {
                Integer val = (Integer)e.getValue();
                val = new Integer(val.intValue() + num);
                e = new Element(agentToken, val);
                _log.debug("Agent [" + agentToken + "] now has " + 
                           val + " elements in queue");
            }
            _workingCache.put(e);
        }
    }
    
    public String toString() {
        return "RuntimeAIServiceMerger";
    }
    
    /**
     * Enqueues a list of {@link ServiceMergeInfo}s, indicating services
     * to be merged into appdef.
     */
    static void scheduleServiceMerges(final String agentToken, 
                                      final List serviceMerges) 
    {
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (!success)
                    return;
            
                incrementWorkingCache(agentToken, serviceMerges.size());
            }

            public void beforeCommit() {
            }
        });
        
        List evts = new ArrayList(serviceMerges.size());
        
        for (Iterator i=serviceMerges.iterator(); i.hasNext(); ) {
            ServiceMergeInfo sInfo = (ServiceMergeInfo)i.next();
            
            if (_log.isDebugEnabled()) {
                _log.debug("Enqueueing service merge for " + 
                           sInfo.aiservice.getName() + " on server id=" + 
                           sInfo.serverId);
            }
                
            evts.add(new MergeServiceReportZevent(sInfo));
        }
        
        ZeventManager.getInstance().enqueueEventsAfterCommit(evts);
    }
    
    static boolean currentlyWorkingOn(Agent a) {
        return _workingCache.isKeyInCache(a.getAgentToken());
    }
}
