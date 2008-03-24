<h3>Plugin information for <%= h plugin.name %></h3>

<%= linkTo '<-- Back to Index', [action:'index'] %>

<br/>
<br/>
<br/>
<h3>${l.Views}</h3>
<table>
<thead>
  <tr>
    <th>${l.Path}</th>
    <th>${l.Description}</th>
    <th>${l.AttachableTo}</th>
    <th>${l.CanAttach}</th>
    <th>${l.AttachedAt}</th>
  </tr>
</thead>
<tbody>
<% for (v in plugin.views) { %>
  <tr>
    <td><a href="/hqu/${plugin.name}${v.path}"><%= h v.path %></a></td>
    <td><%= h v.description %></td>
    <td><%= h v.attachType.description %></td>
    <td><%= h v.isAttachable(v.prototype) %></td>
    <% if (v.isAttachable(v.prototype)) { %>
      <td><%= buttonTo "Attach", [action : 'attach', id : v] %></td>
    <% } %>
  </tr>
  <% for (a in v.attachments) { %>
    <tr>
      <td/>
      <td>
        <%= h a.toString() %>
      </td>
      <td/>
      <td/>
      <td>
        <%= format_date("yy.MM.dd HH:mm:ss z", new Date(a.attachTime)) %>
      </td>
      <td>
        <%= buttonTo "Detach", [action : 'detach', id : a] %></td>
      </td>
    </tr>
  <% } %>

<% } %>
</tbody>
</table>
