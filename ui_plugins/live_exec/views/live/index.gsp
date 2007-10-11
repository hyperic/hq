<p>
  <select>
  <% for (p in platforms) { %>
    <option value="${p.id}">${h p.name}</option>
  <% } %>
  </select>
</p>
