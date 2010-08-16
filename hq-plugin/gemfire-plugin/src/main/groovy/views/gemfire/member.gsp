<h2>Deatils of ${member.get("name")} with ID: ${member.get("_id")}</h2>
<table class="tablesorter">
  <thead>
    <tr>
      <th>heap</th>
      <th>cpu</th>
      <th>uptime</th>
      <th>clients</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>${member.get("heap")}%</td>
      <td>${member.get("cpu")}%</td>
      <td>${member.get("uptime")}</td>
      <td>${member.get("clients").size()}</td>
    </tr>
  </tbody>
</table>
<%if(member.get("gatewayhub.gateways")!=null){%>
<h3>gateways:</h3>
<dl>
  <%for(gateway in member.get("gatewayhub.gateways")){%>
  <dt>
    ${gateway.get("gemfire.member.gateway.id.string")} Queue Size=${gateway.get("gemfire.member.gateway.queuesize.int")}
  </dt>
  <dd>
    <table class="tablesorter">
      <thead>
        <tr>
          <th>Name</th>
          <th>Host</th>
          <th>Port</th>
        </tr>
      </thead>
      <tbody>
    <%for(endpoint in gateway.get("gemfire.member.gateway.endpoints.collection")){%>
        <tr>
          <td>${endpoint.get("gemfire.member.gateway.endpoint.id.string")}</td>
          <td>${endpoint.get("gemfire.member.gateway.endpoint.host.string")}</td>
          <td>${endpoint.get("gemfire.member.gateway.endpoint.port.int")}</td>
        </tr>
      <% } %>
      </tbody>
    </table>
  </dd>
    <% } %>
</dl>
  <% } %>
<h3>Regions:</h3>
<table class="tablesorter">
  <thead>
    <tr>
      <th>Name</th>
      <th>Path</th>
      <th>scope</th>
      <th>Data Policy</th>
      <th>Insert Policy</th>
      <th>Entry counts</th>
      <th>Disk Attrs</th>
    </tr>
  </thead>
  <tbody>
<%for (region in member.get("regions").values()){ %>
    <tr>
      <td>${region.get("gemfire.region.name.string")}</td>
      <td>${region.get("gemfire.region.path.string")}</td>
      <td>${region.get("gemfire.region.scope.string")}</td>
      <td>${region.get("gemfire.region.datapolicy.string")}</td>
      <td>${region.get("gemfire.region.interestpolicy.string")}</td>
      <td>${region.get("gemfire.region.entrycount.int")}</td>
      <td>${region.get("gemfire.region.diskattrs.string")}</td>
    </tr>
  <% } %>
  </tbody>
</table>
<h3>Clients:</h3>
<table class="tablesorter">
  <thead>
    <tr>
      <th>id</th>
      <th>name</th>
      <th>host</th>
      <th>port</th>
      <th>queuesize</th>
      <th>gets</th>
      <th>puts</th>
      <th>cachemisses</th>
      <th>cpu</th>
      <th>threads</th>
    </tr>
  </thead>
  <tbody>
<%for (client in clients){ def member=members.get(client.get("gemfire.client.id.string")) %>
    <tr>
      <td>${client.get("gemfire.client.id.string")}</td>
      <td>${member.get("name")}</td>
      <td>${member.get("host")}</td>
      <td>${member.get("port")}</td>
      <td>${client.get("gemfire.client.queuesize.int")}</td>
      <td>${client.get("gemfire.client.stats.gets.int")}</td>
      <td>${client.get("gemfire.client.stats.puts.int")}</td>
      <td>${client.get("gemfire.client.stats.cachemisses.int")}</td>
      <td>${client.get("gemfire.client.stats.cpus.int")}%</td>
      <td>${client.get("gemfire.client.stats.threads.int")}</td>
    </tr>
  <% } %>
  </tbody>
</table>
