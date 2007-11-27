<script type="text/javascript">
function sendCode() {
  dojo.io.bind({
    url: '<%= urlFor(action:"execute") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    content: {code: dojo.byId("code").value},
    load: function(type, data, evt) {
      dojo.byId('result').innerHTML = data.result;
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

<textarea id="code" cols="120", rows="40">
</textarea>

<br/>
<button onclick="sendCode()">Execute</button>

<br/>
Results:<br/>
<pre>
<div id='result'>
</div>
<pre>
