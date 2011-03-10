<%if(member!=null) { %>
<h2>Details of ${member.get("name")} with ID: ${member.get("id")}<span class="loading"></span></h2>
<div class="frame">
  <table>
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
        <td class="number">${member.get("heap")}</td>
        <td class="number">${member.get("cpu")}</td>
        <td>${member.get("uptime")}</td>
        <td class="number">${member.get("clients").size()}</td>
      </tr>
    </tbody>
  </table>
  <%if(member.get("gatewayhub.gateways")!=null){%>
  <h3>gateways:</h3>
  <dl>
    <%for(gateway in member.get("gatewayhub.gateways")){%>
    <dt>
      <b>${gateway.get("gemfire.member.gateway.id.string")}</b><br/>
      Queue Size=${gateway.get("gemfire.member.gateway.queuesize.int")}<br/>
      Connected=${gateway.get("gemfire.member.gateway.isconnected.boolean")}
    </dt>
    <dd>
      <h4>End Points:</h4>
      <table>
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
  <table>
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
        <td class="number">${region.get("gemfire.region.entrycount.int")}</td>
        <td>${region.get("gemfire.region.diskattrs.string")}</td>
      </tr>
    <% } %>
    </tbody>
  </table>
  <%if(member.get("clients").size()>0) { %>
  <h3>Clients:</h3>
  <table>
    <thead>
      <tr>
        <th>id</th>
        <th>queuesize</th>
        <th>gets</th>
        <th>puts</th>
        <th>cachemisses</th>
        <th>threads</th>
      </tr>
    </thead>
    <tbody>
    <%for (client in member.get("clients").values()){%>
      <tr>
        <td><a id="${client.get("gemfire.client.id.string")}" class="member" href="#${client.get("gemfire.client.id.string")}">${client.get("gemfire.client.id.string")}</a></td>
        <td class="number">${client.get("gemfire.client.queuesize.int")}</td>
        <td class="number">${client.get("gemfire.client.stats.gets.int")}</td>
        <td class="number">${client.get("gemfire.client.stats.puts.int")}</td>
        <td class="number">${client.get("gemfire.client.stats.cachemisses.int")}</td>
        <td class="number">${client.get("gemfire.client.stats.threads.int")}</td>
      </tr>
      <% } %>
    </tbody>
  </table>
    <% } %>
</div>
  <% } else { %>
 <h2>Member with ID: ${mid} Not FOUND!!! <span class="loading"></span></h2>
 <% } %>
