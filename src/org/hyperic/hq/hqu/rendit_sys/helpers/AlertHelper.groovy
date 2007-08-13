package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl
import org.hyperic.hq.events.AlertSeverity

class AlertHelper extends BaseHelper {
    private alertMan  = AlertManagerEJBImpl.one
    private galertMan = GalertManagerEJBImpl.one
    private defMan    = AlertDefinitionManagerEJBImpl.one
    
    AlertHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find Alerts within a specified timerange, greater or equal to a 
     * given priority.
     *
     * @param severity  The minimum severity for the returned alerts
     * @param timeRange The range (in millis) prior to endTime which 
     *                  the resulting alerts will be contained in
     * @param endTime   Millis since the epoch specifying the end of the time
     *                  range that alerts will be returned for
     * @param pInfo     PageInfo that contains an AlertSortField for its
     *                  sort parameters
     */
    def findAlerts(AlertSeverity severity, long timeRange, long endTime, 
                   PageInfo pInfo) 
    {
        alertMan.findAlerts(user.id, severity.code, timeRange, endTime, pInfo)
    }
    
    /**
     * Finds all recent alerts with at least the specified severity level.
     *
     * @see findAlerts(AlertSeverity, long, long, PageInfo)
     */
    def findAlerts(AlertSeverity severity, PageInfo pInfo) {
        long millis = System.currentTimeMillis()
        alertMan.findAlerts(user.id, severity.code, millis, millis, pInfo)
    }
    
    /**
     * Find Group alerts within a specified timerange, greater or equal to a 
     * given priority.
     *
     * @param severity  The minimum severity for the returned alerts
     * @param timeRange The range (in millis) prior to endTime which 
     *                  the resulting alerts will be contained in
     * @param endTime   Millis since the epoch specifying the end of the time
     *                  range that alerts will be returned for
     * @param pInfo     PageInfo that contains a GalertLogSortField for its
     *                  sort parameters
     * @return a list of GalertLogs
     */
    def findGroupAlerts(AlertSeverity severity, long timeRange, long endTime, 
                        PageInfo pInfo) 
    {
        galertMan.findAlerts(userValue, severity, timeRange, endTime, pInfo)
    }
    
    /**
     * Finds all recent alerts with at least the specified severity level.
     *
     * @see findGroupAlerts(AlertSeverity, long, long, PageInfo)
     */
    def findGroupAlerts(AlertSeverity severity, PageInfo pInfo) {
        long millis = System.currentTimeMillis()
        galertMan.findAlerts(userValue, severity, millis, millis, pInfo)
    }
    
    def findDefinitions(AlertSeverity severity, Boolean enabled,
                        boolean simpleOnly, PageInfo pInfo) 
    {
        defMan.findAlertDefinitions(userValue, severity, enabled, simpleOnly, 
                                    pInfo)
    }
}
