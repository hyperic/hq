/**
 * Used by CloudProviders to declare which metrics they want to have
 * displayed on the dashboard.
 * 
 * @see CloudProvider#getIndicatorCharts()
 */
class DashboardIndicator {
    MetricName metric
    String     label
}