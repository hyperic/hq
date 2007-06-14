<div id="alert" style="width:98%;padding-left:10px;">
<%= dojoTable(id:'content',
              columns:['Date', 'Alert', 'Resource', 'Fixed', 'Severity' ],
              url:urlFor(action:'data')) %>
</div> 
