<script type="text/javascript">
var fmt      = {};
var commands = [];
var cmd;
var liveResults = [];
var ajaxCount = 0;
var lastSelected = undefined;

<% for (c in commands) { %>
    cmd = '<%= c %>';
    commands.push(cmd);
    fmt[cmd] = [];
    
    <% for (f in cmdFmt[c]) { %>
      fmt[cmd].push('<%= f %>');
    <% } %>
<% } %>

function selectCommand(cmd) {
  if (cmd == '---') {
    dojo.html.hide('goButtonCont');
  } else {  
    dojo.html.show('goButtonCont');
  }
  
  for (var i=0; i<commands.length; i++) {
    if (commands[i] == cmd && cmd != '---' &&
        fmt[cmd].length > 1) 
    {
      dojo.html.show("fmt_cont_" + commands[i]);
    } else {
      dojo.html.hide("fmt_cont_" + commands[i]);
    }
  }
}

function showResult(eid) {
  for (var i=0; i<liveResults.length; i++) {
    var r = liveResults[i];
    if (r.rid == eid) {
      if (r.result) {
        dojo.byId('result').innerHTML = r.result;
      } else {
        dojo.byId('result').innerHTML = r.error;
      }
      break;
    }
  }
  <% if (isGroup) { %>
    if (lastSelected) {
      dojo.byId('mem_' + lastSelected).style.color = 'black';
    }
    dojo.byId('mem_' + eid).style.color = 'red';
    lastSelected = eid;
  <% } %>
}

function processResult(result) {
  liveResults = result.results;
  <% if (!isGroup) { %>
    showResult('${eid}');
  <% } else { %>
    dojo.byId("groupMembers").className = 'hasData';  
  
    for (var i=0; i<liveResults.length; i++) {
      var r = liveResults[i];
      
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
  var cmd = cmdSelect.options[cmdSelect.selectedIndex].value;
  var url = '<%= urlFor(action:'invoke') %>' + 
            '?cmd=' + cmd + 
            '&eid=<%= eid %>';
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
      alert('error! ' + err);
    },
  });
}

</script>
<style>
.outerLiveDataCont {margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;}
.bxblueborder {border:1px solid #7BAFFF;overflow-y:auto;}
.leftbx {float:left;width:18%;margin-right:10px;height:400px;}
.BlockTitle {font-family: arial, sans-serif;font-size: 12px;color: #ffffff;font-weight: bold;background-color: #60A5EA;}
#result {padding:0px;margin:0px;float:right;width:78%;display:inline;height: 400px;overflow-x: hidden; overflow-y: auto;}
#result thead {width:100%;padding:0px;margin:0px;}
#result table thead td {font-family: arial, sans-serif;font-size: 12px;color: #ffffff;font-weight: bold;background-color: #60A5EA;padding:3px;margin:0px;}
#result table{width:100%;padding:0px;}
#result table td {padding:3px;border-bottom:1px solid #cccccc;}
.fivepad {padding:5px;}
.pendingData {color:gray;}
.hasData     {;}
.goodResults  {width:20px;display:inline;background: url(/images/icon_email.gif);}
.errorResults {width:20px;display:inline;background: url(/images/icon_actual.gif);}
</style>

<div class="outerLiveDataCont">
  <div class="leftbx">
    <div class="bxblueborder">
      <div class="BlockTitle">Execute Command</div>
      <div class="fivepad">
        <select id="commandSelect" 
                onchange="selectCommand(options[selectedIndex].value)">
        <% for (c in commands) { %>
          <option value="${c}">${h c}</option>
        <% } %>
      </select>
      <div class="acLoader" id="spinner"></div>
    </div>

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
    <div id="goButtonCont" class="fivepad"><button onclick="runCommand()">Select</button></div>
  </div>

  <% if (isGroup) { %>
  Group Members<br/>
  <div id="groupMembers" class="pendingData">
    <ul>
    <% for (m in groupMembers) { %>
      <li><span id="mem_${m.entityID}">${h m.name}</span>
          <div id="clicker_${m.entityID}" 
               onclick="showResult('${m.entityID}')">&nbsp;&nbsp;&nbsp;</div>
      </li>
    <% } %>
    </ul>
  </div>
  <% } %>
</div>

<div id="result" class="bxblueborder"></div>
</div>
