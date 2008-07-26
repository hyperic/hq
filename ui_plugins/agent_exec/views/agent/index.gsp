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
  dojo.byId("results_msgLive").innerHTML = "Results of " + liveResults.command +
                                       " for " + resInfo[eid].name;
  var results = liveResults.results;
  for (var i=0; i<results.length; i++) {
    var r = results[i];
    if (r.rid == eid) {
      if (r.result) {
      hideErrorPanel();
      dojo.byId('result').innerHTML =  "<div class='agentcmd_result'><table cellpadding='0' cellspacing='0' width='100%'><thead><tr><td>Result</td></tr></thead><tbody><tr><td>" + r.result + "</td></tr></tbody></table></div>";
      } else {
      handleError(r.error);
      //dojo.byId('result').innerHTML = r.error;
      }
      break;
    }
  }
  <% if (isGroup) { %>
    if (lastSelected) {
      dojo.byId('mem_' + lastSelected).style.color = 'black';
      dojo.byId('mem_' + lastSelected).style.fontWeight = 'normal';
    }
    dojo.byId('mem_' + eid).style.color = '#60A5EA';
    dojo.byId('mem_' + eid).style.fontWeight = 'bold';
    lastSelected = eid;
  <% } %>
}

function processResult(result) {
  liveResults = result;
  <% if (!isGroup) { %>
    showResult('${eid}');
  <% } else { %>
    dojo.byId("groupMembers").className = 'hasData';
  
    var res = result.results;
    for (var i=0; i<res.length; i++) {
      var r = res[i];
      
      if (r.result) {
        dojo.byId('clicker_' + r.rid).className = 'goodResults';
      } else {
        dojo.byId('clicker_' + r.rid).className = 'errorResults';
      }
      if (lastSelected) {
        showResult(lastSelected);
      }
    }
  <% } %>
}

function runCommand() {
  var cmdSelect = dojo.byId('commandSelect');  
  if (cmdSelect.selectedIndex == 0)
    return;
  var cmd = cmdSelect.options[cmdSelect.selectedIndex].value;

  var paramSelect = dojo.byId('paramSelect');
  var param;
  if (paramSelect != null) {
    if (paramSelect.selectedIndex >= 0)
      param = paramSelect.options[paramSelect.selectedIndex].value;
    else
      return;
  }
  
  var url;
  if (cmd == 'upgrade') {
    url = '<%= urlFor(action:'invoke') %>' + 
            '?cmd=' + cmd + 
            '&bundle=' + param + 
            '&eid=<%= eid %>';
  } 
  else if (cmd == 'push plugin') {
    url = '<%= urlFor(action:'invoke') %>' + 
            '?cmd=' + cmd + 
            '&plugin=' + param + 
            '&eid=<%= eid %>';
  }   
  else {
    url = '<%= urlFor(action:'invoke') %>' + 
            '?cmd=' + cmd + 
            '&eid=<%= eid %>';  
  }
  
  var fmtSelect = dojo.byId('fmt_' + cmd);
  if (fmtSelect.selectedIndex != -1) {
    var fmt = fmtSelect.options[fmtSelect.selectedIndex].value;
    url = url + '&formatter=' + fmt;
  } 

  if (++ajaxCount > 0) {
    dojo.byId("spinner").style.visibility = 'visible';  
  }
    
  dojo.io.bind({
    url: url,
    method: "get",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      if (--ajaxCount == 0) {
        dojo.byId("spinner").style.visibility = 'hidden';  
      }
      processResult(data);
    },
    error: function(err, msg) {
      //alert('There has been an error:  ' + err);
    }
  });
}

function handleError(er) {
    var msgPanelObj = dojo.byId("messagePanel");
    if(msgPanelObj.style.display != "block") {
        msgPanelObj.style.display = "block";
    }

    if (er.search(/Unknown command/) < 0)
        dojo.byId("messagePanelMessage").innerHTML = er;
    else
        dojo.byId("messagePanelMessage").innerHTML = "${l.agentUnknownCommand}";
}

function hideErrorPanel() {
      var msgPanelObj = dojo.byId("messagePanel");
            if(msgPanelObj.style.display = "block") {
            msgPanelObj.style.display = "none";
            dojo.byId("messagePanelMessage").innerHTML = '';
            }
}

var legends = {};
legends['restart'] = '${l.restart}';
legends['ping'] = '${l.ping}';
legends['upgrade'] = '${l.upgrade}';
legends['push plugin'] = '${l.push_plugin}';

function updateLegend(select){
    var legendDiv = dojo.byId("legend");
    if(select.selectedIndex <= 0){
        legendDiv.innerHTML = "";
        return;
    }
    legendDiv.innerHTML = legends[select.options[select.selectedIndex].value];
}

function updateCmdOptions(select){
    var options = dojo.byId("cmdOptions");

      var paramSelect = dojo.byId("paramSelect");
      if (paramSelect != null)
        options.removeChild(paramSelect);
      var paramLbl = dojo.byId("paramLbl");
      if (paramLbl != null)
        options.removeChild(paramLbl);
      var execute = dojo.byId("execute");
      if (execute != null)
        options.removeChild(execute);
        
   if(select.selectedIndex <= 0) {
       return;
   }
   else if (select.options[select.selectedIndex].value == 'upgrade') {
     
        <% if (bundles != []) { %>
            var paramLbl = document.createElement('div');
            paramLbl.setAttribute("id", "paramLbl");
            paramLbl.setAttribute("class", "instruction1");
            paramLbl.innerHTML="Select upgradeable agent bundle:";
            options.appendChild(paramLbl);
            
            var paramSelect = document.createElement('select');
            paramSelect.setAttribute("id", "paramSelect");
            paramSelect.setAttribute("style", "margin-bottom:5px;");
            var option;
            <% for (b in bundles) { %>
                 option = document.createElement('option');
                 option.setAttribute("value", "${b}");
                 option.innerHTML="${h b}";
                 paramSelect.appendChild(option);
            <% } %>
           options.appendChild(paramSelect);
     
            var execBtn = document.createElement('input');
            execBtn.setAttribute("type", "button");
            execBtn.setAttribute("id", "execBtn");
            execBtn.setAttribute("value", "Execute");
            execBtn.setAttribute("onClick", "runCommand()");
            
            var execute = document.createElement('div');
            execute.setAttribute("id", "execute");
            execute.appendChild(execBtn);
            options.appendChild(execute);
        <% } else { %>
            var paramLbl = document.createElement('div');
            paramLbl.setAttribute("id", "paramLbl");
            paramLbl.setAttribute("class", "instruction1");
            paramLbl.innerHTML="There are no agent bundles available for upgrade.";
            options.appendChild(paramLbl);
         <% } %>
    }
   else if (select.options[select.selectedIndex].value == 'push plugin') {
     
        <% if (plugins != []) { %>
            var paramLbl = document.createElement('div');
            paramLbl.setAttribute("id", "paramLbl");
            paramLbl.setAttribute("class", "instruction1");
            paramLbl.innerHTML="Select server plugin to push to agent:";
            options.appendChild(paramLbl);
            
            var paramSelect = document.createElement('select');
            paramSelect.setAttribute("id", "paramSelect");
            paramSelect.setAttribute("style", "margin-bottom:5px;");
            var option;
            <% for (p in plugins) { %>
                 option = document.createElement('option');
                 option.setAttribute("value", "${p}");
                 option.innerHTML="${h p}";
                 paramSelect.appendChild(option);
            <% } %>
           options.appendChild(paramSelect);
     
            var execBtn = document.createElement('input');
            execBtn.setAttribute("type", "button");
            execBtn.setAttribute("id", "execBtn");
            execBtn.setAttribute("value", "Execute");
            execBtn.setAttribute("onClick", "runCommand()");
            
            var execute = document.createElement('div');
            execute.setAttribute("id", "execute");
            execute.appendChild(execBtn);
            options.appendChild(execute);
        <% } else { %>
            var paramLbl = document.createElement('div');
            paramLbl.setAttribute("id", "paramLbl");
            paramLbl.setAttribute("class", "instruction1");
            paramLbl.innerHTML="There are no server plugins available to push.";
            options.appendChild(paramLbl);
         <% } %>
    }    
    else  {
            var execBtn = document.createElement('input');
            execBtn.setAttribute("type", "button");
            execBtn.setAttribute("id", "execBtn");
            execBtn.setAttribute("value", "Execute");
            execBtn.setAttribute("onClick", "runCommand()");
            
            var execute = document.createElement('div');
            execute.setAttribute("id", "execute");
            execute.appendChild(execBtn);
            options.appendChild(execute);
    }
}

dojo.addOnLoad(function(){
    updateLegend(dojo.byId("commandSelect"));
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
            <div class="instruction1">Please select an agent operation to run:</div>
        <select id="commandSelect" onchange="updateLegend(this);updateCmdOptions(this);" style="margin-bottom:5px;">
        <% for (c in commands) { %>
          <option value="${c}">${h c}</option>
        <% } %>
      </select>
      </div>
      <div id="legend" style="padding: 1px 5px 5px 2px; font-style: italic;"></div>
      <div id="cmdOptions" style="padding: 1px 5px 5px 2px; font-style: italic;"></div>
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
