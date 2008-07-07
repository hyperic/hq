<script type="text/javascript">
function sendCode() {
  dojo.byId('timeStatus').innerHTML = '... executing';
   dojo.io.bind({
    url: '<%= urlFor(action:"execute") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    content: {
        code:   dojo.byId("code").value,
        debug:  dojo.byId("hiberDebug").checked
    },
    load: function(type, data, evt) {
      dojo.byId('result').innerHTML = data.result;
      dojo.byId('hiberStats').innerHTML = responseObject.hiberStats;
      dojo.byId('timeStatus').innerHTML = responseObject.timeStatus;
    },
    error: function(err, msg) {
      alert('error! ' + err);
    }
  });
}

function chooseTemplate(t) {
  dojo.io.bind({
    url: '<%= urlFor(action:"getTemplate") %>',
    method: "get",
    mimetype: "text/json-comment-filtered",
    content: {template: t},
    load: function(type, data, evt) {
      dojo.byId('code').value = data.result;
    },
    error: function(err, msg) {
      alert('error! ' + err);
    }
  });
}

</script>

Templates: 
<% for(t in templates) { %>
  <a onclick="chooseTemplate('${t}')">${t}</a> |
<% } %>
<br/>

<textarea id="code" cols="120", rows="30">
</textarea>

<br/>
<button onclick="sendCode()">Execute</button>
<input type="checkbox" id="hiberDebug">Hibernate Debugging</input>
<br/>

<div id='timeStatus'>
  Status:  Idle
</div>

<br/>
<div id='hiberStats'>
</div>

<h2>Result</h2>
<pre>
  <div id='result'>
  </div>
<pre>
