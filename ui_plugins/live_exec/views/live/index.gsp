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
    }
    dojo.byId('mem_' + eid).style.color = 'red';
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
.pendingData li {color:gray;}
.hasData  li {color:black;}
.goodResults  {width:20px;display:inline;background: url(/images/icon_email.gif);}
.errorResults {width:20px;display:inline;background: url(/images/icon_actual.gif);}
</style>

<div class="outerLiveDataCont">
  <div class="leftbx">
    <div class="bxblueborder">
      <div class="BlockTitle"><div style="float:left;">Execute Command</div>
      <div class="acLoader2" id="spinner" style="display:inline;float:right;"></div>
      <div style="clear:both;height:1px;"></div>
      </div>
      <div class="fivepad">
        <select id="commandSelect" 
                onchange="runCommand()">
        <% for (c in commands) { %>
          <option value="${c}">${h c}</option>
        <% } %>
      </select>
      
      <% if (isGroup) { %>
      <div style="padding:5px 3px;">Group Members</div>
      <div id="groupMembers" class="pendingData">
      <ul style="margin:0px;padding:0px;list-style-type:none;">
        <% for (m in groupMembers) { %>
        <li style="padding:2px;"><div style="display:inline;float:left;"><span id="mem_${m.entityID}">${h m.name}</span></div>
          <div id="clicker_${m.entityID}" style="float:right;display:inline;" onclick="showResult('${m.entityID}')">&nbsp;&nbsp;&nbsp;</div>
            <div style="clear:both;height:1px;"></div>
          </div>
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
  <div id="results_msg"></div>
  <div id="result" class="bxblueborder"></div>
</div>
