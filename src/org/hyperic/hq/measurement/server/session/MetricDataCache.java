package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.List;

import org.hyperic.hq.product.MetricValue;

public interface MetricDataCache {

    /**
     * Add MetricValues to the cache. This method checks the timestamp of
     * each MetricValue to be added to ensure it's not an older data point than
     * what is already cached.
     *
     * @param data The list of DataPoint objects representing each MetricValue.
     * @return The list of DataPoint objects added to the cache. Any DataPoints 
     *         older than what is already cached will NOT be contained in this 
     *         list.
     */
    Collection<DataPoint> bulkAdd(List<DataPoint> data);

    /**
     * Get a MetricValue from the cache.
     *
     * @param mid The measurement id.
     * @param timestamp The beginning of the cache window.
     * @return The MetricValue from the cache, or null if the element is not
     * found, or the item in the cache is stale.
     */
    MetricValue get(Integer mid, long timestamp);

    /**
     * Remove a MetricValue from cache
     * @param mid The measurement id to remove.
     */
    void remove(Integer mid);
    
    /**
     * Add a MetricValue to the cache. This method checks the timestamp of
     * the MetricValue to be added to ensure it's not an older data point than
     * what is already cached.
     * 
     * Each invocation of this method is synchronized internally, so consider 
     * using the {@link #bulkAdd(List) bulk add} for batch updates to the cache.
     *
     * @param mid The measurement id.
     * @param mval The MetricValue to store.
     * @return true if the MetricValue was added to the cache, false otherwise.
     */
    boolean add(Integer mid, MetricValue mval);

}