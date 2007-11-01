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
            AppdefBossEJBImpl.one.getServicesView(user, 'Nagios Plugin', 
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
             label:  {it.getCPropValue()}],
            [field:  CPropResourceSortField.RESOURCE, 
             width:  '10%',
             header: {localeBundle.Service},
             label:  {it.resourceName}],
            [field:  CPropResourceSortField.METRIC_VALUE, 
             width:  '10%',
             header: {localeBundle.Status},
             label:{
                    if (it.lastValue != null) {
                        return "${it.lastValue.value}"
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
        ]
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
    
    def data(params) {
        def json = DojoUtil.processTableRequest(NAGIUP_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
