<html>
<body>
  <h1>Top ... on Crack</h1>
  <img src="/dojo/top_box.jpg" width="320" height="240"/>
  <ul>
  <% for (g in groups) { %>
    <li> 
      <%= link_to g.name, [action:'showCrack'], [id:g.id] %>
    </li>   
  <% } %>
  </ul>
</body>
</html>
