<div id="systemsDownTable">
  <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
                refresh:60, pageControls:false, url:urlFor(action:'data'),
                schema:systemsDownSchema, numRows:100) %>
</div>
