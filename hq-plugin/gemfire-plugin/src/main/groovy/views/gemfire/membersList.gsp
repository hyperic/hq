<ul>
  <li>Servers: ${s}</li>
  <li>Gateways: ${g}</li>
  <li>Clients: ${a}</li>
</ul>
<table class="tablesorter">
  <thead>
    <tr>
      <th class="labelColumn">id</th>
      <th>name</th>
      <th>host</th>
      <th>port</th>
      <th>heap</th>
      <th>cpu</th>
      <th>uptime</th>
      <th>clients</th>
      <th>type</th>
    </tr>
  </thead>
  <tbody>
<%for (member in members.values()){%>
    <tr>
      <td>${member.get("_id")}</td>
      <td>${member.get("name")}</td>
      <td>${member.get("host")}</td>
      <td>${member.get("port")}</td>
      <td>${member.get("heap")}%</td>
      <td>${member.get("cpu")}%</td>
      <td>${member.get("uptime")}</td>
      <td>${member.get("clients").size()}</td>
      <td>${member.get("type")}</td>
    </tr>
  <% } %>
  </tbody>
</table>
