import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl as DMM
import org.hyperic.util.PrintfFormat
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.sigar.cmd.Free
import org.hyperic.sigar.Sigar
import org.hyperic.sigar.CpuPerc
import org.hyperic.hq.common.DiagnosticThread
import org.hyperic.hq.common.Humidor

import net.sf.ehcache.CacheManager

import java.text.SimpleDateFormat;

class HealthController 
	extends BaseController
{
    def HealthController() {
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
        setJSONMethods(['getSystemStats', 'getDiag', 'cacheData'])
    }
    
    private getCacheSchema() {
        def regionCol = new CacheColumn('region', 'Region', true)
        def sizeCol   = new CacheColumn('size',   'Size',   true)
        def hitsCol   = new CacheColumn('hits',   'Hits',   true)
        def missCol   = new CacheColumn('misses', 'Misses', true)
        
        def globalId = 0
        [
            getData: {pageInfo, params ->
                getCacheData(pageInfo)
            },
            defaultSort: regionCol,
            defaultSortOrder: 1,  // descending
            rowId: {globalId++},
            styleClass: {(it.misses <= it.size) ? null : "red"},
            columns: [
                [field:  regionCol,
                 width:  '50%',
                 label:  {it.region}],
                [field:  sizeCol,
                 width:  '10%',
                 label:  {"${it.size}"}],
                [field:  hitsCol,
                 width:  '10%',
                 label:  {"${it.hits}"}],
                [field:  missCol,
                 width:  '10%',
                 label:  {"${it.misses}"}],
            ],
        ]
    }
    
	private getCacheData(pageInfo) {
	    def cm = CacheManager.instance
	    def res = []
	               
	    for (name in cm.cacheNames) {
	        def cache = cm.getCache(name)
	        res << [region: name,
	                size:   cache.size,
	                hits:   cache.hitCount,
	                misses: cache.missCountNotFound]
	    }
	    
	    def d = pageInfo.sort.description
	    res = res.sort {a, b ->
	        return a."${d}" <=> b."${d}"
	    }
	    if (!pageInfo.ascending) 
	        res = res.reverse()
	    
	    // XXX:  This is still incorrect
	    def startIdx = pageInfo.startRow
	    def endIdx   = startIdx + pageInfo.pageSize
	    if (endIdx >= res.size)
	        endIdx = -1
	    return res[startIdx..endIdx]
    }

	def index(params) {
    	render(locals:[ diags: DiagnosticThread.diagnosticObjects,
    	                cacheSchema: cacheSchema,
    	                metricsPerMinute: metricsPerMinute])
    }
    
	private getMetricsPerMinute() {
	    def vals  = DMM.one.findMetricCountSummaries()
	    def total = 0.0
	    
	    for (v in vals) {
	        total = total + (float)v.total / (float)v.interval
	    }
	    (int)total
	}
	
    def getDiag(params) {
        def diagName = params.getOne('diag')
        for (d in DiagnosticThread.diagnosticObjects) {
            if (d.shortName == diagName) {
                return [diagData: HtmlUtil.escapeHtml(d.status)]
            }
        }
    }
    
    def cacheData(params) {
        DojoUtil.processTableRequest(cacheSchema, params)
    }

    private formatBytes(b) {
        UnitsFormat.format(new UnitNumber(b, UnitsConstants.UNIT_BYTES,
                                          UnitsConstants.SCALE_NONE),
                           locale, null).toString()
    }
    
    def getSystemStats(params) {
        def s = Humidor.instance.sigar
        def loadAvgFmt = new PrintfFormat('%.2f')
        def dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm a");
        
        def cpu      = s.cpuPerc
        def sysMem   = s.mem
        def sysSwap  = s.swap
        def pid      = s.pid
        def procFds  = 'unknown'
        def procMem  = s.getProcMem(pid)
        def procCpu  = s.getProcCpu(pid)
        def procTime = s.getProcTime(pid)
        def loadAvg  = s.loadAverage
        def runtime  = Runtime.runtime
            
        try {
            procFds = s.getProcFd(pid).total
        } catch(Exception e) {
        }
            
        return [sysUserCpu:    (int)(cpu.user * 100),
                sysSysCpu:     (int)(cpu.sys * 100),
                sysNiceCpu:    (int)(cpu.nice * 100),
                sysIdleCpu:    (int)(cpu.idle * 100),
                sysWaitCpu:    (int)(cpu.wait * 100),
                sysPercCpu:    (int)(100 - cpu.idle * 100),
                loadAvg1:      loadAvgFmt.sprintf(loadAvg[0]),
                loadAvg5:      loadAvgFmt.sprintf(loadAvg[1]),
                loadAvg15:     loadAvgFmt.sprintf(loadAvg[2]),
                totalMem:      formatBytes(sysMem.total),
                usedMem:       formatBytes(sysMem.used),
                freeMem:       formatBytes(sysMem.free),
                percMem:       (int)(sysMem.used * 100 / sysMem.total),
                totalSwap:     formatBytes(sysSwap.total),
                usedSwap:      formatBytes(sysSwap.used),
                freeSwap:      formatBytes(sysSwap.free),
                percSwap:      (int)(sysSwap.used * 100 / sysSwap.total),
                pid:           pid,
                procStartTime: dateFormat.format(procTime.startTime),
                procOpenFds:   procFds,
                procMemSize:   formatBytes(procMem.size),
                procMemRes:    formatBytes(procMem.resident),
                procMemShare:  formatBytes(procMem.share),
                procCpu:       (int)(procCpu.percent * 100.0 / runtime.availableProcessors()),
                jvmTotalMem:   formatBytes(runtime.totalMemory()),
                jvmFreeMem:    formatBytes(runtime.freeMemory()),
                jvmMaxMem:     formatBytes(runtime.maxMemory()),
                jvmPercMem:    (int)((runtime.maxMemory() - runtime.freeMemory()) * 100 / runtime.maxMemory()),
        ]
    }
}
