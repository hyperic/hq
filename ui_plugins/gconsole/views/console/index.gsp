<% form_for([action:'index']) { f -> %>
<p>
  <%= f.text_area([name:'code_input', value:r['last_code']]) %>
</p>
<p>
  <%= f.submit_button([label:'Execute']) %>
</p>
<% } %>
  
<% render(partial:'result') %>
