<h3>Plugin information for <%= h plugin.name %></h3>

<%= link_to '<-- Back to Index', [action:'index'] %>

<br/>
<br/>
<br/>
<h3>${l.Views}</h3>
<table>
<thead>
  <tr>
    <th>${l.Path}</th>
    <th>${l.AttachableTo}</th>
    <th>${l.CanAttach}</th>
    <th>${l.AttachedAt}</th>
  </tr>
</thead>
<tbody>
<% for (v in plugin.views) { %>
  <tr>
    <td><%= h v.path %></td>
    <td><%= h v.attachType.description %></td>
    <td><%= h v.attachable %></td>
    <% if (v.attachable) { %>
      <td><%= button_to "Attach", [action : 'attach', id : v] %></td>
    <% } %>
  </tr>
  <% for (a in v.attachments) { %>
    <tr>
      <td/>
      <td/>
      <td/>
      <td>
        <%= format_date("yy.MM.dd HH:mm:ss z", new Date(a.attachTime)) %>
      </td>
      <td>
        <%= button_to "Detach", [action : 'detach', id : a] %></td>
      </td>
    </tr>
  <% } %>

<% } %>
</tbody>
</table>
