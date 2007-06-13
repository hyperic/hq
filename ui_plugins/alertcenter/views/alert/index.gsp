<div id="alert" style="width:98%;margin-left:auto;margin-right:auto;margin-top:20px;margin-bottom:20px;">

<%= dojoTable(id:'content',
              columns:['Date', 'Alert', 'Resource', 'Fixed', 'Severity' ],
              url:urlFor(action:'data')) %>
</div> 
