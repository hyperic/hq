package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.NotificationGroup;
import org.springframework.stereotype.Component;

@Component
public class DestinationEvaluator {
    private final Log log = LogFactory.getLog(DestinationEvaluator.class);
    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another,
    // then sync between them), o/w will pose problems in scale
    protected Map<NotificationEndpoint,FilterChain<BaseNotification>> destToFilter = new ConcurrentHashMap<NotificationEndpoint,FilterChain<BaseNotification>>();
    protected Map<Class<? extends BaseNotification>,Set<NotificationEndpoint>> entityTypeToReg = new ConcurrentHashMap<Class<? extends BaseNotification>,Set<NotificationEndpoint>>();
    protected Map<NotificationEndpoint,Class<? extends BaseNotification>> regToEntityType = new  ConcurrentHashMap<NotificationEndpoint,Class<? extends BaseNotification>>();
    /**
     * append filters
     * 
     * @param dest
     * @param filters
     */
    public void register(Class<? extends BaseNotification> entityType, NotificationEndpoint endpoint, Collection<? extends Filter<? extends BaseNotification,? extends FilteringCondition<?>>> filters) {
        if (filters == null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be registered");
            }
        }
        synchronized (destToFilter) {
            FilterChain<BaseNotification> filterChain = this.destToFilter.get(endpoint);
            if (filterChain==null) {
                filterChain = new FilterChain<BaseNotification>(filters);
                destToFilter.put(endpoint, filterChain);
                if (log.isDebugEnabled()) {
                    log.debug("registering the following filters to destination " + endpoint + 
                              " (no previous filters were assigned to it):\n" + filters);
                }
            } else {
                filterChain.addAll(filters);
                if (log.isDebugEnabled()) {
                    log.debug("appending the following filters to destination " + endpoint + ":\n" + filters);
                }
            }
        }
        Set<NotificationEndpoint> regIDSet = entityTypeToReg.get(entityType);
        if (regIDSet==null) {
            regIDSet = new HashSet<NotificationEndpoint>();
            regIDSet.add(endpoint);
            entityTypeToReg.put(entityType, regIDSet);
        } else {
            regIDSet.add(endpoint);
        }
        regToEntityType.put(endpoint,entityType);        
    }

    /**
     * unregister all filters assigned to this destination
     * @param dest
     */
    public void unregister(NotificationEndpoint endpoint) {
        FilterChain<? extends BaseNotification> filterChain = this.regToFilter.remove(endpoint);
        synchronized (destToFilter) {
            filterChain = destToFilter.remove(endpoint);
        }
        if (log.isDebugEnabled()) {
            if (filterChain == null) {
                log.debug("no filters were previously registered with destination " + endpoint);
            } else {
                // TODO~ remove all filter chain filters from it
                log.debug("un-registering all previously regitered filters from destination " + endpoint + ":\n" + filterChain);
            }
        }
        Class<? extends BaseNotification> entityType = regToEntityType.remove(endpoint);
        entityTypeToReg.remove(entityType);
    }

    /**
     * 
     * @param dest
     * @param filters
     */
    public void unregister(NotificationEndpoint endpoint, List<Filter<N,? extends FilteringCondition<?>>> filters) {
        if (filters==null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be un-registered from endpoint " + endpoint);
            }
            return;
        }
        synchronized (destToFilter) {
            FilterChain<N> filterChain = destToFilter.get(endpoint);
            if (filterChain==null) {
                if (log.isDebugEnabled()) {
                    log.debug("no filters were previously registered with endpoint " + endpoint);
                }
            } else {
                filterChain.removeAll(filters);
                if (log.isDebugEnabled()) {
                    log.debug("un-registering the following filters from endpoint " + endpoint + ":\n" + filters);
                }
                if (filterChain.isEmpty()) {
                    destToFilter.remove(endpoint);
                    if (log.isDebugEnabled()) {
                        log.debug("un-registering the following endpoint " + endpoint);
                    }
                }
            }
        }
    }

    public Map<NotificationEndpoint, NotificationGroup> evaluate(final List<? extends BaseNotification> ns, Class<? extends BaseNotification> entityType) {
        final Map<NotificationEndpoint, NotificationGroup> rtn = new HashMap<NotificationEndpoint, NotificationGroup>();

        Set<NotificationEndpoint> regIDForEntityTypeSet;
        final Map<NotificationEndpoint, FilterChain<BaseNotification>> tmpDestToFilter;
        synchronized (regIDForEntityTypeSet) {
			regIDForEntityTypeSet = this.entityTypeToReg.get(entityType);			
    	    if (regIDForEntityTypeSet==null) {
	            return rtn;
    	    }
			regIDForEntityTypeSet = new HashSet<NotificationEndpoint>(regIDForEntityTypeSet);
        }
        synchronized (destToFilter) {
            tmpDestToFilter = new HashMap<NotificationEndpoint, FilterChain<BaseNotification>>(destToFilter);
		}
        for(final NotificationEndpoint endpoint:regIDForEntityTypeSet) {
            final FilterChain<BaseNotification> filterChain = tmpDestToFilter.get(endpoint);
			List<? extends BaseNotification> filteredNS = null;
			filteredNS = ((List<? extends BaseNotification>) filterChain.filter(ns));
            if (filteredNS!=null && !filteredNS.isEmpty()) {

                NotificationGroup nsGrp = rtn.get(endpoint);
                if (nsGrp == null) {
                	nsGrp = new NotificationGroup(endpoint, filteredNS);
                    rtn.put(endpoint, nsGrp);
                }
                nsGrp.addAll(filteredNS);
            }
        }
        Set<BaseNotification> set = new HashSet<BaseNotification>();
        for (Entry<NotificationEndpoint, NotificationGroup> entry : rtn.entrySet()) {
			NotificationGroup ng = entry.getValue();
			Collection<BaseNotification> values = ng.getNotifications();
            for (BaseNotification v : values) {
                boolean added = set.add(v);
                if (!added) {
                    log.debug(v);
                }
            }
        }
        return rtn;
    }

}
