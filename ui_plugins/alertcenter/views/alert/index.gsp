<script type="text/javascript">
function selectAlertType(t) {
  if (t == "1") {
    dojo.html.show('alertsTable');
    dojo.html.hide('groupAlertsTable');
  } else if (t == "2") {
    dojo.html.hide('alertsTable');
    dojo.html.show('groupAlertsTable');
  }
}

function refreshAlertTables() {
  Alerts_refreshTable();
  GroupAlerts_refreshTable();
}
  
function selectMinAlertPriority(val) {
  Alerts_getUrlXtras()['minPriority']      = val;
  GroupAlerts_getUrlXtras()['minPriority'] = val;
  refreshAlertTables();
}

function selectAlertTime(val) {
  Alerts_getUrlXtras()['alertTime']      = val;
  GroupAlerts_getUrlXtras()['alertTime'] = val;
  refreshAlertTables();
}

function selectDefType(t) {
  if (t == '1') {
    dojo.html.show('defsTable')
    dojo.html.show('excludeTypeBasedInput')
    dojo.html.hide('typeDefsTable')
    dojo.html.hide('galertDefsTable')
  } else if (t == '2') {
    dojo.html.hide('defsTable')
    dojo.html.hide('excludeTypeBasedInput')
    dojo.html.show('typeDefsTable')
    dojo.html.hide('galertDefsTable')
  } else if (t == '3') {
    dojo.html.hide('defsTable')
    dojo.html.hide('excludeTypeBasedInput')
    dojo.html.hide('typeDefsTable')
    dojo.html.show('galertDefsTable')
  }
}

</script>

<div dojoType="TabContainer" id="mainTabContainer" 
     style="width: 95%; height: 900px;">
  <div dojoType="ContentPane" label="Alerts">
    <div style="margin:5px;">
      <div style="float:left;width:18%;margin-right:12px;">
        <div class="filters">
          <div class="BlockTitle">${l.AlertFilter}</div>
          <div class="filterBox">
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.AlertType}:</strong></span>
              <select onchange='selectAlertType(options[selectedIndex].value)'>
                <option value='1'>${l.ClassicAlerts}</option>
                <option value='2'>${l.GroupAlerts}</option>
              </select>          
            </div>
          
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.MinPriority}</strong></span>
              <%= selectList(severities, 
     	                     [id:'alertSevSelect',
     	                      onchange:'selectMinAlertPriority(options[selectedIndex].value)']) %>
            </div>          

            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.InTheLast}</strong></span>
       	      <%= selectList(lastDays, 
     	                     [id:'alertTimeSelect',
     	                      onchange:'selectAlertTime(options[selectedIndex].value)']) %>
            </div>          
          </div>
        </div>
      </div>
      <div style="float:left;width:80%">
        <div id="alertsTable">
          <%= dojoTable(id:'Alerts', url:urlFor(action:'data'),
                        schema:alertSchema, numRows:15) %>
        </div>
        <div id="groupAlertsTable" style="display:none;">
          <%= dojoTable(id:'GroupAlerts', url:urlFor(action:'groupData'),
                        schema:galertSchema, numRows:15) %>
        </div>
      </div>
    </div>
  </div>
  
  <div dojoType="ContentPane" label="Definitions">
    <div style="margin:5px;">
      <div style="float:left;width:18%;margin-right:12px;">
        <div style="background-color:#EFEFEF">
          <div class="BlockTitle">${l.DefFilter}</div>
          <div style="padding:8px;">
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.DefType}:</strong></span>
              <select onchange='selectDefType(options[selectedIndex].value)'>
                <option value='1'>${l.PlainDefs}</option>
                <option value='2'>${l.TypeBasedDefs}</option>
                <option value='3'>${l.GroupDefs}</option>
              </select>          
            </div>
            <div id="excludeTypeBasedInput" class="fieldSetStacked" 
                 style="margin-bottom:8px;">
              <span><strong>${l.ExcludeTypeBased}:</strong></span>
              <input id="excludeTypeBox" type="checkbox" name="excludeTypeBased" 
                     value="true"  onchange="Defs_getUrlXtras()['excludeTypes'] = dojo.byId('excludeTypeBox').checked;  Defs_refreshTable();"/>
             </div>
           </div>
        </div>
      </div>
      <div style="float:left;width:80%">
        <div id="defsTable">
          <%= dojoTable(id:'Defs', url:urlFor(action:'defData'),
                        schema:defSchema, numRows:15) %>
        </div>
      
        <div id="typeDefsTable" style="display:none;">
          <%= dojoTable(id:'TypeDefs', url:urlFor(action:'typeDefData'),
                        schema:typeDefSchema, numRows:15) %>
        </div>    

        <div id="galertDefsTable" style="display:none;">
          <%= dojoTable(id:'GalertDefs', url:urlFor(action:'galertDefData'),
                        schema:galertDefSchema, numRows:15) %>
        </div>    
      </div>
    </div>
  </div>
</div>
