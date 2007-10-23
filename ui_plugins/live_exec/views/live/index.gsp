<script type="text/javascript">
var fmt      = {};
var commands = [];
var cmd;

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
  
  dojo.io.bind({
    url: url,
    method: "get",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      if (data.result) {
        dojo.byId('result').innerHTML = data.result;
      } else {
        dojo.byId('result').innerHTML = data.error;
      }
    },
    error: function(err) {
      alert('error!');
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
</div>
<div id="result" class="bxblueborder"></div>
</div>

