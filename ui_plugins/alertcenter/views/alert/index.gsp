<% if (isEE) { %>
<script type="text/javascript">
     var onloads = new Array();
    function bodyOnLoad() {
      for ( var i = 0 ; i < onloads.length ; i++ )
        onloads[i]();
    }

function swapTables(sel) {
    if (sel=="0")  {
        dojo.html.show('alertsTable');
        dojo.html.hide("groupalertsTable")
    }
    if (sel=="1")  {
        dojo.html.hide('alertsTable');
        dojo.html.show("groupalertsTable")
    }
}

function setSelectedOption() {
  var selectDrop = document.getElementById('selectDrop');
  selectDrop.selectedIndex = selectDrop.options[0];
}

onloads.push(setSelectedOption);
</script>

<div class="BlockTitle"> ${l.SelectTypeMsg}:
  <select id='selectDrop' onchange=swapTables(options[selectedIndex].value)>
    <option value='0' selected>${l.ClassicAlerts}</option>
    <option value='1'>${l.GroupAlerts}</option>
  </select>
</div>

<div id="groupalertsTable" style="display:none;">
  <%= dojoTable(id:'GroupAlerts', url:urlFor(action:'groupData'),
                schema:galertSchema, numRows:15) %>
</div>                
<% } %>

<div id="alertsTable">
  <%= dojoTable(id:'Alerts', url:urlFor(action:'data'),
                schema:alertSchema, numRows:15) %>
</div>
