<resources>
<% for (r in resources) { %>
  <resource id="${r.id}" instanceId="${r.instanceId}" name="${h r.name}"/>
<% } %>
</resources>
