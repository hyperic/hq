<div id="reportHeader">
  <pre>
-- HQ Health Report --
${reportTime}
${userName} @ ${fqdn}
Version: ${hqVersion} # ${buildNumber}
Schema: ${schemaVersion}
System ID: ${guid}

-- HQ Load --
# agents:    ${numAgents}
Metric load: ${metricsPerMinute}


-- System Vitals --
System Load Average:
- ${l.oneMin}: ${loadAvg1}
- ${l.fiveMin}: ${loadAvg5}
- ${l.fifteenMin}: ${loadAvg15}
    
System Memory:
- ${l.total}: ${totalMem}
- ${l.used}: ${usedMem}
- ${l.free}: ${freeMem}
    
System Swap:
- ${l.total}: ${totalSwap}
- ${l.used}: ${usedSwap}
- ${l.free}: ${freeSwap}
    

-- HQ Process --
- ${l.pid}: ${pid}
- ${l.procOpenFds}: ${procOpenFds}
- ${l.procStartTime}: ${procStartTime}
- ${l.procMemSize}: ${procMemSize}
- ${l.procMemRes}: ${procMemRes}
- ${l.procMemShare}: ${procMemShare}
- ${l.procCpu}: ${procCpu}
- ${l.numCpu}: ${numCpu}


-- ${l.jvm} --

- ${l.jvmPercMem}: ${jvmPercMem}
- ${l.jvmFreeMem}: ${jvmFreeMem}
- ${l.jvmTotalMem}: ${jvmTotalMem}
- ${l.jvmMaxMem}: ${jvmMaxMem}
    

<% for (d in diagnostics) { %>
-- ${l.diagnostics}:  ${d.name} --

${d.status}


<% } %>
    
  </pre>
</div>
