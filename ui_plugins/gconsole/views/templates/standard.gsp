<html>
<head>
	<title>HQ Groovy Console</title>
<script src="/js/dojo/dojo.js" type="text/javascript"></script>
<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>
</head>
	
<body>
	<%= template.body %>

    <% render(partial:'footer') %>
</body>
</html>
