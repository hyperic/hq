<div id="reportHeader">
  <pre>
-- HQ Health Report --
${reportTime}
${userName} @ ${fqdn}

${dbVersion}
${dbCharacterSet}

-- HQ Load --
${l.metricsPerMinute}: ${metricsPerMinute}
${l.numPlatforms}: ${numPlatforms}
${l.numCpus}: ${numCpus}
${l.numAgents}: ${numAgents}
${l.numActiveAgents}: ${numActiveAgents}
${l.numServers}: ${numServers}
${l.numServices}: ${numServices}
${l.numApplications}: ${numApplications}
${l.numRoles}: ${numRoles}
${l.numUsers}: ${numUsers}
${l.numAlertDefs}: ${numAlertDefs}
${l.numResources}: ${numResources}
${l.numResourceTypes}: ${numResourceTypes}
${l.numGroups}: ${numGroups}
${l.numEscalations}: ${numEscalations}
${l.numActiveEscalations}: ${numActiveEscalations}

-- System Vitals --
System Load Average:
- ${l.oneMin}: ${loadAvg1}
- ${l.fiveMin}: ${loadAvg5}
- ${l.fifteenMin}: ${loadAvg15}
    
System Processors: <% for (c in cpuInfos) { %>
- ${c.vendor} ${c.model} ${c.mhz} <% } %>

System Memory:
- ${l.total}: ${totalMem}
- ${l.used}: ${usedMem}
- ${l.free}: ${freeMem}
    
System Swap:
- ${l.total}: ${totalSwap}
- ${l.used}: ${usedSwap}
- ${l.free}: ${freeSwap}
    

-- HQ Process --
- Version: ${hqVersion}
- Schema: ${schemaVersion}
- ID: ${guid}
- Command Line: ${cmdLine}
- ${l.pid}: ${pid}
- ${l.procOpenFds}: ${procOpenFds}
- ${l.procStartTime}: ${procStartTime}
- ${l.procMemSize}: ${procMemSize}
- ${l.procMemRes}: ${procMemRes}
- ${l.procMemShare}: ${procMemShare}
- ${l.procCpu}: ${procCpu}
- ${l.numCpu}: ${numCpu}

Environment:
<% for (p in procEnv.entrySet().sort {a,b->a.key <=> b.key}) { %>
- ${p.key} = ${p.value} <% } %>

-- Orphaned Nodes -- <% for (n in orphanedNodes) { %>
- ID: ${n.id} - Type: ${n.type} - ${n.name} <% } %>

-- ${l.jvm} --
- ${l.jvmPercMem}: ${jvmPercMem}
- ${l.jvmFreeMem}: ${jvmFreeMem}
- ${l.jvmTotalMem}: ${jvmTotalMem}
- ${l.jvmMaxMem}: ${jvmMaxMem}

    
-- JVM Properties --
<% for (p in jvmProps.entrySet().sort {a,b->a.key <=> b.key}) { %>
- ${p.key} = ${p.value} <% } %>

<% for (d in diagnostics) { %>
<!-- filter out the diagnostic ehcache in order to give a more detailed on-demand output -->
<% if (!"EhCache Diagnostics".equals(d.name)){ %>
--- ${l.diagnostics}:  ${d.name} --

${d.status}

<% } %>
<% } %>

-- EhCache Diagnostics --

<% print "Cache Region".padRight(70) + "Size".padRight(15) + "Hits".padRight(20) + "Misses".padRight(20) + "Limit".padRight(15) + "Memory Usage".padRight(10) %>
<% print "------------".padRight(70) + "----".padRight(15) + "----".padRight(20) + "------".padRight(20) + "-----".padRight(15) + "------------".padRight(10) %>
<% for (data in cacheHealths.sort {a,b->a.region.toLowerCase() <=> b.region.toLowerCase()}){ %>
<% print data.region.padRight(70) + (data.size+"").padRight(15) +  (data.hits+"").padRight(20) + (data.misses+"").padRight(20) + (data.limit+"").padRight(15) +  (data.memoryUsage+"").padRight(10) %><% } %>


-- Database Queries --
<% for (q in dbQueries.entrySet().sort {a,b->a.key <=> b.key}){ %>
${q.value}
<% } %>

<% if (licenseInfo) { %>
-- License --
Licensee:  ${licenseInfo.licensee}
Expire:    ${licenseInfo.licenseExpire}
Platforms: ${licenseInfo.licensePlatforms}
Count:     ${licenseInfo.platformCount}
<% } %>

-- Inventory Summary --

<% print "Resource Type".padRight(100) + "Total".padRight(15) %>
<% print "------------".padRight(100) + "----".padRight(15) %>
<% for (data in inventoryData.sort {a,b->a.name.toLowerCase() <=> b.name.toLowerCase()}){ %>
<% print data.name.padRight(100) + (data.total+"").padRight(15)  %><% } %>


-- Agents --
<%= agentFmt.sprintf([l.fqdn, AgentSortField.ADDR.value, 
                      AgentSortField.PORT.value, AgentSortField.VERSION.value,
                      AgentSortField.CTIME.value, l.numPlatforms, 
                      l.timeOffset, l.numMetrics, l.licenseCount] as Object[]) %>
-----------------------------------------------------------------------                      
<% for (a in agentData) { %>
<%= agentFmt.sprintf([a.platform, a.server, "${a.agent.port}", a.agent.version, 
                      a.creationTime, "${a.agent.platforms.size()}", 
                      "${a.offset}", "${a.numMetrics}", a.licenseCount] as Object[]) %> <% } %>
                      



<% if (jvmSupportsTraces) { %>
-- Thread Dump --
  <% for (ent in Thread.allStackTraces) { %>
    <%= h(ent.key) %>
    <% for (elem in ent.value) { %> <%= h(elem) %> 
    <% } %>
  <% } %>
<% } %>
    
  </pre>
</div>
