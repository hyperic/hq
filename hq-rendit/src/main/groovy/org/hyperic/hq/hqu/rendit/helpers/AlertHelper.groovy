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

package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.events.server.session.AlertDefSortField
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.galerts.server.session.GalertManagerImpl
import org.hyperic.hq.events.AlertSeverity

class AlertHelper extends BaseHelper {
    private alertMan  = Bootstrap.getBean(AlertManager.class)
    private galertMan = Bootstrap.getBean(GalertManager.class)
    private defMan    = Bootstrap.getBean(AlertDefinitionManager.class)
    
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
                   boolean escOnly, boolean notFixed, Integer groupId,
                   PageInfo pInfo) 
    {
        alertMan.findAlerts(user.id, severity.code, timeRange, endTime, escOnly,
                            notFixed, groupId, pInfo)
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
                        boolean escOnly, boolean notFixed, Integer groupId,
                        PageInfo pInfo) 
    {
        galertMan.findAlerts(user, severity, timeRange, endTime, escOnly,
                             notFixed, groupId, pInfo)
    }
    
    /**
     * Finds all recent alerts with at least the specified severity level.
     *
     * @see findGroupAlerts(AlertSeverity, long, long, PageInfo)
     */
    def findGroupAlerts(AlertSeverity severity, PageInfo pInfo) {
        long millis = System.currentTimeMillis()
        galertMan.findAlerts(user, severity, millis, millis, pInfo)
    }
    
    /**
     * Finds all alert definitions.  
     * 
     * @param severity Minimum severity for returned defs
     * @param enabled  If non-null, specifies whether the returned defs should
     *                 be enabled or disabled
     * @param excludeTypeBased  Exclude alert-defs created from a type-based
     *                          template
     * @param pInfo Paging information.  The sort field must be a value from
     *              {@link AlertDefSortField}
     */
    def findDefinitions(AlertSeverity severity, Boolean enabled,
                        boolean excludeTypeBased, PageInfo pInfo) 
    {
        defMan.findAlertDefinitions(user, severity, enabled, 
                                    excludeTypeBased, pInfo)
    }
     
    def findDefinitions(AlertSeverity severity, Boolean enabled,
                        boolean excludeTypeBased) 
    {
        findDefinitions(severity, enabled, excludeTypeBased, 
                        PageInfo.getAll(AlertDefSortField.NAME, true))
    }

    /**
     * Find type-based alert definitions.  These are the templates for
     * the individual resource definitions.
     *
     * @param enabled If non-null, specifies whether the returned defs
     *                should be enabled or disabled.
     * @param pInfo Paging information, where the sort field is a value from
     *              {@link AlertDefSortField}
     */
    def findTypeBasedDefinitions(Boolean enabled, PageInfo pInfo) { 
        defMan.findTypeBasedDefinitions(user, enabled, pInfo) 
    }

    /**
     * Find all type based alert definitions.
     */
    def findTypeBasedDefinitions() {
        defMan.findTypeBasedDefinitions(user, null, 
                                        PageInfo.getAll(AlertDefSortField.NAME,
                                                        true))
    }
    
    /**
     * Find group alert definitions.
     *
     * @param minSeverity Minimum severity for returned definitions
     * @param enabled     If non-null, specifies whether the returned defs 
     *                    should be enabled or disabled.
     * @param pInfo       Pageing information where the sort field is one
     *                    of {@link GalertDefSortField}
     */
    def findGroupDefinitions(AlertSeverity minSeverity,
                             Boolean enabled, PageInfo pInfo)
    {
        galertMan.findAlertDefs(user, minSeverity, enabled, pInfo)
    }

    /**
     * Delete an Alert Definition.
     * @deprecated Use AlertDefinitionCategory.delete() instead.
     */
    def deleteDefinition(AlertDefinition definition) {
        defMan.deleteAlertDefinitions(user, [ definition.id ] as Integer[])
    } 

    /**
     * Get an AlertDefinition by id.
     * @return The AlertDefinition with the given id, or null if it's not
     * found or has been deleted.
     */
    def getById(Integer id) {
        def definition = defMan.getByIdAndCheck(user, id)
        if (definition?.deleted) {
            return null
        }
        return definition
    }
}
