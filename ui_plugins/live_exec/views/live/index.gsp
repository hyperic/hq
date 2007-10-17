<script type="text/javascript">
function selectCommand(cmd) {
  if (cmd == '---') {
    dojo.html.hide('goButtonCont');
  } else {  
    dojo.html.show('goButtonCont');
  }
}

function go() {
  var cmdSelect = dojo.byId('commandSelect');
  var cmd = cmdSelect.options[cmdSelect.selectedIndex].value;
  var url = '<%= urlFor(action:'invoke') %>' + '?cmd=' + cmd;
  url = url + '&eid=<%= eid %>'
  dojo.io.bind({
    url: url,
    method: "get",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('result').innerHTML = data.result;
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
</div>

<div id="goButtonCont">
  <button onclick="go()">Go!</button>
</div>

<div id="result">
</div>