import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl as SCM
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.measurement.DataPurgeJob
import org.hyperic.hq.measurement.shared.DataCompressUtil

class ThrashController
	extends BaseController
{
    def ThrashController() {
        onlyAllowSuperUsers()
        setJSONMethods(['compressData', 'dbAnalyze', 'dbMaint', 
                        'purgeEventLogs'])
    }
    
    def index(params) {
    }

    def compressData(params) {
        def start = now()
        DataCompressUtil.localHome.create().compressData()
        [compressResult: "Completed in ${now() - start} ms"] 
    }
    
    def dbAnalyze(params) {
        def start = now()
        DataPurgeJob.runDBAnalyze(SCM.one)
        [analyzeResult: "Completed in ${now() - start} ms"]
    }
    
    def dbMaint(params) {
        def start = now()
        DataPurgeJob.runDBMaintenance(SCM.one)
        [maintResult: "Completed in ${now() - start} ms"]
    }
    
    def purgeEventLogs(params) {
        def start = now()
        DataPurgeJob.purgeEventLogs(SCM.one.config, now())
        [purgeResult: "Completed in ${now() - start} ms"]
    }
}
