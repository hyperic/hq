<html>
<head>
	<title>HQ Alert Center</title>

    <script type="text/javascript">
        var djConfig = {
            isDebug: true
        };
    </script>

    <script src="/js/dojo/dojo.js" type="text/javascript">
    </script>

    <script type="text/javascript">
        dojo.require("dojo.widget.FilteringTable");
        dojo.hostenv.writeIncludes();
    </script>
    <script type="text/javascript">
        function getDojo() {
            dojo.require("dojo.widget.FilteringTable");
            dojo.hostenv.writeIncludes();
        }
    </script>	
    
</head>
	
<body>
	<%= template.body %>
</body>
</html>
