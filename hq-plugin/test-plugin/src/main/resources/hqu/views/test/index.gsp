<script type="text/javascript">
document.navTabCat = "Admin";
function sendCode() {
  	hqDojo.byId('timeStatus').innerHTML = '... executing';
   	hqDojo.xhrPost({
    	url: '<%= urlFor(action:"execute", encodeUrl:true) %>',
    	handleAs: "json-comment-filtered",
    	content: {
        	code:   hqDojo.byId("code").value,
 	       debug:  hqDojo.byId("hiberDebug").checked
    	},
    	load: function(response, args) {
     		hqDojo.byId('result').innerHTML = response.result;
		    hqDojo.byId('hiberStats').innerHTML = response.hiberStats;
      		hqDojo.byId('timeStatus').innerHTML = response.timeStatus;
    	},
    	error: function(response, args) {
      		alert('error! ' + response);
    	}
  	});
}

function chooseTemplate(t) {
  	hqDojo.xhrGet({
    	url: '<%= urlFor(action:"getTemplate") %>',
	    handleAs: "json-comment-filtered",
    	content: {
        	template: t
        },
    	load: function(response, args) {
      		hqDojo.byId('code').value = response.result;
    	},
    	error: function(response, args) {
      		alert('error! ' + response);
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
