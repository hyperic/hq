import java.text.DateFormat
import org.hyperic.util.units.FormatSpecifics
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController

class SystemsdownController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    
    def SystemsdownController() {
        setTemplate('standard')
    }
    
    def getNow() {
        System.currentTimeMillis()
    }

    def index(params) {
    }

    def data(params) {
    }
}
