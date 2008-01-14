<%= hquStylesheets() %>

<h3>UI Plugin Management</h3>

<script type="text/javascript">
function refreshAll() {
    pluginTable_refreshTable();
};

</script>

<%= dojoTable(id:'pluginTable', title:l.Plugins,
              refresh:10, url:urlFor(action:'pluginData'),
              schema:pluginSchema, numRows:500, pageControls:false) %>
