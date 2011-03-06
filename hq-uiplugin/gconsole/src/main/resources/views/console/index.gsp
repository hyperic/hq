<script type="text/javascript">
document.navTabCat = "Admin";
function sendCode() {
  	dojo.byId('timeStatus').innerHTML = '... executing';
	dojo.xhrPost({
    	url: '<%= urlFor(action:"execute") %>',
    	handleAs: "json-comment-filtered",
    	content: {
        	code:   dojo.byId("code").value,
    	},
    	load: function(response, args) {
      		dojo.byId('result').innerHTML = response.result;
      		dojo.byId('timeStatus').innerHTML = response.timeStatus;
    	},
    	error: function(response, args) {
      		alert('error! ' + response);
    	}
  	});
}

function chooseTemplate(t) {
  	dojo.xhrGet({
    	url: '<%= urlFor(action:"getTemplate") %>',
    	handleAs: "json-comment-filtered",
    	content: {
        	template: t
        },
    	load: function(response, args) {
      		dojo.byId('code').value = response.result;
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
    
    <div>
        <a class="buttonGreen" onclick="sendCode()" href="javascript:void(0)"><span>Execute</span></a>
    </div>
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
