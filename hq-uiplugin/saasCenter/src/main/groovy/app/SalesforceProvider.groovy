/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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