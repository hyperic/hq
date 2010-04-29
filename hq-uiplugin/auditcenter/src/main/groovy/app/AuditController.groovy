import java.text.DateFormat
import org.hyperic.util.units.FormatSpecifics
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.common.server.session.AuditSortField
import org.hyperic.hq.common.server.session.AuditPurpose
import org.hyperic.hq.common.server.session.AuditImportance

class AuditController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    
    def AuditController() {
        setTemplate('standard')
    }
    
    def getNow() {
        System.currentTimeMillis()
    }
    
    private final AUDIT_SCHEMA = [
        getData: {pageInfo, params -> 
            def auditTime = params.getOne('auditTime', "${now}").toLong()
            auditHelper.findAudits(0, now, AuditImportance.LOW, null, null,
                                   null, pageInfo)
        },
        defaultSort: AuditSortField.START_TIME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:AuditSortField.START_TIME, width:'15%',
             label:{df.format(it.startTime)}],
            [field:AuditSortField.DURATION, width:'5%',
             label:{
                def dur = it.endTime - it.startTime
                def val = new UnitNumber(dur, UnitsConstants.UNIT_DURATION,
                                         UnitsConstants.SCALE_MILLI)
                UnitsFormat.format(val).toString() 
             }],
            [field:AuditSortField.SUBJECT, width:'10%',
             label:{it.subject.fullName}],
            [field:AuditSortField.RESOURCE, width:'29%',
             label:{it.resource.name}],
            [field:[getValue: {localeBundle.Message },
                    description:'message', sortable:false], width:'41%',
             label:{it.htmlMessage}],
        ]
    ]

    def index(params) {
    	render(locals:[ auditSchema : AUDIT_SCHEMA])
    }

    def data(params) {
        def json = DojoUtil.processTableRequest(AUDIT_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
