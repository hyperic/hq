import java.text.DateFormat
import java.util.Locale
import org.hyperic.util.units.FormatSpecifics
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.appdef.server.session.DownResSortField

class SystemsdownController extends BaseController {
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    
    private final SYSTEMSDOWN_SCHEMA = [
        getData: {pageInfo, params -> 
            resourceHelper.getDownResources(params.getOne('typeId'), pageInfo)
        },
        defaultSort: DownResSortField.DOWNTIME,
        defaultSortOrder: 1,  // descending
        columns: [
            [field:DownResSortField.RESOURCE, width:'45%',
             label:{linkTo(it.name,
                           [resource:it.resource.entityId])}],
            [field:DownResSortField.TYPE, width:'10%',
             label:{it.type}],
            [field:DownResSortField.SINCE, width:'25%',
             label:{df.format(it.timestamp)}],
            [field:DownResSortField.DOWNTIME, width:'20%',
             label:{formatDuration(it.duration)}],
        ]
    ]

   def SystemsdownController() {
        setTemplate('standard')
    }
    
    def getNow() {
        System.currentTimeMillis()
    }

    def formatDuration(d) {
        return UnitsFormat.format(new UnitNumber(d, UnitsConstants.UNIT_DURATION,
                                                 UnitsConstants.SCALE_MILLI),
                                  Locale.getDefault(), null).toString()
    }

    def index(params) {
    	render(locals:[ systemsDownSchema : SYSTEMSDOWN_SCHEMA, numRows : params.numRows])
    }

    def data(params) {
        def json = DojoUtil.processTableRequest(SYSTEMSDOWN_SCHEMA , params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }

    def getTypeJSON(type, count) {
        def json = ""
        if (type != null) {
            json += "{name: \"" + type.name + "\", id: \""
            json += type.appdefType + ":" + type.id + "\", count: " + count + "},\n"
        }
        return json
    }

    def summary(params) {
        def map = resourceHelper.downResourcesMap.entrySet()

        def json = "[\n"

        def appdefType = 1
        map.each { entry ->
            def list = entry.value

            if (list.size() > 0) {
                json += "{parent: \"" + entry.key + "\",\n" +
                        "id: " + appdefType + ",\n" +
                        "count: " + list.size() + ",\n" +
                        "children:[\n"
    
                def previous = null
                def count = 0
    
                list.each { type ->
                    if (previous == null || previous.name != type.name) {
                        json += getTypeJSON(previous, count)
                        previous = type
                        count = 0
                    }
    
                    count++
                }

                json += getTypeJSON(previous, count)
                json += "]\n},\n"
            }

            appdefType++
        }

        json += "]"
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
