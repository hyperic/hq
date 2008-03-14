<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>

<link rel=stylesheet href="/hqu/public/hqu.css" type="text/css">

<%  hquTwoPanedFilter() { w ->
        w.filter(l.filter) {
            w.filterElement(l.minStatus) { 
                out.write(selectList(allStatusVals, 
     	                             [id:'statusSelect',
                                      onchange:'EventLogs_refreshTable(); ']))
            }
            
            w.filterElement(l.type) {
                out.write(selectList(allTypes,
     	                             [id:'typeSelect',
                                      onchange:'EventLogs_refreshTable(); ']))
            }
            
            w.filterElement(l.timeRange) {
                out.write(selectList(timePeriods,
     	                             [id:'timeSelect',
                                      onchange:'EventLogs_refreshTable(); ']))
            }

            w.filterElement(l.inGroups) { %>
                <select id="groupSelect" multiple="true" name="groupSelect"
			            style="height:200px; width:175px; border:5px solid #ededed;"
			            onchange="EventLogs_refreshTable();">
     	        <% for (g in allGroups) { %>
			      <option value="${g.id}">${g.name}</option>
                <% } %>
                </select>
            <%
            }
        }
        w.pane {
            out.write(dojoTable(id:'EventLogs', title:l.logs,
                                refresh:600, url:urlFor(action:'logData'),
                                schema:logSchema, numRows:15))
        }
    }%>

<script type="text/javascript">
    function getEventLogsUrlMap(id) {
        var res = {};
        var statusSelect = dojo.byId('statusSelect');
        var typeSelect   = dojo.byId('typeSelect');
        var timeSelect   = dojo.byId('timeSelect');
        var groupSelect  = dojo.byId('groupSelect');
        res['minStatus'] = statusSelect.options[statusSelect.selectedIndex].value;
        res['type']      = typeSelect.options[typeSelect.selectedIndex].value;
        res['timeRange'] = timeSelect.options[timeSelect.selectedIndex].value;
        res['groups']    = "";
        for (i=0; i<groupSelect.length; i++) {
            if (groupSelect.options[i].selected) {
                res['groups'] = res['groups'] + "," + groupSelect.options[i].value;
            }
        }
        return res;
    }
    
    EventLogs_addUrlXtraCallback(getEventLogsUrlMap);
</script>
