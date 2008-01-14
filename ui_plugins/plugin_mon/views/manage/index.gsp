<%= hquStylesheets() %>

<h3>UI Plugin Management</h3>

<p>
Deleting a plugin will detach all views and remove the plugin from the 
database.
</p>

<p>
They can be redeployed from a development environment by 
issuing <i>ant hqu-redeploy-plugin -Dplugin=myPlugin</i>
</p>

<script type="text/javascript">
function refreshAll() {
    pluginTable_refreshTable();
};

</script>

<%= dojoTable(id:'pluginTable', title:l.Plugins,
              refresh:60, url:urlFor(action:'pluginData'),
              schema:pluginSchema, numRows:500, pageControls:false) %>
