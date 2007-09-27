<html>
<body>
  <h1>LiveData commands for <%= link_to resource.name, [resource:resource]  %> 
      (<%= resource.appdefResourceTypeValue.name %>)</h1>
  
  <p>
  <%= link_to "<-- Go Back", [action:'index'], [:] %>
  </p>
  Available commands:
  <ul>
    <% for (c in cmds) { %>
        <li> 
          <%= link_to c, [:],  [id:resource.id, command:c] %>
        </li>
    <% } %>
  </ul>
  
  <% if (result != null) { %>
    <b>$command</b> result:<br/>
    <pre>
<%= h(result) %>
    </pre>
  <% } %>  
</body>
</html>
