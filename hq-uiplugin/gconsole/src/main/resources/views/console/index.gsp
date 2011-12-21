<div class="gConsoleContainer">
    <label>Available Templates</label>
    <fieldset>
    <% if(templates == null || templates.size == 0 ) { %>
        There are no templates available.
    <% } %>
    <% for(t in templates) { %>
      <a class="chooseTemplateLink">${t}</a> |
    <% } %>
    </fieldset>
    <br/>
    <label for="code" style="display:block">Code</label>
    <textarea id="code" rows="30"></textarea>
    <br/><br/>
    
    <div>
        <a id="executeLink" class="buttonGreen" href="#"><span>Execute</span></a>
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
<script type="text/javascript">
document.navTabCat = "Admin";
var gconsoleExecuteUrl = "<%= urlFor(action:"execute", encodeUrl:true) %>";

dojo11.addOnLoad(function() {
	dojo11.connect(dojo11.byId("executeLink"), "onclick", function(e) {
		dojo11.byId('timeStatus').innerHTML = '... executing';
		
		dojo11.xhrPost({
    		url: gconsoleExecuteUrl,
	    	handleAs: "json-comment-filtered",
    		content: {
        		code:   dojo11.byId("code").value
	    	},
    		load: function(response, args) {
      			dojo11.byId('result').innerHTML = response.result;
      			dojo11.byId('timeStatus').innerHTML = response.timeStatus;
      			
      			if (response.actionToken) {
      				// use new CSRF token for subsequent POST requests
      				gconsoleExecuteUrl = response.actionToken;
      			}
	    	},
    		error: function(response, args) {
      			alert('error! ' + response);
	    	}
	    });
  	});

  	dojo11.query(".chooseTemplateLink").onclick(function(e) {
  		dojo11.xhrGet({
    		url: '<%= urlFor(action:"getTemplate") %>',
	    	handleAs: "json-comment-filtered",
    		content: {
        		template: e.target.innerHTML
	        },
    		load: function(response, args) {
      			dojo11.byId('code').value = response.result;
	    	},
    		error: function(response, args) {
      			alert('error! ' + response);
	    	}
  		});
  	});
});
</script>