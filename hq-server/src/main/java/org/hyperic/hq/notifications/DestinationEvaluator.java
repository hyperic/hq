package org.hyperic.hq.notifications;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.hyperic.hq.notifications.filtering.FilterChain;
import org.hyperic.hq.notifications.filtering.IFilter;

public abstract class DestinationEvaluator<T> {
    protected Map<Destination,FilterChain<T>> destToFilter = new HashMap<Destination,FilterChain<T>>();

    protected abstract FilterChain<T> instantiateFilterChain(Collection<IFilter<T>> filters);
    
    /**
     * append filters
     * 
     * @param dest
     * @param filters
     */
    public void register(Destination dest, Collection<IFilter<T>> filters) {
        FilterChain<T> filterChain = this.destToFilter.get(dest);
        if (filterChain==null) {
            filterChain = instantiateFilterChain(filters);
            this.destToFilter.put(dest,filterChain);
        } else {
            filterChain.addAll(filters);
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
