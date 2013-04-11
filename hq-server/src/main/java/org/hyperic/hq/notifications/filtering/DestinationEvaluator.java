package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.DefaultEndpoint;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.hyperic.hq.notifications.model.BaseNotification;

public abstract class DestinationEvaluator<N extends BaseNotification> {
    private final Log log = LogFactory.getLog(DestinationEvaluator.class);
    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another,
    // then sync between them), o/w will pose problems in scale
    protected Map<NotificationEndpoint,FilterChain<N>> destToFilter = new HashMap<NotificationEndpoint,FilterChain<N>>();

    protected abstract FilterChain<N> instantiateFilterChain(Collection<Filter<N,? extends FilteringCondition<?>>> filters);
    
    /**
     * append filters
     * 
     * @param dest
     * @param filters
     */
    public void register(NotificationEndpoint endpoint, Collection<Filter<N,? extends FilteringCondition<?>>> filters) {
        if (filters == null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be registered with destination " + endpoint);
            }
        }
        synchronized (destToFilter) {
            FilterChain<N> filterChain = destToFilter.get(endpoint);
            if (filterChain==null) {
                filterChain = instantiateFilterChain(filters);
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
    }

    /**
     * unregister all filters assigned to this destination
     * @param dest
     */
    public void unregisterAll(NotificationEndpoint endpoint) {
        FilterChain<N> filterChain = null;
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
    }

    public FilterChain<N> getRegistration(String registrationID) {
        FilterChain<N> filterChain = destToFilter.get(new DefaultEndpoint(registrationID));
        return filterChain;
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

    public Map<NotificationEndpoint, Collection<N>> evaluate(List<N> entities) {
        final Map<NotificationEndpoint, Collection<N>> rtn = new HashMap<NotificationEndpoint, Collection<N>>();
        final Map<NotificationEndpoint, FilterChain<N>> tmp;
        synchronized (destToFilter) {
            tmp = new HashMap<NotificationEndpoint, FilterChain<N>>(destToFilter);
        }
        for (final Entry<NotificationEndpoint,FilterChain<N>> entry : tmp.entrySet()) {
            final FilterChain<N> filterChain = entry.getValue();
            final Collection<N> filteredEntities = ((Collection<N>) filterChain.filter(entities));
            if (filteredEntities != null && !filteredEntities.isEmpty()) {
                final NotificationEndpoint endpoint = entry.getKey();
                Collection<N> list = rtn.get(endpoint);
                if (list == null) {
                    list = new ArrayList<N>();
                    rtn.put(endpoint, list);
                }
                list.addAll(filteredEntities);
            }
        }
        Set<N> set = new HashSet<N>();
        for (Entry<NotificationEndpoint, Collection<N>> entry : rtn.entrySet()) {
            Collection<N> values = entry.getValue();
            for (N v : values) {
                boolean added = set.add(v);
                if (!added) {
                    log.debug(v);
                }
            }
        }
        return rtn;
    }

}
