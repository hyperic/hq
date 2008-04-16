<script type="text/javascript">
function sendCode() {
  dojo.xhrPost({
    url: '<%= urlFor(action:"execute") %>',
    handleAs: "json-comment-filtered",
    content: {code: dojo.byId("code").value},
    load: function(responseObject, ioArgs) {
      dojo.byId('result').innerHTML = responseObject.result;
    },
    error: function(response, ioArgs) {
      alert('error! ' + response);
    }
  });
}

function chooseTemplate(t) {
  dojo.xhrGet({
    url: '<%= urlFor(action:"getTemplate") %>',
    handleAs: "json-comment-filtered",
    content: {template: t},
    load: function(responseObject, ioArgs) {
      dojo.byId('code').value = responseObject.result;
    },
    error: function(response, ioArgs) {
      alert('error! ' + response);
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
