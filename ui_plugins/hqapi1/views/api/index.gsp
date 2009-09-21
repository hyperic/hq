<script type="text/javascript">
    document.navTabCat = "Admin";
</script>
<html>
    <%
        def version = plugin.descriptor.get('plugin.version')
        def clientPackage = "hqapi1-" + version + ".tar.gz"
        def apijarUrl = "/" + urlFor(asset:clientPackage)
    %>

    <h2>Hyperic HQ API Version ${version}</h2>

    This page provides resources and documentation for the Hyperic HQ API.<br>

    <h3>Download</h3>
    <ul>
        <li>
            Download <a href="${apijarUrl}">${clientPackage}</a>
        </li>
    </ul>

    <h3>Current method statistics</h3>

    <table border="1">
        <thead>
            <tr>
                <td>Method</td><td>Total Calls</td><td>Min Time</td><td>Max Time</td><td>Total Time</td>                
            </tr>
        <%
            stats.each { k, v ->
        %>
                <tr>
                    <td>${k}</td><td>${v.calls}</td><td>${v.minTime}</td><td>${v.maxTime}</td><td>${v.totalTime}</td>
                </tr>
        <%
            }
        %>
        </thead>
    </table>
   
</html>