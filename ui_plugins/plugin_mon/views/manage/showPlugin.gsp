<h3>Plugin information for <%= h plugin.name %></h3>

<%= link_to '<-- Back to Index', [action:'index'] %>

<br/>
<br/>
<br/>
Views
<table>
<thead>
  <tr>
    <th>Path</th>
    <th>Attachable to</th>
    <th>Can attach?</th>
  </tr>
</thead>
<tbody>
<% for (v in plugin.views) { %>
  <tr>
    <td><%= h v.path %></td>
    <td><%= h v.attachType.description %></td>
    <td><%= h v.attachable %></td>
    <td><%= button_to "Attach", [action : 'attach', id : v] %></td>
  </tr>
<% } %>
</tbody>
</table>
