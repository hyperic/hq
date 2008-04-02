<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>
<%= hquStylesheets() %>

<%= dojoTable(id:'methodTable', title:'Methods',
              refresh:60, url:urlFor(action:'methodData'),
              schema:methodSchema, numRows:500, pageControls:false) %>
