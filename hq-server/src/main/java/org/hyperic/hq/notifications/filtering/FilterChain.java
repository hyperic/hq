package org.hyperic.hq.notifications.filtering;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * currently behave as the collection object passed to it in the c'tor
 * 
 * @author yakarn
 *
 */
public class FilterChain<T> extends AbstractCollection<IFilter<T>> {
    protected Collection<IFilter<T>> filters;
     
    public FilterChain(Collection<IFilter<T>> filters) {
        this.addAll(filters);
    }
    @Override
    public boolean add(IFilter<T> filter) {
        return this.filters.add(filter);
    }
    @Override
    public Iterator<IFilter<T>> iterator() {
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
    public boolean addAll(Collection<? extends IFilter<T>> c) {
        // TODO impose one filter policy per entity type
        return super.addAll(c);
    }
    public Collection<T> filter(final Collection<T> entities) {
        Collection<T> filteredEntities = entities;
        if (this.filters==null) {
            return filteredEntities;
        }
        for(IFilter<T> filter:this.filters) {
            filteredEntities = filter.filter(filteredEntities);
        }
        return filteredEntities;
        
    }
    @Override
    public String toString() {
        return super.toString();
        // TODO print internal filters list and see debug logs that prints this class are fine
    }
}