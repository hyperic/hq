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

import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.bizapp.shared.MeasurementBoss
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateSortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.measurement.MeasurementConstants
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.util.pager.PageControl

class MetricHelper extends BaseHelper {
    private tmplMan = Bootstrap.getBean(TemplateManager.class)
    private measMan = Bootstrap.getBean(MeasurementManager.class)
    private measBoss = Bootstrap.getBean(MeasurementBoss.class)

    MetricHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * General purpose utility method for finding metrics and metric 
     * templates.  Note that these are the metadata for metrics, not the
     * actual metric data itself.
     *
     * Optional arguments:
     *    'user',      defaults to the current user {@link AuthzSubject} 
     *    'permCheck', defaults to true (check user permission)
     *
     *
     * To find all metric templates:     find all: 'templates'
     *    for a specific resource type:  find all: 'templates', resourceType: 'Linux' 
     *        or                      :  find all: 'templates', resourceType: 'regex:Win.*' 
     */
     def find(Map args) {
         args = args + [:]
         ['all', 'withPaging', 'resourceType', 'enabled', 'entity'].each {
             args.get(it, null)
         }
         args.get('user', user)
         args.get('permCheck', true)

         if (!args.permCheck && !args.user.isSuperUser()) {
             args.user = overlord
         }

         if (args.all == 'templates') {
             if (args.withPaging == null) {
                 args.withPaging =
                     PageInfo.getAll(MeasurementTemplateSortField.TEMPLATE_NAME,
                                     true) 
             }

             def filter = {it}
             def resourceType = args.resourceType
             if (resourceType && resourceType.startsWith('regex')) {
                 def regex = ~resourceType[6..-1]
                 
                 // XXX:  This does not page correctly!
                 return tmplMan.findTemplates(args.user, args.withPaging,
                                              args.enabled).grep {
                     it.monitorableType.name ==~ regex 
                 }
             } else if (resourceType) {
                 return tmplMan.findTemplatesByMonitorableType(args.user,
                                                               args.withPaging,
                                                               resourceType,
                                                               args.enabled)
             } else {
                 return tmplMan.findTemplates(args.user, args.withPaging,
                                              args.enabled)
             }
         } else if (args.all == 'metrics') {
             // XXX: This actually only finds the enabled measurements, need
             // to find all regardless of enablement
             return measMan.findMeasurements(args.user, args.entity, null,
                                             PageControl.PAGE_ALL)
         }
         
         throw new IllegalArgumentException("Unsupported find args")
     }

     MeasurementTemplate findTemplateById(int id) {
         tmplMan.getTemplate(id)
     }

     Measurement findMeasurementById(int id) {
         measMan.getMeasurement(id)
     }
     
     Map getMetricsSummary(List resources, long begin, long end) {
		def mgr = SessionManager.instance
		def sessionId = mgr.put(user)
		def aeids = []
       
		resources.each { r ->
       		aeids << r.entityId
		}
       
		return measBoss.findMetrics(sessionId,
           aeids as AppdefEntityID[],
           MeasurementConstants.FILTER_NONE,
           null,
           begin,
           end,
           false) 
     }

    /**
     * @deprecated Use MetricCategory.
     */
     def setDefaultInterval(int id, long interval) {
         Integer[] tmpls = new Integer[1]
         tmpls[0] = id
         tmplMan.updateTemplateDefaultInterval(user, tmpls, interval)
     }

    /**
     * @deprecated Use MetricCategory
     */
     def setDefaultIndicator(int id, boolean on) {
         def tmpl = findTemplateById(id)
         tmplMan.setDesignated(tmpl, on)
     }

    /**
     * @deprecated Use MetricCategory
     */
     def setDefaultOn(int id, boolean on) {
         Integer[] tmpls = new Integer[1]
         tmpls[0] = id
         tmplMan.setTemplateEnabledByDefault(user, tmpls, on)
     }

    /**
     * @deprecated Use MetricCategory
     */
     def enableMeasurement(Integer mId, Long interval) {
        measMan.enableMeasurement(user, mId, interval);
     }

    /**
     * @deprecated Use MetricCategory
     */
     def updateMeasurementInterval(Integer mId, Long interval) {
        measMan.updateMeasurementInterval(user, mId, interval);
     }
}
