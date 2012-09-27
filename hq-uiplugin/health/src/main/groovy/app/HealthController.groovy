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

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.AppdefEntityValue
import org.hyperic.hq.appdef.server.session.AgentSortField
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.appdef.Agent
import org.hyperic.util.PrintfFormat
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper 
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.util.HQUtil
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.common.Humidor
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.jdbc.DBUtil
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.measurement.shared.MeasurementManager

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager
import net.sf.ehcache.config.CacheConfiguration

import java.text.DateFormat;
import java.sql.Types
import javax.naming.InitialContext

import groovy.sql.Sql

class HealthController 
    extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private final PrintfFormat agentFmt =
        new PrintfFormat("%-25s %-15s %-5s %-9s %-17s %-13s %-16s %-10s %s")
    private cpropMan = Bootstrap.getBean(CPropManager.class)
	private measurementMan = Bootstrap.getBean(MeasurementManager.class)

    HealthController() {
        onlyAllowSuperUsers()
        setJSONMethods(['getSystemStats', 'getDiag', 'cacheData', 
                        'agentData', 'inventoryData', 'runQuery', 'executeQuery', 'executeMaintenanceOp', 'totalCacheSizeInBytes'])
    }

    boolean logRequests() {
        false
    }

    private getAgentSchema() {
        def res = [
            getData: {pageInfo, params ->
                getAgentData(pageInfo)
            },
            defaultSort: AgentSortField.CTIME,
            defaultSortOrder: 1,
            rowId: {it.agent.id},
            columns: [
                [field: [getValue: {localeBundle.fqdn},
                 description:'fqdn', sortable:false], 
                 width: '20%',
                 label: {it.platformHtml}],
                [field: AgentSortField.ADDR,
                 width: '15%',
                 label: {it.serverHtml}],
                [field: AgentSortField.PORT,
                 width: '5%',
                 label: {it.agent.port}],
                [field: AgentSortField.VERSION,
                 width: '10%',
                 label: {it.agent.version}],
                [field: [getValue: {localeBundle.bundleVersion},
                 description:'bundleVersion', sortable:false],
                 width: '10%',
                 label: {it.bundleVersion}],                 
                [field: AgentSortField.CTIME,
                 width: '18%',
                 label: {it.creationTime}],
                [field: [getValue: {localeBundle.numPlatforms},
                 description:'numPlatforms', sortable:false], 
                 width: '8%',
                 label: {it.agent.platforms.size()}],
                [field: [getValue: {localeBundle.numMetrics},
                 description:'numMetrics', sortable:false], 
                 width: '10%',
                 label: {it.numMetrics}],
                [field: [getValue: {localeBundle.timeOffset},
                 description:'timeOffset', sortable:false], 
                 width: '19%',
                 label: {it.offsetHtml}],
            ],
        ]   

        if (HQUtil.isEnterpriseEdition()) {
            res.columns << [
                field: [getValue: {localeBundle.licenseCount},
                description:'licenseCount', sortable:false],
                width: '10%',
                label: {it.licenseCount}]
        }
        
        res
    }
    
    private getAgentCProp(Agent a, Server s, String prop) {
        def overlord = Bootstrap.getBean(AuthzSubjectManager.class).overlordPojo
        def aev = new AppdefEntityValue(AppdefEntityID.newServerID(s.id), overlord)
        def cprop = "N/A"
        try {
            cprop = cpropMan.getValue(aev, prop)
        } catch (Exception e) {
            // cannot recover
        }
        cprop
    }
    
    private getLicenseCount(Agent a) {
        if (!HQUtil.isEnterpriseEdition())
            return ""
            
        def lman = Bootstrap.getBean(com.hyperic.hq.license.LicenseManager.class)
        "${lman.getCountPerAgent(a)}"
    }
    
    private getAgentData(pageInfo) {
        def res = []
        def agents     = agentHelper.find(withPaging: pageInfo)
        def offsetData = measurementMan.findAgentOffsetTuples()
        def metricData = measurementMan.findNumMetricsPerAgent()
        for (a in agents) {
            def found = false
            def numMetrics = 0
            
            if (metricData[a])
                numMetrics = metricData[a]
            for (d in offsetData) {
                if (d[0] == a) {
                    def metricVal = d[3].lastDataPoint?.value
                    if (metricVal == null)
                        metricVal = '?'
                    
                    res << [agent:a, 
                            platform:d[1].fqdn,
                            platformHtml:linkTo(d[1].fqdn, [resource:d[1].resource]),
                            server:a.address,
                            serverHtml:linkTo(a.address, [resource:d[2].resource]),
                            offset:metricVal,
                            offsetHtml:linkTo(metricVal, [resource:d[3]]), 
                            build:getAgentCProp(a, d[2], "build"),
                            bundleVersion:getAgentCProp(a, d[2], "AgentBundleVersion"),
                            numMetrics:numMetrics,
                            creationTime:df.format(a.creationTime),
                            licenseCount:getLicenseCount(a),
                            version:getAgentCProp(a, d[2], "version")]
                    found = true
                    break
                }
            }
            if (!found) {
                res << [agent:a, numMetrics:numMetrics,
                        platform:'Unknown', platformHtml:'Unknown',
                        server:a.address, serverHtml:a.address,
                        offset:'?', offsetHtml:'?', 
                        creationTime:df.format(a.creationTime),
                        licenseCount:getLicenseCount(a)]
            }
        }
        res
    }
    
    def agentData(params) {
        DojoUtil.processTableRequest(agentSchema, params)
    }
    
    private getCacheSchema() {
        def regionCol = new CacheColumn('region', 'Region', true)
        def sizeCol   = new CacheColumn('size',   'Size',   true)
        def hitsCol   = new CacheColumn('hits',   'Hits',   true)
        def missCol   = new CacheColumn('misses', 'Misses', true)
        def limitCol  = new CacheColumn('limit', 'Limit', true)
        
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
                 width:  '40%',
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
                [field:  limitCol,
                 width:  '10%',
                 label:  {"${it.limit}"}],
            ],
        ]
    }
    
    private getCacheData(pageInfo) {
        def res = getCacheHealths()
        
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
    
    private  getCacheHealths() {
        def manager = CacheManager.getInstance()
        def caches = manager.getCacheNames()
        List<Map<String,Object>> healths = new ArrayList<Map<String,Object>>(caches.size());
        for (Cache cacheName : caches ) {
            def cache = manager.getCache(cacheName)
            CacheConfiguration config = cache.getCacheConfiguration()
            def limit = config.getMaxElementsInMemory()
            Map<String,Object> health = new HashMap<String,Object>();
            health.put("region", cache.getName());
            health.put("limit", limit)
            health.put("size", new Integer(cache.getSize()));
            health.put("hits", new Integer(cache.getHitCount()));
            health.put("misses", new Integer(cache.getMissCountNotFound()));
            healths.add(health);
        }
        return healths;
    }

    private getDiagnostics() {
        Bootstrap.getBean(DiagnosticsLogger.class)
                 .diagnosticObjects.sort {a, b -> a.name <=> b.name }
    }
    
    def index(params) {
        render(locals:[ 
            diags:             diagnostics,
            cacheSchema:       cacheSchema,
            agentSchema:       agentSchema,
            inventorySchema:   inventorySchema,
            metricsPerMinute:  metricsPerMinute,
            numPlatforms:      resourceHelper.find(count:'platforms'),
            numCpus:   resourceHelper.find(count:'cpus'),
            
            numAgents:         agentHelper.find(count:'agents'),
            numActiveAgents: agentHelper.find(count:'activeAgents'),
            
            numServers:        resourceHelper.find(count:'servers'),
            numServices:       resourceHelper.find(count:'services'),
            numApplications:   resourceHelper.find(count:'applications'),
            numRoles:   resourceHelper.find(count:'roles'),
            numUsers:  resourceHelper.find(count:'users'),
            numAlertDefs:  resourceHelper.find(count:'alertDefs'),
            numResources:  resourceHelper.find(count:'resources'),
            numResourceTypes:  resourceHelper.find(count:'resourceTypes'),
            numGroups:  resourceHelper.find(count:'groups'),
            
            numEscalations:  resourceHelper.find(count:'escalations'),
            numActiveEscalations:  resourceHelper.find(count:'activeEscalations'),
           
            databaseQueries:   databaseQueries,
            databaseActions:   databaseActions,
            maintenanceOps:    maintenanceOps,
            jvmSupportsTraces: getJVMSupportsTraces() ])
    }
    
    private getMetricsPerMinute() {
		def vals  = measurementMan.findMetricCountSummaries()
        def total = 0.0
        
        for (v in vals) {
            total = total + (float)v.total / (float)v.interval
        }
        (int)total
    }

    private getInventorySchema() {
        def resourceTypeCol = new CacheColumn('resourceType', 'Resource Type', true)
        def totalCol   = new CacheColumn('total',   'Total',   true)
        
        def globalId = 0
        [
            getData: {pageInfo, params ->
                getInventoryData(pageInfo)
            },
            defaultSort: resourceTypeCol,
            defaultSortOrder: 1,  // descending
            rowId: {globalId++},
            columns: [
                [field:  resourceTypeCol,
                 width:  '40%',
                 label:  {it.name}],
                [field:  totalCol,
                 width:  '10%',
                 label:  {it.total}],
            ],
        ]

    }

    def inventoryData(params) {
        DojoUtil.processTableRequest(inventorySchema, params)
    }
    
    private getInventoryData(params) {
        def types = []
        resourceHelper.findAppdefPrototypes().each { p ->
            types.add(['name': p.name, 
                       'total': getResourceTypeCount(p)] )
        }
        return types
    }
            
    private getResourceTypeCount(p) {
        return resourceHelper.findByPrototype(['byPrototype': p.name]).size()
    }
    
    def getDiag(params) {
        def diagName = params.getOne('diag')
        for (d in diagnostics) {
            if (d.shortName == diagName) {
                return [diagData: '<pre>' + d.shortStatus + '</pre>']
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

    private formatMemoryUsage(b) {
        def kb
        if (b == -1) {
            return 'unknown'.padLeft(18)
        } else if (b == null) {
            return 'null'.padLeft(18)
        } else if (b > 0) {
            kb = (b / 1024)
        } else {
            kb = 0
        }
        def fkb = sprintf("%.2f", kb.toFloat())
        return fkb.padLeft(18)
    }
    
    private formatPercent(n, total) {
        if (total == 0)
           return 0
        return (int)(n * 100 / total)
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

        //e.g. Linux
        def free;
        def used;
        if ((sysMem.free != sysMem.actualFree ||
            (sysMem.used != sysMem.actualUsed))) {
            free = sysMem.actualFree
            used = sysMem.actualUsed
        } else {
            free = sysMem.free
            used = sysMem.used
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
                usedMem:       formatBytes(used),
                freeMem:       formatBytes(free),
                percMem:       formatPercent(used, sysMem.total),
                totalSwap:     formatBytes(sysSwap.total),
                usedSwap:      formatBytes(sysSwap.used),
                freeSwap:      formatBytes(sysSwap.free),
                percSwap:      formatPercent(sysSwap.used, sysSwap.total),
                pid:           pid,
                procStartTime: dateFormat.format(procTime.startTime),
                procOpenFds:   procFds,
                procMemSize:   formatBytes(procMem.size),
                procMemRes:    formatBytes(procMem.resident),
                procMemShare:  formatBytes(procMem.share),
                procCpu:       formatPercent(procCpu.percent, runtime.availableProcessors()),
                jvmTotalMem:   formatBytes(runtime.totalMemory()),
                jvmFreeMem:    formatBytes(runtime.freeMemory()),
                jvmMaxMem:     formatBytes(runtime.maxMemory()),
                jvmPercMem:    formatPercent(runtime.maxMemory() - runtime.freeMemory(), runtime.maxMemory()),
        ]
    }
    
    def printReport(params) {
        def s = Humidor.instance.sigar
        def dateFormat  = DateFormat.dateTimeInstance
        def cmdLine     = s.getProcArgs('$$')
        def procEnv     = s.getProcEnv('$$')
        def agentPager  = PageInfo.getAll(AgentSortField.ADDR, true) 
        def inventoryPager  = PageInfo.getAll(ResourceSortField.NAME, true)
        
        def locals = [
            numCpu:           Runtime.runtime.availableProcessors(),
            fqdn:             s.getFQDN(),
            guid:             Bootstrap.getBean(ServerConfigManager.class).getGUID(),
            dbVersion:        runQueryAsText('version'),
            dbCharacterSet:   runQueryAsText('dbCharacterSet'),
            reportTime:       dateFormat.format(System.currentTimeMillis()),
            userName:         user.fullName,
            numPlatforms:     resourceHelper.find(count:'platforms'),
            numCpus:          resourceHelper.find(count:'cpus'),            
            numAgents:        agentHelper.find(count:'agents'),
            numActiveAgents:  agentHelper.find(count:'activeAgents'),            
            numServers:       resourceHelper.find(count:'servers'),
            numServices:      resourceHelper.find(count:'services'),
            numApplications:  resourceHelper.find(count:'applications'),
            numRoles:         resourceHelper.find(count:'roles'),
            numUsers:         resourceHelper.find(count:'users'),
            numAlertDefs:     resourceHelper.find(count:'alertDefs'),
            numResources:     resourceHelper.find(count:'resources'),
            numResourceTypes: resourceHelper.find(count:'resourceTypes'),
            numGroups:        resourceHelper.find(count:'groups'),            
            numEscalations:   resourceHelper.find(count:'escalations'),
            numActiveEscalations:  resourceHelper.find(count:'activeEscalations'),           
            metricsPerMinute: metricsPerMinute,
            diagnostics:      diagnostics,
            hqVersion:        Bootstrap.getBean(ProductBoss.class).version,
            jvmProps:         System.properties,
            schemaVersion:    Bootstrap.getBean(ServerConfigManager.class).config.getProperty('CAM_SCHEMA_VERSION'),
            cmdLine:          cmdLine,
            procEnv:          procEnv,
            cpuInfos:         s.cpuInfoList,
            jvmSupportsTraces: getJVMSupportsTraces(),
            agentData:        getAgentData(agentPager),
            inventoryData:    getInventoryData(),
            agentFmt:         agentFmt,
            AgentSortField:   AgentSortField,
            licenseInfo:      [:],
            cacheHealths:     getCacheHealths(),
            dbQueries:        runAllOrphanQueries(),
            orphanedNodes:    _findOrphanedData(params),
        ] + getSystemStats([:]) 
        
        if (HQUtil.isEnterpriseEdition()) {
            locals.licenseInfo = Bootstrap.getBean(com.hyperic.hq.license.LicenseManager.class).licenseInfo
        }
        
        render(locals: locals)
    }
	
	private def _findOrphanedData(params) {
		def overlord = HQUtil.getOverlord()
		def rHelp = new ResourceHelper(overlord)
		 
		def orphans = []
		 
		def servers = rHelp.findAllServers()
		 
		servers.each { s ->
			def server = s.toServer()
			if (server.getPlatform() == null) {
				orphans << ['id': server.id, 'type':'Server', 'name': server.name, 'obj': s, 'overlord': overlord]
			}
		}
		 
		def services = rHelp.findAllServices()
		 
		services.each { s ->
			def service = s.toService()
			if (service.getServer() == null) {
				orphans << ['id': service.id, 'type':'Service', 'name': service.name, 'obj': s, 'overlord': overlord]
			}
		}
		
		return orphans
	}
	
    def serverProp(params) {
        def s = Humidor.instance.sigar
        def dateFormat  = DateFormat.dateTimeInstance
        def cmdLine     = s.getProcArgs('$$')
        def procEnv     = s.getProcEnv('$$')
        def databaseVersion = runQueryAsText('version')
        def dbVersionIndex = databaseVersion.lastIndexOf(':')
        def dbVersion = databaseVersion.substring(dbVersionIndex+1)

        def locals = [
            numCpu:           Runtime.runtime.availableProcessors(),
            fqdn:             s.getFQDN(),
            dbVersion:        dbVersion,
            numPlatforms:     resourceHelper.find(count:'platforms'),
            numCpus:          resourceHelper.find(count:'cpus'),
            numAgents:        agentHelper.find(count:'agents'),
            numActiveAgents:  agentHelper.find(count:'activeAgents'),
            numServers:       resourceHelper.find(count:'servers'),
            numServices:      resourceHelper.find(count:'services'),
            numApplications:  resourceHelper.find(count:'applications'),
            numRoles:         resourceHelper.find(count:'roles'),
            numUsers:         resourceHelper.find(count:'users'),
            numAlertDefs:     resourceHelper.find(count:'alertDefs'),
            numResources:     resourceHelper.find(count:'resources'),
            numResourceTypes: resourceHelper.find(count:'resourceTypes'),
            numGroups:        resourceHelper.find(count:'groups'),
            numEscalations:   resourceHelper.find(count:'escalations'),
            numActiveEscalations:  resourceHelper.find(count:'activeEscalations'),
            metricsPerMinute: metricsPerMinute,
            hqVersion:        Bootstrap.getBean(ProductBoss.class).version,
            jvmProps:         System.properties,
            schemaVersion:    Bootstrap.getBean(ServerConfigManager.class).config.getProperty('CAM_SCHEMA_VERSION'),
            cmdLine:          cmdLine,
            procEnv:          procEnv,
        ] + getSystemStats([:])

        render(locals: locals)
    }
    
    private withConnection(Closure c) {
        def ctx  = new InitialContext()
        def conn
        
        try {
            conn = Bootstrap.getBean("DBUtil").connection;
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
          orphanedGroupMapRows: [
             name: localeBundle['queryOrphanedGroupMapRows'],
             query: "SELECT COUNT(*) FROM EAM_RES_GRP_RES_MAP WHERE RESOURCE_ID IN " +
                   "(SELECT ID FROM EAM_RESOURCE WHERE " +
                   "RESOURCE_TYPE_ID = 301 AND INSTANCE_ID NOT IN " +
                   "(SELECT ID FROM EAM_PLATFORM)) OR RESOURCE_ID IN " +
                   "(SELECT ID FROM EAM_RESOURCE WHERE " +
                   "RESOURCE_TYPE_ID = 303 AND INSTANCE_ID NOT IN " +
                   "(SELECT ID FROM EAM_SERVER)) OR RESOURCE_ID IN " +
                   "(SELECT ID FROM EAM_RESOURCE WHERE " +
                   "RESOURCE_TYPE_ID = 305 AND INSTANCE_ID NOT IN " +
                   "(SELECT ID FROM EAM_SERVICE))"],
          orphanedResources: [
             name: localeBundle['queryOrphanedResources'],
             query: "SELECT COUNT(*) FROM EAM_RESOURCE WHERE RESOURCE_TYPE_ID IN " +
                   "(301, 303, 305) AND NOT EXISTS (SELECT RESOURCE_GROUP_ID " +
                   "FROM EAM_RES_GRP_RES_MAP WHERE RESOURCE_ID = ID)"],
          orphanedAlertDefs: [
             name: localeBundle['queryOrphanedAlertDefs'],
             query: "SELECT COUNT(*) FROM EAM_ALERT_DEFINITION " +
                   "WHERE RESOURCE_ID IS NULL AND PARENT_ID IS NOT NULL " +
                   "AND NOT PARENT_ID=0"],
          orphanedAuditRows: [
             name: localeBundle['queryOrphanedAuditRows'],
             query: "SELECT COUNT(*) FROM EAM_AUDIT WHERE NOT EXISTS " +
                   "(SELECT RESOURCE_GROUP_ID FROM EAM_RES_GRP_RES_MAP " +
                   "WHERE EAM_AUDIT.RESOURCE_ID = EAM_RES_GRP_RES_MAP.RESOURCE_ID)"],
          orphanedEscalationState: [
             name: localeBundle['queryOrphanedEscalationState'],
             query: "SELECT COUNT(*) FROM EAM_ESCALATION_STATE WHERE " +
                    "(ALERT_TYPE = 559038737 AND NOT EXISTS " +
                    "(SELECT 1 FROM EAM_ALERT WHERE ALERT_ID = EAM_ALERT.ID)) OR " +
                    "(ALERT_TYPE = 195934910 AND NOT EXISTS " +
                    "(SELECT 1 FROM EAM_GALERT_LOGS WHERE ALERT_ID = EAM_GALERT_LOGS.ID))"],
          orphanedServers: [
              name: localeBundle['queryOrphanedServers'],
              query: "SELECT COUNT(*) FROM EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 303 " +
                     "AND INSTANCE_ID NOT IN (SELECT ID FROM EAM_SERVER)"],
          orphanedServices: [
              name: localeBundle['queryOrphanedServices'],
              query: "SELECT COUNT(*) FROM EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 305 " +
                     "AND INSTANCE_ID NOT IN (SELECT ID FROM EAM_SERVICE)"],
          orphanedPlatforms: [
              name: localeBundle['queryOrphanedPlatforms'],
              query: "SELECT COUNT(*) FROM EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 301 " +
                     "AND INSTANCE_ID NOT IN (SELECT ID FROM EAM_PLATFORM)"],
          orphanedResourceGroups: [
              name: localeBundle['queryOrphanedResourceGroups'],
              query: "SELECT COUNT(*) FROM EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 3 " +
                     "AND INSTANCE_ID NOT IN (SELECT ID FROM EAM_RESOURCE_GROUP)"],
          resourceAlertsActiveButDisabled: [ 
             name: localeBundle['queryResourceAlertDefsActiveButDisabled'], 
             query: {conn -> "select id, name, description, resource_id from EAM_ALERT_DEFINITION where "+
                    "(parent_id is null or parent_id > 0) and active="+
                    DBUtil.getBooleanValue(true, conn)+" and enabled="+
                    DBUtil.getBooleanValue(false, conn)+" and deleted="+
                    DBUtil.getBooleanValue(false, conn)}],
          version: [ 
              name: localeBundle['queryVersion'],     
              query:  {conn -> getDatabaseVersionQuery(conn)}],
          dbCharacterSet: [
              name: localeBundle['queryDatabaseCharacterSet'],
              query: {conn -> getDatabaseCharacterSet(conn)}],
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
    
    private getDatabaseActions() {
        def queries = [ 
          aiqPurge: [ 
             name: localeBundle['actionPurgeAI'], 
             query: [ "DELETE FROM EAM_AIQ_IP",
                      "DELETE FROM EAM_AIQ_SERVICE",
                      "DELETE FROM EAM_AIQ_SERVER",
                      "DELETE FROM EAM_AIQ_PLATFORM",
                    ]
          ],
          escStatePurge: [ 
             name: localeBundle['actionPurgeEscState'], 
             query: [ "DELETE FROM EAM_ESCALATION_STATE WHERE " +
                      "(ALERT_TYPE = 559038737 AND NOT EXISTS " +
                      "(SELECT 1 FROM EAM_ALERT WHERE ALERT_ID = EAM_ALERT.ID)) OR " +
                      "(ALERT_TYPE = 195934910 AND NOT EXISTS " +
                      "(SELECT 1 FROM EAM_GALERT_LOGS WHERE ALERT_ID = EAM_GALERT_LOGS.ID))",
                    ]
          ],
        ]
    }

    private getDatabaseVersionQuery(conn) {
        if (DBUtil.isOracle(conn)) {
            return "SELECT * FROM V\$VERSION"
        } else {
            return "SELECT VERSION()"
        }
    }
    
    private getDatabaseCharacterSet(conn) {
        if (DBUtil.isOracle(conn)){
            return "SELECT * FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET'"
        } else if (DBUtil.isMySQL(conn)){
            return "SHOW VARIABLES LIKE 'C%_DATABASE'"
        } else if (DBUtil.isPostgreSQL(conn)){
            return "SHOW SERVER_ENCODING" 
        } 
        
    }
    
    private h(str) {
        str.toString().toHtml()
    }
    
    private getJVMSupportsTraces() {
        def ver = System.getProperty('java.version')[0..2]
        ver != '1.3' && ver != '1.4' 
    }
    
    def runQueryAsText(query) {
        runQuery([query: [query]], false)
    }    
    
    def runQuery(params) {
        runQuery(params, true)
    }
    
	def runAllOrphanQueries(){
		def allResults = [:]
		getDatabaseQueries().each{ queryEntry ->
			if (queryEntry.key.startsWith("orphaned")) {
				allResults.put(queryEntry.key,runQueryAsText(queryEntry.key))
			}
		}
		return allResults
	}
	
    def runQuery(params, returnHtml) {
        def id    = params.getOne('query')
        def query
        
        if (databaseQueries[id].query in Closure) {
            query = withConnection() { conn -> 
                databaseQueries[id].query(conn)       
            }
        } else {
            query = databaseQueries[id].query        
        }    
        
        def name  = databaseQueries[id].name
        def start = now()

        log.debug("Running query [${query}]")
        def res = withConnection() { conn ->
            def sql    = new Sql(conn)
            def output = new StringBuffer()
            def txtOutput = new StringBuffer()
            def rowIdx = 0
            def md

            sql.eachRow(query) { rs ->
                if (rowIdx++ == 0) {
                    output << "<table cellspadding=3 cellspacing=0 border=0 width=98%><thead><tr>"
                    txtOutput << "${name}:\n"
                    md = rs.getMetaData()
                    for (i in 1..md.columnCount) {
                        output <<  "<td>${h md.getColumnLabel(i)}</td>"
                        txtOutput << "${h md.getColumnLabel(i)}: "
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
                            txtOutput << "${trimmedCol}\n"
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
                if (returnHtml) {
                    return output
                }
                else {
                    return txtOutput
                }
            }
        }
        
        def queryData = "${name} executed in ${now() - start} ms<br/><br/>"
        if (returnHtml) {
            return [ queryData: queryData + res ]
        }
        else {
            return res
        }
    }

    def executeQuery(params) {
        executeQuery(params, true)
    }

    def executeQuery(params, returnHtml) {
        def id    = params.getOne('query')
        def queries
        
        if (databaseActions[id].query in Closure) {
            queries = withConnection() { conn -> 
                databaseActions[id].query(conn)       
            }
        } else {
            queries = databaseActions[id].query        
        }    
        
        def name  = databaseActions[id].name
        def start = now()

        log.debug("Running queries [${queries}]")
        def res = withConnection() { conn ->
            def sql    = new Sql(conn)
            def output = new StringBuffer()
            def txtOutput = new StringBuffer()
            def md
            output << "<ol>\n"
            queries.each {
                output << "\t<li>${it}: "
                output << sql.executeUpdate(it)
                output << " ${localeBundle['rows']}</li>\n"
            }
            output << "</ol>"
        }

        def queryData = "${name} executed in ${now() - start} ms<br/><br/>"
        if (returnHtml) {
            return [ queryData: queryData + res ]
        }
        else {
            return res
        }
    }
	
	def getMaintenanceOps() {
		def ops = [
			findOrphanedNodes: [
			   name: localeBundle['maintenceOpFindOrphanNodes'],
			   op: { params ->
				   def output = "<table><tr><th>ID</th><th>Type</th><th>Name</th></tr>"
				   for (node in _findOrphanedData(params)) {
					   output += "<tr><td>${node.id}</td><td>${node.type}</td><td>${node.name}</td></tr>"
				   }
				   output += "</table>"
				   return output
			   }
			],
			cleanupOrphanedNodes: [
				name: localeBundle['maintenanceOpCleanupOrphanNodes'],
				op: { params ->
				   def output = "<table><tr><th>ID</th><th>Type</th><th>Name</th></tr>"
				   for (node in _findOrphanedData(params)) {
					   output += "<tr><td>${node.id}</td><td>${node.type}</td><td>${node.name}</td></tr>"
					   node.obj.remove(node.overlord)
				   }
				   output += "</table>"
				   return output
				}
			]
		  ]
  
	}
	
	def executeMaintenanceOp(params) {
        def id    = params.getOne('op')
		def name = maintenanceOps[id].name
		def start = now()
		def res = maintenanceOps[id].op(params)
		def maintenanceOpData = "${name} executed in ${now() - start} ms<br/><br/>"
		return [maintenanceOpData: maintenanceOpData + res]
	}
}
