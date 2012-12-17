package org.hyperic.hq.notifications;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.hyperic.hq.notifications.filtering.FilterChain;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.model.MetricNotification;

public abstract class DestinationEvaluator<T> {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Destination,FilterChain<T>> destToFilter = new ConcurrentHashMap<Destination,FilterChain<T>>();

    protected abstract FilterChain<T> instantiateFilterChain(Collection<Filter<T>> filters);
    
    /**
     * append filters
     * 
     * @param dest
     * @param filters
     */
    public void register(Destination dest, Collection<Filter<T>> filters) {
        if (filters==null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be registered with destination " + dest);
            }
        }
        FilterChain<T> filterChain = this.destToFilter.get(dest);
        if (filterChain==null) {
            filterChain = instantiateFilterChain(filters);
            this.destToFilter.put(dest,filterChain);
            
            if (log.isDebugEnabled()) {
                log.debug("registering the following filters to destination " + dest + " (no previous filters were assigned to it):\n" + filters);
            }
        } else {
            filterChain.addAll(filters);
            
            if (log.isDebugEnabled()) {
                log.debug("appending the following filters to destination " + dest + ":\n" + filters);
            }
        }
    }
    /**
     * unregister all filters assigned to this destination
     * @param dest
     */
    public void unregisterAll(Destination dest) {
        FilterChain<T> filterChain = this.destToFilter.remove(dest);
        if (log.isDebugEnabled()) {
            if (filterChain==null) {
                log.debug("no filters were previously registered with destination " + dest);
            } else {
                // TODO~ remove all filter chain filters from it
                log.debug("un-registering all previously regitered filters from destination " + dest + ":\n" + filterChain);
            }
        }
    }
    /**
     * 
     * @param dest
     * @param filters
     */
    public void unregister(Destination dest, List<Filter<MetricNotification>> filters) {
        if (filters==null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be un-registered from destination " + dest);
            }
            return;
        }
        FilterChain<T> filterChain = this.destToFilter.get(dest);
        if (filterChain==null) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were previously registered with destination " + dest);
            }
        } else {
            filterChain.removeAll(filters);
            if (log.isDebugEnabled()) {
                log.debug("un-registering the following filters from destination " + dest + ":\n" + filters);
            }
            if (filterChain.isEmpty()) {
                this.destToFilter.remove(dest);
                if (log.isDebugEnabled()) {
                    log.debug("un-registering the following destination " + dest);
                }
            }
        }
    }
    public List<ObjectMessage> evaluate(final List<T> entities) throws JMSException {
        List<ObjectMessage> msgs = new ArrayList<ObjectMessage>();
        Set<Entry<Destination,FilterChain<T>>> destToFilterESet = destToFilter.entrySet();
         
        for(Entry<Destination,FilterChain<T>> destToFilterE:destToFilterESet) {
            FilterChain<T> filterChain = destToFilterE.getValue();
            Collection<T> filteredEntities = ((Collection<T>) filterChain.filter(entities));
            if (filteredEntities!=null) {
                ObjectMessage msg = new DummyMsg();
                Destination dest = destToFilterE.getKey();
                msg.setJMSDestination(dest);
                msg.setObject((Serializable) filteredEntities);
                msgs.add(msg);
            }
        }
        return msgs;
    }
}
