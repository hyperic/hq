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

<div>
  Execute Command
  <select id="commandSelect" 
          onchange="selectCommand(options[selectedIndex].value)">
  <% for (c in commands) { %>
    <option value="${c}">${h c}</option>
  <% } %>
  </select>

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

<div id="goButtonCont">
  <button onclick="runCommand()">Go!</button>
</div>

<div id="result">
</div>
