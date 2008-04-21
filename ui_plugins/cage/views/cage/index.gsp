<div id="critters">
  <% if (critterList.isAny()) { %>
    ${l.ifAny}
  <% } else { %>
    ${l.ifAll}
  <% } %>

  <ul>
  <% for (c in group.critterList.critters) { %>
    <li><%= h c.config %></li>
  <% } %>
  </ul>
</div>

<div id="resources">
  <h3>Current group members</h3>
  <table>
    <% for (r in group.resources) { %>
      <tr><td><%= h r.name %></td></tr>
    <% } %>
  </table>
</div>

<div id="matchResources">
  <h3>Proposed group members</h3>
  <table>
    <% for (r in proposedResources) { %>
      <tr><td><%= h r.name %></td></tr>
    <% } %>
  </table>
</div>
