<%= hquStylesheets() %>
<script type="text/javascript">
function selectAlertType(t) {
  if (t == "1") {
    hyperic.html.show('alertsTable');
    hyperic.html.hide('groupAlertsTable');
  } else if (t == "2") {
    hyperic.html.hide('alertsTable');
    hyperic.html.show('groupAlertsTable');
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
    hyperic.html.show('defsTable');
    hyperic.html.show('excludeTypeBasedInput');
    Defs_refreshTable();
    <% if (superUser) { %> 
    	hyperic.html.hide('typeDefsTable');
    <% } %>
    hyperic.html.hide('galertDefsTable');
  } else if (t == '2') {
    hyperic.html.hide('defsTable');
    hyperic.html.hide('excludeTypeBasedInput');
    <% if (superUser) { %> 
    	hyperic.html.show('typeDefsTable');
    	TypeDefs_refreshTable();
    <% } %>
    hyperic.html.hide('galertDefsTable');
  } else if (t == '3') {
    hyperic.html.hide('defsTable');
    hyperic.html.hide('excludeTypeBasedInput');
    <% if (superUser) { %> 
    	hyperic.html.hide('typeDefsTable');
    <% } %>
    hyperic.html.show('galertDefsTable');
    GalertDefs_refreshTable();
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

hqDojo.ready( function(){
    setSelectedOption();
});
</script>
<div id="fullBody" style="clear:both">
<% dojoTabContainer(id:'mainTabContainer', style:'width:100%;height:500px;') { %>
	<% dojoTabPane(id:'alertsContentPane', label:'Alerts') { %>
			<div style="padding:5px;">
				<div style="float:left;width:200px;margin-right:10px;">
	        		<div class="filters">
	          			<div class="BlockTitle">${l.AlertFilter}</div>
	          			<div class="filterBox">
	            			<div class="fieldSetStacked" style="margin-bottom:8px;">
	              				<span>${l.Show}:</span>
	              				<div>
	                				<input type="radio" id="notFixed" name="show" value="notfixed" onclick="refreshAlertTables()"><label for="notFixed">${l.NotFixed}</label><br>
	                				<input type="radio" id="escOnly" name="show" value="inescalation" onclick="refreshAlertTables()"><label for="escOnly">${l.InEscalation}</label><br>
	                				<input type="radio" id="all" name="show" value="all" checked="checked" onclick="refreshAlertTables()"><label for="all">${l.All}</label>
	              				</div>          
	            			</div>
	
	            			<% if (isEE) { %>
	            				<div class="fieldSetStacked" style="margin-bottom:8px;">
	              					<span><label for="alertSelect">${l.AlertType}</label>:</span>
	              					<div>
										<select id="alertSelect" name="alertSelect" onchange='selectAlertType(options[selectedIndex].value)'>
	                						<option value='1'>${l.ClassicAlertsSelect}</option>
	                						<option value='2'>${l.GroupAlertsSelect}</option>
	              						</select>
	              					</div>          
	            				</div>
	            			<% } %>
	          
	            			<div class="fieldSetStacked" style="margin-bottom:8px;">
	              				<span><label for="alertSevSelect">${l.MinPriority}</label>:</span>
	              				<div>
									<%= selectList(severities, [ id:'alertSevSelect',
	                              			name:'alertSevSelect',
	                              			onchange:'refreshAlertTables();' ], null) %>
	              				</div>
	            			</div>          
	            			<div class="fieldSetStacked" style="margin-bottom:8px;">
	              				<span><label for="alertTimeSelect">${l.InTheLast}</label>:</span>
	              				<div>
								  	<%= selectList(lastDays, [ id:'alertTimeSelect',
	                              			name:'alertTimeSelect',
	                              			onchange:'refreshAlertTables();' ], null) %>
	                              
	              				</div>
	            			</div>
	            			<div class="fieldSetStacked" style="margin-bottom:8px;">
	              				<span><label for="alertGroupSelect">${l.GroupFilter}</label>:</span>
	              				<div>
								  	<%= selectList(groups, [ id:'alertGroupSelect',
	                              			name:'alertGroupSelect',
	                              			onchange:'refreshAlertTables();']) %>
	                              
	              				</div>
	            			</div>
	            		</div>
					</div>
				</div>
				<div style="width:auto;height: 445px;overflow-x: hidden; overflow-y: auto;">
	        		<div id="alertsTable" style="width:auto;display:none;">
	          			<form id="Alerts_FixForm" name="Alerts_FixForm" method="POST" action="<%= urlFor(absolute:"/alerts/RemoveAlerts.do", encodeUrl:true) %>">
	          				<div id="Alerts_DataDiv" style="height: 400px;overflow-x: hidden; overflow-y: auto;">
	          					<%= dojoTable(id:'Alerts', title:l.ClassicAlertsTable,
	                        			refresh:60, url:urlFor(action:'data'),
	                        			schema:alertSchema, numRows:15) %>
	          				</div>
	          				<hr/>
	          				<div id="Alerts_AckInstruction" style="font-style:italic; float:right;">
	          					Click the <img src="/images/icon_ack.gif"/> icon to acknowledge an alert
	          				</div>
	          				<div id="Alerts_FixedButtonDiv" style="margin-top:6px">
					          	<input type="button" id="Alerts_FixButton" value="FIXED" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />
					          	&nbsp;&nbsp;
					          	<input type="button" id="Alerts_AckButton" value="ACKNOWLEDGE" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />          	
					          	<input type="hidden" name="buttonAction" value="" />
					          	<input type="hidden" name="output" value="json" />
					          	<input type="hidden" name="fixedNote" value="" />
					          	<input type="hidden" name="ackNote" value="" />
					          	<input type="hidden" name="fixAll" value="false" />
					          	<input type="hidden" name="pauseTime" value="" />
	          				</div>
	          			</form>
	        		</div>
	        		<div id="groupAlertsTable" style="display:none;">
	          			<form id="GroupAlerts_FixForm" name="GroupAlerts_FixForm" method="POST" action="<%= urlFor(absolute:"/alerts/RemoveAlerts.do", encodeUrl:true) %>">
	          				<div id="GroupAlerts_DataDiv" style="height: 400px;overflow-x: hidden; overflow-y: auto;">
	          					<%= dojoTable(id:'GroupAlerts', title:l.GroupAlertsTable,
	                        			refresh:60, url:urlFor(action:'groupData'),
	                        			schema:galertSchema, numRows:15) %>
	          				</div>
	          				<hr/>
	          				<div id="GroupAlerts_AckInstruction" style="font-style:italic; float:right;">
	          					Click the <img src="/images/icon_ack.gif"/> icon to acknowledge an alert
	          				</div>
	          				<div id="GroupAlerts_FixedButtonDiv" style="margin-top:6px">
					          	<input type="button" id="GroupAlerts_FixButton" value="FIXED" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />
					          	&nbsp;&nbsp;
					          	<input type="button" id="GroupAlerts_AckButton" value="ACKNOWLEDGE" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />          	
					          	<input type="hidden" name="buttonAction" value="" />
					          	<input type="hidden" name="output" value="json" />
					          	<input type="hidden" name="fixedNote" value="" />
					          	<input type="hidden" name="ackNote" value="" />
					          	<input type="hidden" name="fixAll" value="false" />          	
					          	<input type="hidden" name="pauseTime" value="" />
	          				</div>          
	          			</form>
	        		</div>      		
		      		<div id="HQAlertCenterDialog" style="display:none;"></div>
		      		<script type="text/javascript">
						hqDojo.require("dijit.dijit");
			          	hqDojo.require("dijit.Dialog");
			          	hqDojo.require("dijit.ProgressBar");
			          	
			          	var MyAlertCenter = null;
			          	hqDojo.ready(function(){
			          		MyAlertCenter = new hyperic.alert_center("Alert Center");
			          		
			          		hqDojo.connect("Alerts_refreshTable", function() { MyAlertCenter.resetAlertTable(hqDojo.byId('Alerts_FixForm')); });
			          		hqDojo.connect("GroupAlerts_refreshTable", function() { MyAlertCenter.resetAlertTable(hqDojo.byId('GroupAlerts_FixForm')); });         		
			          	});
		      		</script>
		      	</div>
		     </div>
  	<% } %>
	<% dojoTabPane(id:'definitionsContentPane', label:'Definition') { %>
			<div style="padding:5px;">
				<div style="float:left;width:200px;margin-right:10px;">
	        		<div class="filters">
	          			<div class="BlockTitle">${l.DefFilter}</div>
	          			<div class="filterBox">
	            			<% if (isEE) { %>
	            				<div class="fieldSetStacked" style="margin-bottom:8px;">
	              					<span><label for="defSelect">${l.DefType}</label>:</span>
	              					<div>
									  	<select id="defSelect" name="defSelect" onchange='selectDefType(options[selectedIndex].value)'>
	                						<option value='1'>${l.ClassicDefsSelect}</option>
	                						<option value='3'>${l.GroupDefsSelect}</option>
	                						<% if (superUser) { %>
	                  							<option value='2'>${l.TypeBasedDefsSelect}</option>
	                						<% } %>
	              						</select>
	              					</div>          
	            				</div>
	            				<div id="excludeTypeBasedInput" class="fieldSetStacked" style="margin-bottom:8px;">
	              					<input id="excludeTypeBased" type="checkbox" name="excludeTypeBased" 
	                     				value="true"  onclick="Defs_refreshTable();"/>
	              					<label for="excludeTypeBased">${l.ExcludeTypeBased}</label>
	            				</div>
	            			<% } %>
	            			<div id="onlyShowDisabledInput" class="fieldSetStacked" style="margin-bottom:8px;">
	              				<input id="onlyShowDisabled" type="checkbox" name="onlyShowDisabled" 
	                     			value="true"  onclick="refreshDefTables();"/>
	              				<label for="onlyShowDisabled">${l.OnlyShowDisabled}</label>
	            			</div>
	          			</div>
	        		</div>
				</div>
				<div style="width:auto;height: 445px;overflow-x: hidden; overflow-y: auto;">
	        		<div id="defsTable" style="display:none;">
	          			<%= dojoTable(id:'Defs', title:l.ClassicDefsTable,
	                        url:urlFor(action:'defData'),
	                        schema:defSchema, 
							numRows:15) %>
	        		</div>
	        		<div id="typeDefsTable" style="display:none;">
	          			<% if (superUser) { %>
	            			<%= dojoTable(id:'TypeDefs', title:l.TypeDefsTable,
	                          		url:urlFor(action:'typeDefData'),
	                          		schema:typeDefSchema, 
									numRows:15) %>
	          			<% } %>
	        		</div>    
	        		<div id="galertDefsTable" style="display:none;">
	          			<%= dojoTable(id:'GalertDefs', 
	                    	    title:l.GroupDefsTable,
	                        	url:urlFor(action:'galertDefData'),
	                        	schema:galertDefSchema, 
	                        	numRows:15,
	                        	readOnly:true) %>
	        		</div>
	        	</div>    
			</div>
	<% } %>
<% } %>
</div>
<script type="text/javascript">
    function getAlertsUrlMap(id) {
        var res = {};
        var sevSelect  = hqDojo.byId('alertSevSelect');
        var timeSelect = hqDojo.byId('alertTimeSelect');
        var groupSelect = hqDojo.byId('alertGroupSelect');
        res['minPriority'] = sevSelect.options[sevSelect.selectedIndex].value;
        res['alertTime']   = timeSelect.options[timeSelect.selectedIndex].value;
        res['group']   = groupSelect.options[groupSelect.selectedIndex].value;

        var escOnly    = hqDojo.byId('escOnly');
        var notFixed   = hqDojo.byId('notFixed');
        if (escOnly.checked) {
          res['show'] = escOnly.value;
        }
        else if (notFixed.checked) {
          res['show'] = notFixed.value;
        }
        else {
          res['show'] = 'all';
        }

        return res;
    }
    
    function getDefsUrlMap(id) {
        var res = {};
        <% if (isEE) { %>
          res['excludeTypes']   = hqDojo.byId('excludeTypeBased').checked;
        <% } %>
        res['onlyShowDisabled'] = hqDojo.byId('onlyShowDisabled').checked;        
        return res;
    }
    
    Alerts_addUrlXtraCallback(getAlertsUrlMap);
    Defs_addUrlXtraCallback(getDefsUrlMap);

    <% if (isEE) { %>
      GroupAlerts_addUrlXtraCallback(getAlertsUrlMap);
      TypeDefs_addUrlXtraCallback(getDefsUrlMap);
      GalertDefs_addUrlXtraCallback(getDefsUrlMap);
    <% } %>
    
    hqDojo.subscribe("XHRComplete", function() {
    	setFoot();
    });
</script>
