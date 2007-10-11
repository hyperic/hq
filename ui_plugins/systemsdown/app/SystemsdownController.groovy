import java.util.Locale
import org.hyperic.util.units.FormatSpecifics
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController

class SystemsdownController extends BaseController {
   def SystemsdownController() {
        setTemplate('standard')
    }
    
    def formatDuration(d) {
        return UnitsFormat.format(new UnitNumber(d, UnitsConstants.UNIT_DURATION,
                                                 UnitsConstants.SCALE_MILLI),
                                  Locale.getDefault(), null)
    }

    def index(params) {
    }

    def data(params) {
        def json = "{\n"
        def downs = resourceHelper.downResources
        downs.entrySet().each { entry ->
            json += '"' + entry.value.name + '" : "' +
                    formatDuration(entry.key.duration) + '",' + "\n"
        }
        json += "\n}"
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
