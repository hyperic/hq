<script type="text/javascript">
function swapTables(sel) {
    if (sel=="0")  {
        dojo.html.show('alertsTable');
        dojo.html.hide("groupalertsTable");
        dojo.html.hide("defsTable");
    }
    if (sel=="1")  {
        dojo.html.hide('alertsTable');
        dojo.html.show("groupalertsTable");
        dojo.html.hide('defsTable');
    }
    if (sel=="2")  {
        dojo.html.hide('alertsTable');
        dojo.html.hide("groupalertsTable");
        dojo.html.show("defsTable");
    }
}

function setSelectedOption() {
  var selectDrop = document.getElementById('selectDrop');
  selectDrop.selectedIndex = selectDrop.options[0];
}

onloads.push(setSelectedOption);
</script>

<div class="BlockTitle"> ${l.SelectTypeMsg}:
  <select id='selectDrop' onchange='swapTables(options[selectedIndex].value)'>
    <option value='0' selected>${l.ClassicAlerts}</option>
    <% if (isEE) { %>
      <option value='1'>${l.GroupAlerts}</option>
    <% } %>
    <option value='2'>${l.ClassicDefs}</option>
  </select>
</div>

<div id="alertsTable">
  <%= dojoTable(id:'Alerts', url:urlFor(action:'data'),
                schema:alertSchema, numRows:15) %>
</div>

<div id="groupalertsTable" style="display:none;">
<% if (isEE) { %>
  <%= dojoTable(id:'GroupAlerts', url:urlFor(action:'groupData'),
                schema:galertSchema, numRows:15) %>
<% } %>
</div>           

<div id="defsTable" style="display:none;">
  <%= dojoTable(id:'Defs', url:urlFor(action:'defData'),
                schema:defSchema, numRows:15) %>
</div>
