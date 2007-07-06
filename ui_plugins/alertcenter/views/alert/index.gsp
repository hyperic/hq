<script type="text/javascript">
function swapTables(sel) {
     if (sel=="Alerts")  {
        dojo.html.show('alertsTable');
        dojo.html.hide("groupalertsTable")
      }
    if (sel=="Group Alerts")  {
      dojo.html.hide('alertsTable');
        dojo.html.show("groupalertsTable")
      }
}
</script>
<div class="BlockTitle"> Select Alert type:
<select onchange=swapTables(options[selectedIndex].text)>
<option selected="yes">Alerts</option>
<option>Group Alerts</option>
</select>
</div>
<div id="alertsTable">
  <%= dojoTable(id:'Alerts', url:urlFor(action:'data'),
                schema:alertSchema, numRows:25) %>
</div> 
<div id="groupalertsTable" style="display:none;">
  <%= dojoTable(id:'GroupAlerts', url:urlFor(action:'data'),
                schema:alertSchema, numRows:25) %>
</div>