<script type="text/javascript">
document.navTabCat = "Admin";
function sendCode() {
  	dojo11.byId('timeStatus').innerHTML = '... executing';
   	dojo11.xhrPost({
    	url: '<%= urlFor(action:"execute", encodeUrl:true) %>',
    	handleAs: "json-comment-filtered",
    	content: {
           code:   dojo11.byId("code").value,
 	       debug:  dojo11.byId("hiberDebug").checked
    	},
    	load: function(response, args) {
     		dojo11.byId('result').innerHTML = response.result;
		    dojo11.byId('hiberStats').innerHTML = response.hiberStats;
      		dojo11.byId('timeStatus').innerHTML = response.timeStatus;
    	},
    	error: function(response, args) {
      		alert('error! ' + response);
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
    error: function(type, data, evt) {
      alert('error! ' + data);
    }
  });
}

</script>
<div class="gConsoleContainer">
    <label>Available Templates</label>
    <fieldset>
    <% if(templates == null || templates.size == 0 ) { %>
        There are no templates available.
    <% } %>
    <% for(t in templates) { %>
      <a onclick="chooseTemplate('${t}')">${t}</a> |
    <% } %>
    </fieldset>
    <br/>
    <label for="code" style="display:block">Code</label>
    <textarea id="code" rows="30"></textarea>
    <br/><br/>
    
    
    <br/>
    
    <div id='timeStatus'>
      Status:  Idle
    </div>
    <br/>
    
    
    <label>Result</label>
    <fieldset>
        <pre>
          <div id='result'></div>
        <pre>
    </fieldset>
</div>
