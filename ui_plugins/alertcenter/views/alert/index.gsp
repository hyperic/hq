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
  
function refreshDefTables() {
  Defs_refreshTable();
  TypeDefs_refreshTable();
  GalertDefs_refreshTable();
}

function selectDefType(t) {
  if (t == '1') {
    dojo.html.show('defsTable')
    dojo.html.show('excludeTypeBasedInput')
    <% if (superUser) { %> dojo.html.hide('typeDefsTable') <% } %>
    dojo.html.hide('galertDefsTable')
  } else if (t == '2') {
    dojo.html.hide('defsTable')
    dojo.html.hide('excludeTypeBasedInput')
    <% if (superUser) { %> dojo.html.show('typeDefsTable') <% } %>
    dojo.html.hide('galertDefsTable')
  } else if (t == '3') {
    dojo.html.hide('defsTable')
    dojo.html.hide('excludeTypeBasedInput')
    <% if (superUser) { %> dojo.html.hide('typeDefsTable') <% } %>
    dojo.html.show('galertDefsTable')
  }
}
 
function setSelectedOption() {
  <% if (!isEE) { %>
    selectAlertType('1');
    selectDefType('1');
    return;
  <% } else { %>
    var selectDrop = document.getElementById('alertSelect')
    selectAlertType(selectDrop.options[selectDrop.selectedIndex].value);
    selectDrop = document.getElementById('defSelect')
    selectDefType(selectDrop.options[selectDrop.selectedIndex].value);
  <% } %>
}

onloads.push(setSelectedOption);
</script>

<div dojoType="TabContainer" id="mainTabContainer" 
     style="width: 100%; height:500px;">
  <div dojoType="ContentPane" label="Alerts">
    <div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
      <div style="float:left;width:18%;margin-right:10px;">
        <div class="filters">
          <div class="BlockTitle">${l.AlertFilter}</div>
          <div class="filterBox">
            <% if (isEE) { %>
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.AlertType}:</strong></span>
              <select id="alertSelect" 
                      onchange='selectAlertType(options[selectedIndex].value)'>
                <option value='1'>${l.ClassicAlerts}</option>
                <option value='2'>${l.GroupAlerts}</option>
              </select>          
            </div>
            <% } %>
          
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.MinPriority}</strong></span>
              <%= selectList(severities, 
     	                     [id:'alertSevSelect',
     	                      onchange:'refreshAlertTables();']) %>
            </div>          

            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.InTheLast}</strong></span>
       	      <%= selectList(lastDays, 
     	                     [id:'alertTimeSelect',
     	                      onchange:'refreshAlertTables();']) %>
            </div>          
            </div>
        </div>
      </div>
      <div style="float:right;width:78%;display:inline;height: 445px;overflow-x: hidden; overflow-y: auto;" id="alertsCont">
        <div id="alertsTable" style="display:none;">
          <%= dojoTable(id:'Alerts', title:l.ClassicAlerts,
                          titleHtml: "<div class='refreshButton'><input type='button' value='Refresh' onclick='refreshTable();'/></div> ",
                        url:urlFor(action:'data'),
                        schema:alertSchema, numRows:15) %>
        </div>
        <div id="groupAlertsTable" style="display:none;">
          <%= dojoTable(id:'GroupAlerts', title:l.GroupAlerts,
                         titleHtml: "<div class='refreshButton'><input type='button' value='Refresh' onclick='refreshTable();'/></div> ",
                        url:urlFor(action:'groupData'),
                        schema:galertSchema, numRows:15) %>
        </div>
      </div>
    </div>
<div style="clear:both;height:1px;"></div>
  </div>
  
  <div dojoType="ContentPane" label="Definitions">
   <div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
      <div style="float:left;display:inline;width:18%;margin-right:10px;">
        <div class="filters">
          <div class="BlockTitle">${l.DefFilter}</div>
          <div class="filterBox">
            <% if (isEE) { %>
            <div class="fieldSetStacked" style="margin-bottom:8px;">
              <span><strong>${l.DefType}:</strong></span>
              <select id="defSelect"
                      onchange='selectDefType(options[selectedIndex].value)'>
                <option value='1'>${l.PlainDefs}</option>
                <option value='3'>${l.GroupDefs}</option>
                <% if (superUser) { %>
                  <option value='2'>${l.TypeBasedDefs}</option>
                <% } %>
              </select>          
            </div>
            
            <div id="excludeTypeBasedInput" class="fieldSetStacked" 
                 style="margin-bottom:8px;">
              <span><strong>${l.ExcludeTypeBased}:</strong></span>
              <input id="excludeTypeBox" type="checkbox" name="excludeTypeBased" 
                     value="true"  onchange="Defs_refreshTable();"/>
            </div>
            <% } %>

            <div id="onlyShowDisabled" class="fieldSetStacked" 
                 style="margin-bottom:8px;">
              <span><strong>${l.OnlyShowDisabled}:</strong></span>
              <input id="onlyShowDisabledBox" type="checkbox" name="onlyShowDisabled" 
                     value="true"  onchange="refreshDefTables();"/>
            </div>
            
          </div>
        </div>
      </div>
       <div style="float:right;display:inline;width:78%;height: 445px;overflow-x: hidden; overflow-y: auto;" id="defsCont">
        <div id="defsTable" style="display:none;">
          <%= dojoTable(id:'Defs', title:l.ClassicDefs,
                        url:urlFor(action:'defData'),
                        schema:defSchema, numRows:15) %>
        </div>
      
        <div id="typeDefsTable" style="display:none;">
          <% if (superUser) { %>
            <%= dojoTable(id:'TypeDefs', title:l.TypeDefs,
                          url:urlFor(action:'typeDefData'),
                          schema:typeDefSchema, numRows:15) %>
          <% } %>
        </div>    

        <div id="galertDefsTable" style="display:none;">
          <%= dojoTable(id:'GalertDefs', title:l.GroupDefsTitle,
                        url:urlFor(action:'galertDefData'),
                        schema:galertDefSchema, numRows:15) %>
        </div>    
      </div>
    </div>
 <div style="clear:both;height:1px;"></div>
  </div>
</div>

<script type="text/javascript">
    function getAlertsUrlMap(id) {
        var res = {};
        var sevSelect  = dojo.byId('alertSevSelect');
        var timeSelect = dojo.byId('alertTimeSelect');
        res['minPriority'] = sevSelect.options[sevSelect.selectedIndex].value;
        res['alertTime']   = timeSelect.options[timeSelect.selectedIndex].value;
        return res;
    }
    
    function getDefsUrlMap(id) {
        var res = {};
        <% if (isEE) { %>
          res['excludeTypes']   = dojo.byId('excludeTypeBox').checked;
        <% } %>
        res['onlyShowDisabled'] = dojo.byId('onlyShowDisabledBox').checked;        
        return res;
    }
    
    Alerts_addUrlXtraCallback(getAlertsUrlMap);
    Defs_addUrlXtraCallback(getDefsUrlMap);

    <% if (isEE) { %>
      GroupAlerts_addUrlXtraCallback(getAlertsUrlMap);
      TypeDefs_addUrlXtraCallback(getDefsUrlMap);
      GalertDefs_addUrlXtraCallback(getDefsUrlMap);
    <% } %>
</script>