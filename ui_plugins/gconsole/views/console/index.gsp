Templates: 
<% for(t in templates) { %>
  <%= link_to t.key, [action:'chooseTemplate', template:t.key] %><br/>
<% } %>
<% formFor([action:'index']) { f -> %>
<p>
  <%= f.text_area(name:'code_input', value:r['last_code']) %>
</p>
<p>
  <%= f.submit_button([label:'Execute']) %>
</p>
<% } %>

<div>
  <% render(partial:'result', locals:[foo:'bar']) %>
</div>
  