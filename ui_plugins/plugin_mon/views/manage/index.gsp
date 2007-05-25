<h3>UI Plugin Management</h3>
<table>
<thead>
  <tr>
    <th>Name</th>
    <th>Version</th>
    <th>Description</th>
  </tr>
</thead>
<tbody>
<% for (p in plugins) { %>
  <tr>
    <td><%= link_to p.name, [action:'showPlugin', id:p] %></td>    
    <td><%= h p.pluginVersion %></td>    
    <td><%= h p.description %></td>    
  </tr>
<% } %>
</tbody>
</table>
