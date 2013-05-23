package org.hyperic.hq.notifications.filtering;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.notifications.model.BaseNotification;

/**
 * currently behave as the collection object passed to it in the c'tor
 * 
 * @author yakarn
 *
 */
public class FilterChain<N extends BaseNotification> extends AbstractCollection<Filter<N,? extends FilteringCondition<?>>> {
    protected Collection<Filter<N,? extends FilteringCondition<?>>> filters;
     
    public FilterChain(Collection<Filter<N,? extends FilteringCondition<?>>> filters) {
        this.filters=filters;
    }
    @Override
    public boolean add(Filter<N,? extends FilteringCondition<?>> filter) {
        return this.filters.add(filter);
    }
    @Override
    public Iterator<Filter<N,? extends FilteringCondition<?>>> iterator() {
        if (this.filters==null) {
            return null;
        }
        return this.filters.iterator();
    }
    @Override
    public int size() {
        return this.filters==null?0:this.filters.size();
    }
    /**
     * impose one filter per entity type
     * 
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends Filter<N,? extends FilteringCondition<?>>> c) {
        // TODO impose one filter policy per entity type
        return super.addAll(c);
    }
    public Collection<? extends BaseNotification> filter(final List<N> entities) {
        List<? extends BaseNotification> filteredEntities = entities;
        if (this.filters==null) {
            return filteredEntities;
        }
        for(Filter<N,? extends FilteringCondition<?>> filter:this.filters) {
            filteredEntities = filter.filter(filteredEntities);
        }
        return filteredEntities;
        
    }

    @Override
    public String toString() {
        return filters.toString();
    }
}