<h2>Distributed System: ${systemName}<span class="loading"></span></h2>
<ul>
  <li>Servers: ${s}</li>
  <li>Gateways: ${g}</li>
  <li>Clients: ${c}</li>
</ul>
<table cellspacing="1">
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
<% if (members!=null) { %>
  <%for (member in members.values()){%>
    <tr>
      <td>${member.get("id")}</td>
      <td><a id="${member.get("id")}" class="member" href="#${member.get("id")}">${member.get("name")}</a></td>
      <td>${member.get("host")}</td>
      <td>${member.get("port")}</td>
      <td class="number">${member.get("heap")}</td>
      <td class="number">${member.get("cpu")}</td>
      <td>${member.get("uptime")}</td>
      <td class="number">${member.get("clients").size()}</td>
      <td>${member.get("type")}</td>
    </tr>
    <% } %>
  <% } %>
  </tbody>
</table>
