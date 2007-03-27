<html>
<head>
  <meta refresh http-equiv="refresh" content="5"/>
</head>

<body>
  <h1>JVM Stats for $group.name </h1>

  <table cellpadding="5" cellspacing="5" border="1">
    <thead>
    <tr>
      <th>JVM</th>
      <th>Commands</th>
      <th>Init</th>
      <th>Max</th>
      <th>Committed</th>
      <th>Used</th>
    </tr>
    </thead>

    <tbody>
    <% stats.eachWithIndex { row, i -> %>
    <tr value="$i">
      <td><%= link_to row.resource.name, [resource:row.resource] %>
      <td><%= link_to 'gc', [action:'gc'],
              [type:row.resource.entityId.type,
               id:row.resource.entityId.id,
               group:group.id]
               %></td>
      <td>$row.data.init</td>
      <td>$row.data.max</td>
      <td>$row.data.committed</td>
      <td>$row.data.used</td>
    </tr>
    <% } %>
    </tbody>
  </table>
</body>
</html>
