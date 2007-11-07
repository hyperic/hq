<html>
<head>
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
  </script>

  <link rel=stylesheet href="/dojo/fancy.css" type="text/css"/>
</head>

<body>
  <h1>Process Information</h1>
  <div>
    <%= button(text:"Destroy", to:[action:'killProcess'], 
               htmlOpts:[id:platform.id, pid:pid]) %>
  </div>
  
  <br/>
  
  <% if (procData.procExe != null) { %>
    <b>Process</b>: <%= h procData.procExe.name %><br/>
    <b>Working Dir</b>: <%= h procData.procExe.cwd %><br/>
  <% } %>

  <% if (procData.procFd != null) { %>
    <b>Open File Descriptors</b>: <%= procData.procFd.total %><br/>  
  <% } %>
  
  <% if (procData.procArgs != null) { %>
    <h3>Arguments</h3>
    <ul>
    <% for (arg in procData.procArgs) { %>
      <%= h arg %>
    <% } %>
    </ul>
  <% } %>

  <% if (procData.procEnv != null) { %>  
    <h3>Environment</h3>
    <table dojoType="filteringTable" id="procTable"
           multiple="false" alternateRows="true" maxSortable="1"
		   cellpadding="0" cellspacing="0" border="0">
    <thead>
      <tr>
        <th field="Foo" dataType="String">Property</th>
        <th field="Bar" dataType="String">Value</th>
      </tr>
    </thead>
    <tbody>
    <% for (env in procData.procEnv) { %>
      <tr>
        <td><%= h env.key %></td>
        <td><%= h env.value %></td
      </tr>
    <% } %>
    </tbody>
    </table>
  <% } %>
</body>
</html>
