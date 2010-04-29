class SalesforceProvider extends CloudProvider {
                         
    /*
     * SFDC Metrics
     */
    private SFDC_RT        = new MetricName('Salesforce.com', 'Response Time')
    
    private PERF_METRICS_SFDC  = [ 
                                 new PerformanceMetric(metric: SFDC_RT,
                                     label: 'Response Time')
                                 ]
    
    /*
     * Web Login Metrics
     */
    private WEB_LOGIN_RT   = new MetricName('Web Login', 'Login Response Time')
    
    private PERF_METRICS_WEB_LOGIN = [
                                 new PerformanceMetric(metric: WEB_LOGIN_RT,  
                                     label: 'Login Response Time')
                                 ]
    
    /*
     * Web to Lead Metrics
     */
    private WEB_TO_LEAD_RT = new MetricName('Web-to-Lead', 'Response Time')
    private PERF_METRICS_WTL  = [
                                new PerformanceMetric(metric: WEB_TO_LEAD_RT,
                                        label:'Response Time')
                               ]
    
    /*
     * Web to Case Metrics
     */
    private WEB_TO_CASE_RT = new MetricName('Web-to-Case', 'Response Time')
    private PERF_METRICS_WTC  = [
                                new PerformanceMetric(metric: WEB_TO_CASE_RT,
                                        label:'Response Time')
                               ]
    
    /*
     * API Metrics
     */
    private API_LOGIN_RT   = new MetricName('Salesforce Web Services API', 'Login Response Time')
    private API_SERVER_RT  = new MetricName('Salesforce Web Services API', 'GetServerTimestamp Response Time')
    private PERF_METRICS_API  = [
                                 new PerformanceMetric(metric: API_LOGIN_RT, 
                                         label: 'Login Response Time'),
                                 new PerformanceMetric(metric: API_SERVER_RT,
                                         label: 'GetServerTimestamp Response Time')
                                ]

    /*
     * API Query Metrics
     */
    private QUERY_LOGIN_RT = new MetricName('Query', 'Login Response Time') 
    private QUERY_RT       = new MetricName('Query', 'Query Response Time')
    private QUERY_SIZE     = new MetricName('Query', 'Query Size')
    private PERF_METRICS_QUERY  = [
                                   new PerformanceMetric(metric: QUERY_LOGIN_RT,
                                           label: 'Login Response Time'),
                                   new PerformanceMetric(metric: QUERY_RT,
                                           label: 'Query Response Time'),
                                   new PerformanceMetric(metric: QUERY_SIZE,
                                           label: 'Query Size')
                                  ]
    
    SalesforceProvider() {
        code     = 'Salesforce.com' 
        longName = 'Salesforce.com'
        services = [
                    new CloudService (longName: 'Salesforce.com', code: 'Salesforce.com', 
                            performanceMetrics: PERF_METRICS_SFDC),
                    new CloudService (longName: 'Web Login', code: 'Web Login', 
                            performanceMetrics: PERF_METRICS_WEB_LOGIN),
                    new CloudService (longName: 'Web to Lead', code: 'Web-to-Lead', 
                            performanceMetrics: PERF_METRICS_WTL),
                    new CloudService (longName: 'Web to Case', code: 'Web-to-Case', 
                            performanceMetrics: PERF_METRICS_WTC),
                    new CloudService (longName: 'Web Service API', code: 'Salesforce Web Services API', 
                            performanceMetrics: PERF_METRICS_API),
                    new CloudService (longName: 'Query', code: 'Salesforce Web Services API - Query', 
                            performanceMetrics: PERF_METRICS_QUERY)
        ]

        indicators = [
                    new DashboardIndicator(metric: WEB_LOGIN_RT, label: 'Web Login Response Time'),
                    new DashboardIndicator(metric: API_LOGIN_RT, label: 'API Response Time'),
        ]
    }
}