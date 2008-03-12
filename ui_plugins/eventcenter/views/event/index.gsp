<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>

<link rel=stylesheet href="/hqu/public/hqu.css" type="text/css">


<div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
  <div style="float:left;width:18%;margin-right:10px;">
    <div class="filters">
      <div class="BlockTitle">${l.filter}</div>
      <div class="filterBox">
        <div class="fieldSetStacked" style="margin-bottom:8px;">
          <span><strong>${l.minStatus}</strong></span>
          <div><%= selectList(allStatusVals, 
     	                     [id:'statusSelect',
     	                      onchange:'EventLogs_refreshTable(); ']) %>
     	  </div>
        </div>          

        <div class="fieldSetStacked" style="margin-bottom:8px;">
          <span><strong>${l.type}</strong></span>
          <div><%= selectList(allTypes,
     	                     [id:'typeSelect',
     	                      onchange:'EventLogs_refreshTable(); ']) %>
     	  </div>
     	</div>
     	
        <div class="fieldSetStacked" style="margin-bottom:8px;">
          <span><strong>${l.timeRange}</strong></span>
          <div><%= selectList(timePeriods,
     	                     [id:'timeSelect',
     	                      onchange:'EventLogs_refreshTable(); ']) %>
     	  </div>
     	</div>
     	
        <div class="fieldSetStacked" style="margin-bottom:8px;">
          <span><strong>${l.inGroups}</strong></span>
     	  <div>
            <select id="groupSelect" multiple="true" name="groupSelect"
			        style="height:200px; width:175px; border:5px solid #ededed;"
			        onchange="EventLogs_refreshTable();">
     	      <% for (g in allGroups) { %>
			    <option value="${g.id}">${g.name}</option>
     	     <% } %>
		    </select>
     	  </div>
        </div>          
        
      </div>
    </div>
  </div>
  <div style="float:right;width:78%;display:inline;height: 445px;overflow-x: hidden; overflow-y: auto;" 
       id="logsCont">
    <div>
      <%= dojoTable(id:'EventLogs', title:l.logs,
                    refresh:600, url:urlFor(action:'logData'),
                    schema:logSchema, numRows:15) %>
    </div>
  </div>
</div>
             
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
