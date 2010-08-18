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

class AWSProvider extends CloudProvider {

    /**
     * S3 Metrics Get & Put
     */
    private S3_READ   = new MetricName('AWS S3 Bucket', 'Get Throughput Per Second')
    private S3_WRITE  = new MetricName('AWS S3 Bucket', 'Put Throughput Per Second')
    
    private PERF_METRICS_S3  = [
                                new PerformanceMetric(metric: S3_READ,  
                                        label: 'Get Throughput per Second'),
                                new PerformanceMetric(metric: S3_WRITE, 
                                        label: 'Put Throughput per Second')
                                ]

    /*
     * SDB Metrics 
     */
    private SDB_TIME   = new MetricName('AWS Simple DB','Query Response Time')
    private SDB_HOURS  = new MetricName('AWS Simple DB','Query Cpu Box Hours')
    
    private PERF_METRICS_SDB = [
                                new PerformanceMetric(metric: SDB_TIME,  
                                        label: 'Query Response Time'),
                                new PerformanceMetric(metric: SDB_HOURS, 
                                        label: 'Query Cpu Box Hours')
                                ]
    
    /*
     * API Metrics
     */
    private API_CALLS     = new MetricName('API Server', 'Total Calls')
    private API_FAILS     = new MetricName('API Server', 'Failed Calls')
    private API_FAILS_5   = new MetricName('API Server', 'Failures 5 Min')
    private API_FAILS_10  = new MetricName('API Server', 'Failures 10 Min')
    private API_FAILS_30  = new MetricName('API Server', 'Failures 30 Min')
    
    private PERF_METRICS_API = [
                                new PerformanceMetric(metric: API_CALLS, 
                                        label:"API Calls"),
                                new PerformanceMetric(metric: API_FAILS, 
                                        label:"API Call Failures"),
                                new PerformanceMetric(metric: API_FAILS_5, 
                                        label:"API Call Failures 5 Min"),
                                new PerformanceMetric(metric: API_FAILS_10, 
                                        label:"API Call Failures 10 Min"),
                                new PerformanceMetric(metric: API_FAILS_30, 
                                        label:"API Call Failures 30 Min")
                           ]
    AWSProvider() {
        code     = 'AWS' 
        longName = 'Amazon Web Services'

        services = [
            new CloudService(longName:'Simple Storage Service', code:'AWS S3 Bucket',
                    performanceMetrics: PERF_METRICS_S3),
            new CloudService(longName:'Simple DB', code:'AWS Simple DB',
                    performanceMetrics: PERF_METRICS_SDB),
            new CloudService(longName:'AWS API', code:'API Server',
                    performanceMetrics: PERF_METRICS_API),
            new CloudService(longName:'Elastic Compute Cloud', code:'AWS Availability Zone'),
            
            new CloudService(longName:'EC2 AMI', code:'AMI Instance')
        ]
        
        indicators = [
            new DashboardIndicator(metric: API_FAILS, 
                    label: 'API Call Failures'),
            new DashboardIndicator(metric: S3_READ, 
                    label: 'S3 Get Throughput Per Second'),
            new DashboardIndicator(metric: S3_WRITE, 
                    label: 'S3 Put Throughput Per Second')
        ]
    }
}