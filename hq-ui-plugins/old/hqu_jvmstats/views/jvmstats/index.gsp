<html>
<body>
  <h1>JVM Stats</h1>
  <ul>
  <% for (g in groups) { %>
    <li>
      <%= link_to g.name, [action:'showStats'], [id:g.id] %>
    </li>
  <% } %>
  </ul>
</body>
</html>

