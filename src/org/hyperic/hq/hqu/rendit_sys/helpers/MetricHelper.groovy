package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.hq.measurement.server.session.MeasurementTemplateSortField

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.HasAuthzOperations
import org.hyperic.util.pager.PageControl

class MetricHelper extends BaseHelper {
    private tmplMan = TemplateManagerEJBImpl.one
    private measMan = DerivedMeasurementManagerEJBImpl.one
    
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
}
