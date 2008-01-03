import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl as DMM
import org.hyperic.hq.bizapp.server.session.ProductBossEJBImpl as PB
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl as SCM
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl
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
import org.hyperic.util.jdbc.DBUtil

import java.text.DateFormat;
import java.sql.Connection
import java.sql.Types
import javax.sql.DataSource
import javax.naming.InitialContext

import groovy.sql.Sql

import net.sf.ehcache.CacheManager


class HealthController 
	extends BaseController
{
    def HealthController() {
        onlyAllowSuperUsers()
        setJSONMethods(['getSystemStats', 'getDiag', 'cacheData', 'runQuery'])
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

	private getDiagnostics() {
	    DiagnosticThread.diagnosticObjects.sort {a, b -> a.name <=> b.name }
	}
	
	def index(params) {
    	render(locals:[ 
    	    diags:             diagnostics,
    	    cacheSchema:       cacheSchema,
    	    metricsPerMinute:  metricsPerMinute,
    	    numPlatforms:      resourceHelper.find(count:'platforms'),
    	    numServers:        resourceHelper.find(count:'servers'),
    	    numServices:       resourceHelper.find(count:'services'),
    	    databaseQueries:   databaseQueries,
    	    jvmSupportsTraces: getJVMSupportsTraces() ])
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
        for (d in diagnostics) {
            if (d.shortName == diagName) {
                return [diagData: '<pre>' + d.status + '</pre>']
            }
        }
    }
    
    def cacheData(params) {
        DojoUtil.processTableRequest(cacheSchema, params)
    }

    private formatBytes(b) {
        if (b == -1)
            return 'unknown'
            
        UnitsFormat.format(new UnitNumber(b, UnitsConstants.UNIT_BYTES,
                                          UnitsConstants.SCALE_NONE),
                           locale, null).toString()
    }
    
    def getSystemStats(params) {
        def s = Humidor.instance.sigar
        def loadAvgFmt = new PrintfFormat('%.2f')
        def dateFormat = DateFormat.getDateTimeInstance()
        
        def cpu      = s.cpuPerc
        def sysMem   = s.mem
        def sysSwap  = s.swap
        def pid      = s.pid
        def procFds  = 'unknown'
        def procMem  = s.getProcMem(pid)
        def procCpu  = s.getProcCpu(pid)
        def procTime = s.getProcTime(pid)
        def NA       = 'N/A' //XXX localeBundle?
        def loadAvg1 = NA
        def loadAvg5 = NA
        def loadAvg15 = NA
        def runtime  = Runtime.runtime
            
        try {
            procFds = s.getProcFd(pid).total
        } catch(Exception e) {
        }

        try {
            def loadAvg = s.loadAverage
            loadAvg1  = loadAvgFmt.sprintf(loadAvg[0])
            loadAvg5  = loadAvgFmt.sprintf(loadAvg[1])
            loadAvg15 = loadAvgFmt.sprintf(loadAvg[2])
        } catch(Exception e) {
            //SigarNotImplementedException on Windows
        }

        return [sysUserCpu:    (int)(cpu.user * 100),
                sysSysCpu:     (int)(cpu.sys * 100),
                sysNiceCpu:    (int)(cpu.nice * 100),
                sysIdleCpu:    (int)(cpu.idle * 100),
                sysWaitCpu:    (int)(cpu.wait * 100),
                sysPercCpu:    (int)(100 - cpu.idle * 100),
                loadAvg1:      loadAvg1,
                loadAvg5:      loadAvg5,
                loadAvg15:     loadAvg15,
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
    
    private printReport(params) {
        def s = Humidor.instance.sigar
        def dateFormat = DateFormat.dateTimeInstance
        def cmdLine = s.getProcArgs('$$')
        def procEnv = s.getProcEnv('$$')
        
        def locals = [
            numCpu:           Runtime.runtime.availableProcessors(),
            fqdn:             s.getFQDN(),
            guid:             SCM.one.getGUID(),
            reportTime:       dateFormat.format(System.currentTimeMillis()),
            userName:         user.fullName,
            numAgents:        AgentManagerEJBImpl.one.agentCount,
            metricsPerMinute: metricsPerMinute,
            diagnostics:      diagnostics,
            hqVersion:        PB.one.version,
            buildNumber:      PB.one.buildNumber,
            jvmProps:         System.properties,
            schemaVersion:    SCM.one.config.getProperty('CAM_SCHEMA_VERSION'),
            cmdLine:          cmdLine,
            procEnv:          procEnv,
            cpuInfos:         s.cpuInfoList,
            jvmSupportsTraces: getJVMSupportsTraces(),
        ] + getSystemStats([:])
    	render(locals: locals)
    }
    
    private withConnection(Closure c) {
        def ctx  = new InitialContext()
        def ds   = ctx.lookup("java:/HypericDS")
        def conn
        
        try {
            conn = ds.connection
            return c.call(conn)
        } finally {
            if (conn != null) conn.close()
        }
    }
    
    private getDatabaseQueries() {
        def queries = [ 
          pgLocks: [ 
             name: localeBundle['queryPostgresLocks'], 
             viewable: {conn -> DBUtil.isPostgreSQL(conn) },          
             query: "select l.mode, transaction, l.granted, " + 
                    "now() - query_start as time, current_query " + 
                    "from pg_locks l, pg_stat_activity a " + 
                    "where l.pid=a.procpid " + 
                    " and now() - query_start > '00:00:01'"],
          pgStatActivity: [ 
             name: localeBundle['queryPostgresActivity'], 
             viewable: {conn -> DBUtil.isPostgreSQL(conn) },          
             query: "select * from pg_stat_activity " + 
                    "where current_query != '<IDLE>' order by query_start desc"],
          aiqPlatform: [ 
             name: localeBundle['queryAIQPlatform'], 
             query: "select * from EAM_AIQ_PLATFORM"], 
          aiqServer: [ 
             name: localeBundle['queryAIQServer'], 
             query: "select * from EAM_AIQ_SERVER"],
          aiqIP: [ 
             name: localeBundle['queryAIQIP'], 
             query: "select * from EAM_AIQ_IP"], 
        ]
        
        def res = [:]
        withConnection() { conn ->
            for (q in queries.keySet()) {
                def query = queries[q]
                if (!query.viewable || 
                    (query.viewable in Closure && query.viewable(conn))) 
                {
                    res[q] = query   
                }
            }
        }
        res
    }
    
    private h(str) {
        HtmlUtil.escapeHtml(str)
    }
    
    private getJVMSupportsTraces() {
        def ver = System.getProperty('java.version')[0..2]
        ver != '1.3' && ver != '1.4' 
    }
    
    def runQuery(params) {
        def id    = params.getOne('query')
        def query = databaseQueries[id].query
        def name  = databaseQueries[id].name
        def start = now()

        log.info("Running query [${query}]")
        def res = withConnection() { conn ->
            def sql    = new Sql(conn)
            def output = new StringBuffer()
            def rowIdx = 0
            def md


            sql.eachRow(query) { rs ->
                if (rowIdx++ == 0) {
                    output << "<table cellspadding=3 cellspacing=0 border=0 width=98%><thead><tr>"
                    md = rs.getMetaData()
                    for (i in 1..md.columnCount) {
                        output <<  "<td>${h md.getColumnLabel(i)}</td>"
                    }
                    output << "</tr></thead><tbody>"
                }
                output << "<tr>"
                for (i in 0..<md.columnCount) {
                    def type = md.getColumnType(i + 1)
                    if (type in [Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY]) {
                        output << "<td>*binary*</td>"
                    } else {
                       def trimmedCol = h(rs[i].toString().trim())
                        if (trimmedCol.length() == 0) {
                        output << "<td>&nbsp;</td>"
                        } else {
                         output << "<td>"
                         output << trimmedCol
                         output << "</td>"
                       }
                    }
                }
                output << "</tr>"
            }
            output << "</tbody></table>"
            if (!rowIdx) {
                return localeBundle['noResultsFound']
            } else {
                return output
            }
        }
        
        def queryData = "${name} executed in ${now() - start} ms<br/><br/>"
        [ queryData: queryData + res ]
    }
}
