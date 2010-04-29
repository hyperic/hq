/**
 * A list of metrics which a CloudProvider returns to be displayed in the
 * detailed performance screen. 
 * 
 * @see CloudProvider#getPerformanceMetrics()
 */
class PerformanceMetric {
    MetricName metric
    String     label
    
    /**
     * If style == 'skinny', make the chart thin and stretch over the entire
     * width.
     */
    String     style   // Optional.  Can be 'skinny'
}