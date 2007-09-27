<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
  <meta refresh http-equiv="refresh" content="5"/>
  
  <div id=dojoDebugOutput></div>
  <script type="text/javascript">
	var djConfig = {
		isDebug: true,
		debugAtAllCosts: true,		
		debugContainerId : "dojoDebugOutput",
	};
  </script>
  <script type="text/javascript" src="/dojo/dojo.js"></script>
  <script type="text/javascript">
    dojo.require("dojo.widget.FilteringTable");
	dojo.hostenv.writeIncludes();
  </script>
  <link rel=stylesheet href="/dojo/fancy.css" type="text/css"/>
</head>

<body>
  <h1>Crack for $group.name (in $fetchTime ms)</h1>
  <table>
  <% for (c in cpuData) { %>
    <tr>
      <td><b>$c.name</b></td>
      <td>$c.value</td>
    </tr>
  <% } %>
  </table>

  <table dojoType="filteringTable" id="dataTable"
  	     multiple="false" alternateRows="true" maxSortable="1"
		 cellpadding="0" cellspacing="0" border="0">
    <thead>
    <tr>
      <th field="Platform" dataType="html"   >Platform</th>
      <th field="PID"      dataType="html"   >PID</th>
      <th field="Command"  dataType="String" >Command</th>
      <th field="CPUPerc"  dataType="String" >%CPU</th>
      <th field="Time"     dataType="String" >Time</th>
      <th field="Shared"   dataType="String" >Shared</th>
      <th field="Resident" dataType="String" >Resident</th>
      <th field="Virtual"  dataType="String" >Virtual</th>
      <th field="State"    dataType="String" >State</th>
    </tr>
    </thead>

    <tbody>
    <% topTable.eachWithIndex { row, i -> %>
    <tr value="$i">
      <td><%= link_to row.platform.name, [resource:row.platform] %></td> 
      <td><%= link_to row.data.pid, [action:'showProcess'],
                      [id:row.platform.id, pid:row.data.pid] %></td>    
      <td>$row.niceName</td>    
      <td>$row.data.formattedCpuPerc</td>    
      <td>$row.data.formattedCpuTotal</td>    
      <td>$row.data.formattedShare</td>    
      <td>$row.data.formattedResident</td>    
      <td>$row.data.formattedSize</td>
      <td>
          <%= avail_icon row.stateIcon %>
      </td>
    </tr>
    <% } %>
    </tbody>
  </table>
</body>
</html>