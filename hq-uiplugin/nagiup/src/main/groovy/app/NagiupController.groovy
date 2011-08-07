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

import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.appdef.server.session.CPropResourceSortField
import org.hyperic.hq.bizapp.server.session.AppdefBossImpl
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.appdef.shared.ServiceManager
import org.hyperic.hq.appdef.shared.ServiceManager
import org.hyperic.hq.events.shared.EventLogManager
import org.hyperic.hq.bizapp.shared.MeasurementBoss
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.auth.shared.SessionManager

class NagiupController 
extends BaseController
{
    EventLogManager EventMan=Bootstrap.getBean(EventLogManager.class)
    ServiceManager ServicesMan=Bootstrap.getBean(ServiceManager.class)
    MeasurementBoss MeasMan=Bootstrap.getBean(MeasurementBoss.class)

    private int globalId = 0
    
    private final NAGIUP_SCHEMA = [
        getData: {pageInfo, params ->
            def services=new ArrayList();
            def proto = resourceHelper.findResourcePrototype('Nagios Plugin')
            def sessionId = SessionManager.instance.put(user)
            
            ServicesMan.getServicesByType(user, 'Nagios Plugin', true).each(){ it->
                def nagiosService=new NagiosService();
                def service=ServicesMan.getServiceById(it.id)

                EventMan.findLastLogs(proto).each() { event -> 
                    if(event.getResource().getInstanceId()==it.id) {
                        nagiosService.lastEvent=event; 
                    }
                }
                
                def mes=MeasMan.findAvailabilityMetric(sessionId,service.entityId)

                MeasMan.getLastMetricValue(sessionId,[mes],-1).each() { avail-> 
                    nagiosService.lastValue=avail
                }

                nagiosService.entityId=service.entityId
                nagiosService.platform=service.server.platform.name
                nagiosService.name=service.name.replace('nagios Plugin','').replace('localhost','').trim()+'"'
                services.add(nagiosService)
            }
            
            services
        },
        defaultSort: CPropResourceSortField.PROPERTY,
        defaultSortOrder: 1,  // descending
        rowId: {globalId++},
        columns: [
            [field:  CPropResourceSortField.PROPERTY, 
                width:  '10%',
                header: {localeBundle.Host},
                label:  {it.platform}],
            [field:  CPropResourceSortField.RESOURCE, 
                width:  '10%',
                header: {localeBundle.Service},
                label:{linkTo(it.name,
                        [resource:it.entityId])}],
            [field:  CPropResourceSortField.METRIC_VALUE, 
                width:  '10%',
                header: {localeBundle.Status},
                label:{
                    if (it.lastValue != null) {
                    	return getLocalizedPropertyForValue(it.lastValue.value)
                    }
                }],
            [field:  CPropResourceSortField.METRIC_TIMESTAMP, 
                width:  '10%',
                header: {localeBundle.LastCheck},
                label:{
                    if (it.lastValue != null) {
                        return "${it.lastValue.label}"
                    }
                }],
            [field: CPropResourceSortField.EVENT_LOG, 
                width:'20%',
                label:{
                    if (it.lastEvent) {
                        return "${it.lastEvent.detail}"
                    }
                }],
        ],
        styleClass: { 
            if (it.lastValue != null) {
            	return getStyleClassFromValue(it.lastValue.value)
            }
        }
    ]

    def NagiupController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index(params) {
    	render(locals:[nagiupSchema : NAGIUP_SCHEMA])
    }
    
    private formatDuration(d) {
        return UnitsFormat.format(new UnitNumber(d, UnitsConstants.UNIT_DURATION,
                UnitsConstants.SCALE_MILLI),
            locale, null).toString()
    }
    
    private getStyleClassFromValue(val){ 
    	if (val == 0.0) {
            return "statusBGOK"
        } else if (val == 1.0) {
            return "statusBGWARNING "
        } else if (val == 2.0) {
            return "statusBGCRITICAL"
        }else 
        return "statusBGUNKNOWN"
    }
    
    private getLocalizedPropertyForValue(val) {
    	if (val == 0.0) {
            return localeBundle.ok
        } else if (val == 1.0) {
            return localeBundle.warning
        } else if (val == 2.0) {
            return localeBundle.critical
        }else 
        return localeBundle.unknown
    }
    
    def data(params) {
        def json = DojoUtil.processTableRequest(NAGIUP_SCHEMA, params)
        render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}

class NagiosService{
    def lastEvent,lastValue,entityId,platform,name
}
