import org.hyperic.hq.appdef.server.session.CPropResourceSortField
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitNumber

class NagiupController 
	extends BaseController
{
    private int globalId = 0
    
    private final NAGIUP_SCHEMA = [
        getData: {pageInfo, params ->
            def proto = resourceHelper.findResourcePrototype('Nagios Plugin') 
            AppdefBossEJBImpl.one.getServicesView(user, proto, 
                                                  'nagiosHost', 'Return Code',
                                                  pageInfo)
        },
        defaultSort: CPropResourceSortField.PROPERTY,
        defaultSortOrder: 1,  // descending
        rowId: {globalId++},
        columns: [
            [field:  CPropResourceSortField.PROPERTY, 
             width:  '10%',
             header: {localeBundle.Host},
             label:  {it.getCPropValue("nagiosHost")}],
            [field:  CPropResourceSortField.RESOURCE, 
             width:  '10%',
             header: {localeBundle.Service},
             label:{linkTo(it.getCPropValue("nagiosServiceDesc"),
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
