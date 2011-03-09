<script type="text/javascript">
var fmt      = {};
var commands = [];
var cmd;
var liveResults = [];
var resInfo = {};  
var ajaxCount = 0;
var lastSelected = undefined;

<% for (m in groupMembers) { %>
  resInfo['${m.entityId}'] = {name: "<%= linkTo(h(m.name), [resource:m]) %>" };
<% } %>

<% for (c in commands) { %>
    cmd = '<%= c %>';
    commands.push(cmd);
    fmt[cmd] = [];
    
    <% for (f in cmdFmt[c]) { %>
      fmt[cmd].push('<%= f %>');
    <% } %>
<% } %>

function showResult(eid) {
  hqDojo.byId("results_msgLive").innerHTML = "Results of " + liveResults.command +
                                       " for " + resInfo[eid].name;
  var results = liveResults.results;
  for (var i=0; i<results.length; i++) {
    var r = results[i];
    if (r.rid == eid) {
      if (r.result) {
      hideErrorPanel();
      hqDojo.byId('result').innerHTML = r.result;
      } else {
      handleError(r.error);
      //hqDojo.byId('result').innerHTML = r.error;
      }
      break;
    }
  }
  <% if (isGroup) { %>
    if (lastSelected) {
      hqDojo.byId('mem_' + lastSelected).style.color = 'black';
      hqDojo.byId('mem_' + lastSelected).style.fontWeight = 'normal';
    }
    hqDojo.byId('mem_' + eid).style.color = '#60A5EA';
    hqDojo.byId('mem_' + eid).style.fontWeight = 'bold';
    lastSelected = eid;
  <% } %>
}

function processResult(result) {
  liveResults = result;
  <% if (!isGroup) { %>
    showResult('${eid}');
  <% } else { %>
    hqDojo.byId("groupMembers").className = 'hasData';
  
    var res = result.results;
    for (var i=0; i<res.length; i++) {
      var r = res[i];
      
      if (r.result) {
        hqDojo.byId('clicker_' + r.rid).className = 'goodResults';
      } else {
        hqDojo.byId('clicker_' + r.rid).className = 'errorResults';
      }
      if (lastSelected) {
        showResult(lastSelected);
      }
    }
  <% } %>
}

function runCommand() {
  	var cmdSelect = hqDojo.byId('commandSelect');

    if (cmdSelect.selectedIndex == 0) return;
    
  	var cmd = cmdSelect.options[cmdSelect.selectedIndex].value;
  	var url = '<%= urlFor(action:'invoke') %>' + 
            '?cmd=' + cmd + 
            '&eid=<%= eid %>';
  	var fmtSelect = hqDojo.byId('fmt_' + cmd);

    if (fmtSelect.selectedIndex != -1) {
    	var fmt = fmtSelect.options[fmtSelect.selectedIndex].value;

        url = url + '&formatter=' + fmt;
  	} 

  	if (++ajaxCount > 0) {
    	hqDojo.byId("spinner").style.visibility = 'visible';  
  	}
    
	hqDojo.xhrGet({
    	url: url,
    	handleAs: "json-comment-filtered",
    	load: function(response, args) {
      		if (--ajaxCount == 0) {
        		hqDojo.byId("spinner").style.visibility = 'hidden';  
      		}

            processResult(response);
    	},
    	error: function(response, args) {
      		//alert('There has been an error:  ' + err);
    	}
  	});
}

function handleError(er) {
    var msgPanelObj = hqDojo.byId("messagePanel");
    if(msgPanelObj.style.display != "block") {
        msgPanelObj.style.display = "block";
    }

    if (er.search(/Unknown command/) < 0)
        hqDojo.byId("messagePanelMessage").innerHTML = er;
    else
        hqDojo.byId("messagePanelMessage").innerHTML = "${l.agentUnknownCommand}";
}

function hideErrorPanel() {
      var msgPanelObj = hqDojo.byId("messagePanel");
            if(msgPanelObj.style.display = "block") {
            msgPanelObj.style.display = "none";
            hqDojo.byId("messagePanelMessage").innerHTML = '';
            }
}

var legends = {};
legends['cpuinfo'] = '${l.cpuinfo}';
legends['cpuperc'] = '${l.cpuperc}';
legends['df'] = '${l.df}';
legends['ifconfig'] = '${l.ifconfig}';
legends['netstat'] = '${l.netstat}';
legends['top'] = '${l.top}';
legends['who'] = '${l.who}';


function updateLegend(select){
    var legendDiv = hqDojo.byId("legend");
    if(select.selectedIndex <= 0){
        legendDiv.innerHTML = "";
        return;
    }
    legendDiv.innerHTML = legends[select.options[select.selectedIndex].value];
}

hqDojo.ready(function(){
    updateLegend(hqDojo.byId("commandSelect"));
});


</script>
<div class="messagePanel messageInfo" style="display:none;" id="messagePanel"><div class="infoIcon"></div><span id="messagePanelMessage"></span></div>
<div class="outerLiveDataCont" style="clear:both;">

  <div class="leftbx">

    <div class="leftboxborder">

      <div class="BlockTitle"><div style="float:left;">Execute Command</div><div class="acLoader2" id="spinner" style="display:inline;float:right;"></div>
      <br class="clearBoth">
      </div>

      <div class="fivepad">

        <div style="padding-left:5px;">
            <div class="instruction1">Please select a query to run:</div>
        <select id="commandSelect" onchange="runCommand();updateLegend(this);" style="margin-bottom:5px;">
        <% for (c in commands) { %>
          <option value="${c}">${h c}</option>
        <% } %>
      </select>
      </div>
      <div id="legend" style="padding: 1px 5px 5px 2px; font-style: italic;"></div>
      <% if (isGroup) { %>
        <div class="grpmembertext">Group Members</div>
        <div id="groupMembers" class="pendingData">
        <ul>
        <% for (m in groupMembers) { %>
        <li>
        <div id="clicker_${m.entityId}" style="float:left;display:inline;height:16px;width:18px;" class="restingExec" onclick="showResult('${m.entityId}')" title="Click to view query information on this resource">&nbsp;&nbsp;&nbsp;&nbsp;</div>
        <div class="groupMemberName"><span id="mem_${m.entityId}">${h m.name}</span></div>

            <br class="clearBoth">
        </li>
        <% } %>
        </ul>
        </div>
        <% } %>

    <div id="formatters_cont">
      <% for (c in commands) { %>
      <div id="fmt_cont_${c}" style="display:none">
        Formatter:
        <select id="fmt_${c}">
          <% for (f in cmdFmt[c]) { %>
            <option value="${f}">${formatters[f].name}</option>
          <% } %>
        </select>
      </div>
      <% } %>
    </div>


  </div>
</div>

</div>

<div id="result_cont">
  <div id="results_msgLive"></div>
  <div id="result" class="bxblueborder"></div>
</div>
 <div style="height:1px;width:1px;clear:both;">&nbsp;</div>
</div>
