<html>
<body>
  <h1>Platform LiveData</h1>
  
  <ul>
  <% for (plat in platforms) { %>
    <li> <%= link_to plat.name, [action:'showResource'], 
                                [id:plat.id] %> </li>
  <% } %>
</body>
</html>
