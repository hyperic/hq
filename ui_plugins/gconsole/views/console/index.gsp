<script type="text/javascript">
document.navTabCat = "Admin";
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
      dojo.byId('hiberStats').innerHTML = data.hiberStats;
      dojo.byId('timeStatus').innerHTML = data.timeStatus;
    },
    error: function(type, data, evt) {
      alert('error! ' + data);
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
    
    <div>
        <a class="buttonGreen" onclick="sendCode()" href="javascript:void(0)"><span>Execute</span></a>
        <input type="checkbox" id="hiberDebug">Hibernate Debugging</input>
    </div>
    <br/>
    
    <div id='timeStatus'>
      Status:  Idle
    </div>
    <br/>
    
    <label>Hibernate Details</label>
    <fieldset>
        <div id='hiberStats'><br/></div>
    </fieldset>
    <br/>
    
    <label>Result</label>
    <fieldset>
        <pre>
          <div id='result'></div>
        <pre>
    </fieldset>
</div>
