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
public class FilterChain<N extends BaseNotification> extends AbstractCollection<Filter<? extends N,? extends FilteringCondition<?>>> {
    protected Collection<Filter<? extends N,? extends FilteringCondition<?>>> filters;
    
    public FilterChain(Collection<? extends Filter<? extends N,? extends FilteringCondition<?>>> filters) {
        this.filters=(Collection<Filter<? extends N, ? extends FilteringCondition<?>>>) filters;
    }
    @Override
    public boolean add(Filter<? extends N,? extends FilteringCondition<?>> filter) {
        return this.filters.add(filter);
    }
    @Override
    public Iterator<Filter<? extends N,? extends FilteringCondition<?>>> iterator() {
        if (this.filters==null) {
            return null;
        }
        return (Iterator<Filter<? extends N, ? extends FilteringCondition<?>>>) this.filters.iterator();
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
    public boolean addAll(Collection<? extends Filter<? extends N,? extends FilteringCondition<?>>> c) {
        // TODO impose one filter policy per entity type
        return super.addAll(c);
    }
    public List<? extends N> filter(final List<? extends N> entities) {
        List<? extends N> filteredEntities = entities;
        if (this.filters==null) {
            return filteredEntities;
        }
        for(Filter<? extends N,? extends FilteringCondition<?>> filter:this.filters) {
            //TODO~ fix unchecked conversion
            filteredEntities = filter.filter((List) filteredEntities);
        }
        return filteredEntities;
        
    }
    @Override
    public String toString() {
        return super.toString();
        // TODO~ print internal filters list and see debug logs that prints this class are fine
    }
    public static void main(String[] args) throws Throwable {
        List<Number> l = null;
        List<? extends Number> l1 = null;
        List<Integer> l2 = null;
        l1=l;
    }//EOM 
}