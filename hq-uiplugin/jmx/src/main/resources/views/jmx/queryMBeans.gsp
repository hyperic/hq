<html>
<head>
<script type="text/javascript">

function invoke(id, name, op) {
    var params = {
    	  "op": op,
    	  "name": name,
    	  "id": id
    };
    var argsid = name + "." + op + ".args"
    var rsltid = name + ".invokeResult"
    var args = hqDojo.byId(argsid)
    if (args) {
        params["args"] = args.value;
    }
    hqDojo.xhrPost({
      	url:  '/<%= urlFor(action:"invoke", encodeUrl:true) %>',
      	handleAs: "json-comment-filtered",
      	content: params,
      	load:  function(response, args) {
        	hqDojo.byId(rsltid).innerHTML = response.html;
      	},
    });
}

function updateFilters(id) {
<%  for (filter in filters) { %>
    if ("${filter.'@id'}" == id) {
      hqDojo.byId('pattern').value = "${filter.objectName.text()}"
      hqDojo.byId('attributeFilter').value = "${filter.attributeRegex.text()}"
      hqDojo.byId('operationFilter').value = "${filter.operationRegex.text()}"
    }
<% } %>
}

hqDojo.ready(function(){
	<% if (message) { %>
    	hqDojo.byId('queryResult').innerHTML = "${message}";
	<% } else { %>
    	hqDojo.byId('queryResult').innerHTML = '';
	<% } %>
	setRefreshInterval(${refreshInterval});
});

function refreshAttributeData() {
    if (hqDojo.byId('mbeans').childNodes.length > 1) {
		hqDojo.byId('queryResult').innerHTML = '... updating';
        hqDojo.xhrGet({
        	url: '<%= urlFor(action:"listMBeans") %>',
          	handleAs: "json-comment-filtered",
          	content: {
              	eid: "${eid}",
              	pattern: hqDojo.byId("pattern").value,
              	attributeFilter: hqDojo.byId("attributeFilter").value,
              	operationFilter: hqDojo.byId("operationFilter").value,
              	presetFilter: hqDojo.byId("presetFilter").value
          	},
          	load: function(response, args) {
                hqDojo.byId('queryResult').innerHTML =  "${message}";
                var res = response.results;
                for (var i=0; i < res.length; i++) {
                  	var r = res[i];

                    if (r.value) {
                    	hqDojo.byId(r.id).innerHTML = r.value;
                  	}
                }
            }
        });
	}
}

var refreshIntervalId = 0;
function setRefreshInterval(val) {
  if (val == 0) {
    clearInterval(refreshIntervalId);
  }
  else {
    clearInterval(refreshIntervalId);
    refreshIntervalId = setInterval("refreshAttributeData()", 60 * 1000 * val);
  }
}
</script>

<style type="text/css">
input[type=text] {
    border: 1px solid gray;
    width: 500px;
    padding: 3px
}
</style>
</head>
<body>

<form name="MBeanQuery" method="POST">
<table width="90%">
<tr><td width="15%">Object Name Pattern:</td>
<td width="60%"><input name="pattern" id="pattern" type="text" value="${pattern}"/></td>
<td>Preset Searches:</td>
<td><select id="presetFilter" name="presetFilter" style="width:160px" onchange="updateFilters(this.options[this.selectedIndex].value);" >
<% for (filter in filters) { %>
 <option value ="${filter.'@id'}"<% if(presetFilter == filter.'@id') { %> selected<% } %>>${filter.'@id'}</option>
 <% } %>
</select>
</td></tr>
<tr><td>Attribute Regex Filter:</td>
<td><input name="attributeFilter" id="attributeFilter" type="text" value="${attributeFilter}"/></td>
<td>Refresh Interval:</td>
<td><select id="refreshInterval" name="refreshInterval" style="width:100px" onchange="setRefreshInterval(this.options[this.selectedIndex].value);" >
  <option value ="0"<% if(refreshInterval == "0") { %> selected<% } %>>Off</option>
 <option value ="1"<% if(refreshInterval == "1") { %> selected<% } %>>1 min</option>
  <option value ="5"<% if(refreshInterval == "5") { %> selected<% } %>>5 min</option>
</select></td></tr>
<tr><td>Operation Regex Filter:</td>
<td><input name="operationFilter" id="operationFilter" type="text" value="${operationFilter}"/></td>
<td></td>
</tr>
</table>
<br/>
<table width="90%">
<tr><td width="15%"><input type="submit" value="Query MBeans"/></td>
<td width="60%"><div id="queryResult"/></td><td>&nbsp;</td></table>
</form>


<div id="mbeans">
<% for (result in data) { %>
   <% def resource = result['resource'] %>
   <% if (resource) { %>
      <%= linkTo(resource.name, [resource:resource]) %>
   <% } %>
   <% for (bean in result['beans']) { %>
      <h3>${bean.name}</h3>
      <% if (bean.attrNames.length > 0) { %>
      <h4>Attributes:</h4>
      <table width="95%" border="1">
	<thead><tr><td width="30%"><b>Name</b></td><td><b>Value</b></td></tr></thead>
         <% for (attrName in bean.attrNames) { %>
            <tr><td>${attrName}</td>
		<td><div id="${bean.dojoId}.${attrName}.value" style="overflow:auto"><%= bean.attrs.get(attrName)['Value'] %></div></td>
            </tr>
         <% } %>
     </table>
      <% } %>

      <% if (bean.ops.size > 0) { %>
         
	<h4>Operations:</h4>
      <table width="95%" border="1">
	<tr><td width="100%">
         <% for (op in bean.ops) { %>
            <% if (op.signature.length < 2) { %>
                <% if (op.signature.length > 0 && bean.ops.size > 1) { %>
                   <br/>
                <% } %>
                <a class="buttonGreen" onclick="invoke(${bean.resId}, '${bean.name}', '${op.name}')" href="#"><span>${op.name}</span></a>
                <% for (def i=0; i<op.signature.length; i++) { %>
                   <input style="width:75px" type="text" id="${bean.name}.${op.name}.args"/>
                <% } %>
              <% } %>  
         <% } %>
         </td></tr>
	</table>
	 <table border="0"><td><tr><div id="${bean.name}.invokeResult" /></tr></td></table>
      <% } %>
   <% } %>
<% } %>
</div>


</body>
</html>
