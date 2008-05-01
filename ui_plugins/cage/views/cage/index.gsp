<div id="critters">
  <% if (critters) { %>
    <% if (isAny) { %>
      ${l.ifAny}
    <% } else { %>
      ${l.ifAll}
    <% } %>

    <ul>
    <% for (c in critters) { %>
      <li><%= h c.config %></li>
    <% } %>
    </ul>
  <% } %>
  
  <% if (systemCritters) { %>
    The following criteria is always met:
    <ul>
    <% for (c in systemCritters) { %>
      <li><%= h c.config %></li>
    <% } %>
    </ul>
  <% } %>
  
  <% if (!critters && !systemCritters) { %>	
    No Criteria specified
  <% } %>
</div>

<div id="resources">
  <h3>Current group members</h3>
  <table>
    <% for (r in group.resources) { %>
      <tr><td><%= h r.name %></td></tr>
    <% } %>
  </table>
</div>

<% if (proposedResources != null) { %>
  <div id="matchResources">
    <h3>Proposed group members</h3>
    <table>
      <% for (r in proposedResources) { %>
        <tr><td><%= h r.name %></td></tr>
      <% } %>
    </table>
  </div>
<% } %>
