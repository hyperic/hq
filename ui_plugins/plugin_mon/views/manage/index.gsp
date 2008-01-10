<h3>UI Plugin Management</h3>


<%= dojoTable(id:'pluginTable', title:l.Plugins,
              refresh:10, url:urlFor(action:'pluginData'),
              schema:pluginSchema, numRows:500, pageControls:false) %>
