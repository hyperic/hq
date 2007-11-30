<script type="text/javascript">
var fmt      = {};
var commands = [];
var cmd;
var liveResults = [];
var resInfo = {};  
var ajaxCount = 0;
var lastSelected = undefined;

<% for (m in groupMembers) { %>
  resInfo['${m.entityID}'] = {name: "<%= linkTo(h(m.name), [resource:m]) %>" };
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
  dojo.byId("results_msg").innerHTML = "Results of " + liveResults.command + 
                                       " for " + resInfo[eid].name;
  var results = liveResults.results;
  for (var i=0; i<results.length; i++) {
    var r = results[i];
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
    }
  });
}

</script>
<style>

</style>

<div class="outerLiveDataCont">

  <div class="leftbx">

    <div class="bxblueborder">

      <div class="BlockTitle"><div style="float:left;">Execute Command</div><div class="acLoader2" id="spinner" style="display:inline;float:right;"></div>
      <br class="clearBoth">
      </div>

      <div class="fivepad">

        <div class="">
            <div class="instruction1">Please select a query to run:</div>
        <select id="commandSelect" onchange="runCommand()">
        <% for (c in commands) { %>
          <option value="${c}">${h c}</option>
        <% } %>
      </select>
      </div>
      
      <% if (isGroup) { %>
        <div class="grpmembertext">Group Members</div>
        <div id="groupMembers" class="pendingData">
        <ul>
        <% for (m in groupMembers) { %>
        <li>
        <div class="groupMemberName"><span id="mem_${m.entityID}">&middot;${h m.name}</span></div>
          <div id="clicker_${m.entityID}" style="float:right;display:inline;height:16px;width:20px;" onclick="showResult('${m.entityID}')" title="Click to view query information on this resource">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div>
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
  <span id="results_msg"></span>
  <div id="result" class="bxblueborder"></div>
</div>

</div>