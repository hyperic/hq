<%= hquStylesheets() %>
<script type="text/javascript">
	document.navTabCat = "Admin";

	function getSystemStats() {
  		hqDojo.xhrGet({
    		url: '<%= urlFor(action:"getSystemStats", encodeUrl:true) %>',
    		handleAs: "json-comment-filtered",
    		preventCache: true,
    		load: function(response, args) {
      			hqDojo.byId('userCPU').innerHTML       = response.sysUserCpu;
			    hqDojo.style('userCPUBar', 'width', response.sysUserCpu) ;
			    hqDojo.byId('sysCPU').innerHTML        = response.sysSysCpu;
			    hqDojo.style('sysCPUBar', 'width', response.sysSysCpu);
			    hqDojo.byId('niceCPU').innerHTML       = response.sysNiceCpu;
			    hqDojo.byId('idleCPU').innerHTML       = response.sysIdleCpu;
			    hqDojo.byId('waitCPU').innerHTML       = response.sysWaitCpu;
			    hqDojo.byId('loadAvg1').innerHTML      = response.loadAvg1;
			    hqDojo.byId('loadAvg5').innerHTML      = response.loadAvg5;
			    hqDojo.byId('loadAvg15').innerHTML     = response.loadAvg15;
			    hqDojo.byId('totalMem').innerHTML      = response.totalMem;
			    hqDojo.byId('usedMem').innerHTML       = response.usedMem;
			    hqDojo.style('usedMemBar', 'width',response.percMem);
			    hqDojo.byId('freeMem').innerHTML       = response.freeMem;
			    hqDojo.byId('totalSwap').innerHTML     = response.totalSwap;
			    hqDojo.byId('usedSwap').innerHTML      = response.usedSwap;
			    hqDojo.style('usedSwapBar', 'width', response.percSwap);
			    hqDojo.byId('freeSwap').innerHTML      = response.freeSwap;
			    hqDojo.byId('pid').innerHTML           = response.pid;
			    hqDojo.byId('procStartTime').innerHTML = response.procStartTime;
			    hqDojo.byId('procOpenFds').innerHTML   = response.procOpenFds;
			    hqDojo.byId('procMemSize').innerHTML   = response.procMemSize;
			    hqDojo.byId('procMemRes').innerHTML    = response.procMemRes;
			    hqDojo.byId('procMemShare').innerHTML  = response.procMemShare;
			    hqDojo.byId('procCpu').innerHTML       = response.procCpu;
			    hqDojo.style('procCpuBar', 'width', response.procCpu);
			    hqDojo.byId('sysPercCpu').innerHTML    = response.sysPercCpu;
			    hqDojo.style('sysPercCpuBar', 'width', response.sysPercCpu);
			    hqDojo.byId('percMem').innerHTML       = response.percMem;
			    hqDojo.style('percMemBar', 'width', response.percMem);
			    hqDojo.byId('percSwap').innerHTML      = response.percSwap;
			    hqDojo.style('percSwapBar', 'width', response.percSwap);
			    hqDojo.byId('jvmTotalMem').innerHTML   = response.jvmTotalMem
			    hqDojo.byId('jvmFreeMem').innerHTML    = response.jvmFreeMem
			    hqDojo.byId('jvmMaxMem').innerHTML     = response.jvmMaxMem
			    hqDojo.byId('jvmPercMem').innerHTML    = response.jvmPercMem
    		}
  		});
  		
  		setTimeout("getSystemStats()", 3000);
	}

	getSystemStats();
</script>

<div id="metrics">
	<div class="metricGroupBlock">
  		<div id="systemLoad" class="metricGroup">
    		<div class="metricCatLabel">${l.sysLoad}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.oneMin}:</td>
      				<td><span class="metricValue" id="loadAvg1"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.fiveMin}:</td>
      				<td><span class="metricValue" id="loadAvg5"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.fifteenMin}:</td>
      				<td><span class="metricValue" id="loadAvg15"></span></td>
    			</tr>
    		</table>
  		</div>
    	<div id="processorStats" class="metricGroup">
    		<div class="metricCatLabel">${l.sysProcStats}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.user}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="userCPUBar" class="bar">
      							<span class="metricValue" id="userCPU"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.system}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="sysCPUBar" class="bar">
      							<span class="metricValue" id="sysCPU"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.nice}:</td>
      				<td><span class="metricValue" id="niceCPU"></span>%</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.idle}:</td>
      				<td><span class="metricValue" id="idleCPU"></span>%</td>
    			</tr>
    			<tr class="metricRow">  
      				<td>${l.wait}:</td>
      				<td><span class="metricValue" id="waitCPU"></span>%</td>
    			</tr>
    		</table>
  		</div>
	</div>
	<div class="metricGroupBlock"> 
  		<div id="memStats" class="metricGroup">
    		<div class="metricCatLabel">${l.sysMemStats}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.total}:</td>
      				<td><span class="metricValue" id="totalMem"></span></td>
    			</tr>
   				<tr class="metricRow">
      				<td>${l.used}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="usedMemBar" class="bar">
      							<span class="metricValue" id="usedMem"></span>
      						</div>
      					</div>
      				</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.free}:</td>
      				<td><span class="metricValue" id="freeMem"></span></td>
    			</tr>
   			</table>
  		</div>
  		<div id="swapStats" class="metricGroup">
    		<div class="metricCatLabel">${l.sysSwapStats}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.total}:</td>
      				<td><span class="metricValue" id="totalSwap"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.used}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="usedSwapBar" class="bar">
      							<span class="metricValue" id="usedSwap"></span>
      						</div>
      					</div>
      				</td>
     			</tr>
    			<tr class="metricRow">
      				<td>${l.free}:</td>
      				<td><span class="metricValue" id="freeSwap"></span></td>
    			</tr>
    		</table>
  		</div>
	</div>
	<div class="metricGroupBlock">
  		<div id="processInfo" class="metricGroup">
    		<div class="metricCatLabel">${l.procInfo}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.pid}:</td>
      				<td><span id="pid"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procOpenFds}:</td>
      				<td><span id="procOpenFds"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procStartTime}:</td>
      				<td><span id="procStartTime"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procMemSize}:</td>
      				<td><span id="procMemSize"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procMemRes}:</td>
      				<td><span id="procMemRes"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procMemShare}:</td>
      				<td><span id="procMemShare"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.procCpu}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="procCpuBar" class="bar">
      							<span id="procCpu"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    		</table>
  		</div>
  		<div id="mailLinks" class="metricGroup">
    		<div class="metricCatLabel">Actions</div>
        	<div class="printButton">${linkTo('Print', [action:'printReport'])}</div>
  		</div>
	</div>
	<div class="metricGroupBlock">
  		<div id="jvmInfo" class="metricGroup">
    		<div class="metricCatLabel">${l.jvm}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.jvmPercMem}:</td>
      				<td><span id="jvmPercMem"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.jvmFreeMem}:</td>
      				<td><span id="jvmFreeMem"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.jvmTotalMem}:</td>
      				<td><span id="jvmTotalMem"></span></td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.jvmMaxMem}:</td>
      				<td><span id="jvmMaxMem"></span></td>
    			</tr>
    		</table>
  		</div>
  		<div id="percentages" class="metricGroup">
    		<div class="metricCatLabel">${l.perc}</div>
    		<table class="metricTable">
    			<tr class="metricRow">
      				<td>${l.cpuUsed}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="sysPercCpuBar" class="bar">
      							<span id="sysPercCpu"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.memUsed}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="percMemBar" class="bar">
      							<span id="percMem"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    			<tr class="metricRow">
      				<td>${l.swapUsed}:</td>
      				<td>
      					<div class="barContainer">
      						<div id="percSwapBar" class="bar">
      							<span id="percSwap"></span>%
      						</div>
      					</div>
      				</td>
    			</tr>
    		</table>
  		</div>
	</div>
</div>
<div id="fullBody" style="clear:both">
  	<% dojoTabContainer(id:'bodyTabContainer', style:'width: 100%; height:450px;') { %>
    	<% dojoTabPane(id:'diagTab', label:l.diagnostics) { %>
      		<div style="padding: 6px;">${l.diagWatchNotice}</div>
      		<div id="diagSelectControls">
        		<span>${l.selectDiagLabel}</span>
        		<select id="diagSelect" onchange='selectDiag(options[selectedIndex].value)'>
          			<option value='none'>-- ${l.selectDiag} --</option>
        			<% for (d in diags) { %>
          				<option value='${d.shortName}'>${h d.name}</option>
        			<% } %>
        		</select>
        		<img src="/images/arrow_refresh.png" onclick="loadDiag()"/>
     		</div>
      		<pre style="background-color: #FFF; border: 0px none;">
        		<div id="diagData"></div>
      		</pre>
    	<% } %>
    	<% dojoTabPane(id:'cacheTab', label:l.cache) { %>
      		<%= dojoTable(id:'cacheTable', title:l.cache,
                    refresh:60, url:urlFor(action:'cacheData'),
                    schema:cacheSchema, numRows:17, pageSize:500, pageControls:false) %>
    	<% } %>
    	<% dojoTabPane(id:'loadTab', label:l.load) { %>
      		<div style="padding: 6px;">
      			${l.metricsPerMinute}: ${metricsPerMinute}<br>
      			${l.numPlatforms}: ${numPlatforms}<br>
      			${l.numCpus}: ${numCpus}<br>
      			${l.numAgents}: ${numAgents}<br>
      			${l.numActiveAgents}: ${numActiveAgents}<br>
      			${l.numServers}: ${numServers}<br>
      			${l.numServices}: ${numServices}<br>
      			${l.numApplications}: ${numApplications}<br>
      			${l.numRoles}: ${numRoles}<br>
      			${l.numUsers}: ${numUsers}<br>
      			${l.numAlertDefs}: ${numAlertDefs}<br>
      			${l.numResources}: ${numResources}<br>
      			${l.numResourceTypes}: ${numResourceTypes}<br>
      			${l.numGroups}: ${numGroups}<br>
      			${l.numEscalations}: ${numEscalations}<br>
      			${l.numActiveEscalations}: ${numActiveEscalations}<br>
      		</div>
    	<% } %>
    	<% dojoTabPane(id:'databaseTab', label:l.database) { %>
      		<div id="querySelectControls">
        		<span>${l.selectActionLabel}</span>
        		<select id="queryExecute">
          			<option value='none'>-- ${l.selectAction} --</option>
        			<% for (q in databaseActions.entrySet().sort {a,b-> a.key <=> b.key}) { %>
          				<option value='${q.key}'>${h q.value.name}</option>
        			<% } %>
        		</select>
        		<img src="/images/tbb_go.gif" onclick="queryAction()"/>
        		<p></p>
        		<span>${l.selectQueryLabel}</span>
        		<select id="querySelect" onchange='selectQuery(options[selectedIndex].value)'>
          			<option value='none'>-- ${l.selectQuery} --</option>
        			<% for (q in databaseQueries.entrySet().sort {a,b-> a.key <=> b.key}) { %>
          				<option value='${q.key}'>${h q.value.name}</option>
        			<% } %>
        		</select>
        		<img src="/images/arrow_refresh.png" onclick="loadQuery()"/>
      		</div>
      		<div id="queryData"></div>
    	<% } %>
    	<% dojoTabPane(id:'agentTab', label:l.agents) { %>
      		<%= dojoTable(id:'agentTable', title:l.agents,
                   refresh:600, url:urlFor(action:'agentData'),
                   schema:agentSchema, numRows:17) %>
    	<% } %>
    	<% dojoTabPane(id:'maintenanceTab', label:l.maintenance) { %>
    		<div id="maintenanceOpControls">
    			<span>${l.selectActionLabel}</span>
    			<select id="maintenanceOpExecute">
    				<option value='none'>-- ${l.selectMaintenanceOp} --</option>
    				<% for (op in maintenanceOps.entrySet().sort {a,b -> a.key <=> b.key}) { %>
    					<option value='${op.key}'>${h op.value.name}</option>
    				<% } %>
    			</select>
    			<img src="/images/tbb_go.gif" onclick="maintenanceOpAction()"/>
    			<p></p>
    		</div>
    		<div id="maintenanceOpData"></div>
    	<% } %>
        <% dojoTabPane(id:'inventoryTab', label:l.inventory) { %>
            <%= dojoTable(id:'inventoryTable', title:l.inventory,
                   refresh:600, url:urlFor(action:'inventoryData'),
                   schema:inventorySchema, numRows:17, pageSize:500, pageControls:false) %>
        <% } %>
  	<% } %>
</div>
<script type="text/javascript">
	function selectDiag(d) {
  		if (d == 'none') {
    		hqDojo.byId('diagData').innerHTML = '';
	    	return;
  		}
    
	  	hqDojo.xhrGet({
    		url: '<%= urlFor(action:"getDiag", encodeUrl:true) %>',
    		handleAs: "json-comment-filtered",
    		preventCache: true,
	    	content: {
    			diag: d
        	},
	    	load: function(response, args) {
    	  		hqDojo.byId('diagData').innerHTML = response.diagData;
    		}
	  	});
	}

	function loadDiag() {
  		var selectDrop = document.getElementById('diagSelect');

  	  	selectDiag(selectDrop.options[selectDrop.selectedIndex].value);
	}

	function refreshDiag() {
  		loadDiag();
  		setTimeout("refreshDiag()", 1000 * 60);
	}

	refreshDiag();

	function selectQuery(q) {
  		if (q == 'none') {
    		hqDojo.byId('queryData').innerHTML = '';

	    	return;
  		}
    
  		hqDojo.xhrGet({
    		url: '<%= urlFor(action:"runQuery", encodeUrl:true) %>',
	    	handleAs: "json-comment-filtered",
	    	preventCache: true,
	    	content: {
	    		query: q
	        },
	    	load: function(response, args) {
	      		hqDojo.byId('queryData').innerHTML = response.queryData;
	    	}
	  	});
	}

	function queryAction() {
	  	var selectDrop = document.getElementById('queryExecute');

		executeQuery(selectDrop.options[selectDrop.selectedIndex].value);
	}
	
	function executeQuery(q) {
	  	if (q == 'none') {
	    	hqDojo.byId('queryData').innerHTML = '';
	    	return;
	  	}
	    
	  	hqDojo.xhrGet({
	    	url: '<%= urlFor(action:"executeQuery", encodeUrl:true) %>',
	    	handleAs: "json-comment-filtered",
	    	preventCache: true,
	    	content: {
	    		query: q
	        },
	    	load: function(response, args) {
	      		hqDojo.byId('queryData').innerHTML = response.queryData;
	    	}
	  	});
	}

	function loadQuery() {
  		var selectDrop = document.getElementById('querySelect');

  	  	selectQuery(selectDrop.options[selectDrop.selectedIndex].value);
	}

	function refreshQuery() {
  		loadQuery();
  		setTimeout("refreshQuery()", 1000 * 60);
	}

	refreshQuery();
	
	function maintenanceOpAction() {
	  	var selectDrop = hqDojo.byId('maintenanceOpExecute');

		executeMaintenanceOp(selectDrop.options[selectDrop.selectedIndex].value);
	}
	
	function executeMaintenanceOp(op) {
		if (op == 'none') {
	    	hqDojo.byId('maintenanceOpData').innerHTML = '';
	    	return;
	    }
	    
	  	hqDojo.xhrGet({
	    	url: '<%= urlFor(action:"executeMaintenanceOp", encodeUrl:true) %>',
	    	handleAs: "json-comment-filtered",
	    	preventCache: true,
	    	content: {
	    		op: op
	        },
	    	load: function(response, args) {
	      		hqDojo.byId('maintenanceOpData').innerHTML = response.maintenanceOpData;
	      		
				hqDojo.query("#maintenanceOpData tr:nth-child(even)").style("backgroundColor", "#FFFFCC");
	    	}
	  	});
	}
	
    
    hqDojo.subscribe("XHRComplete", function() {
    	setFoot();
    });
</script>