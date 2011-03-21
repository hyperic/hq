<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<style>

	.mechanismOff{
		border-bottom: 1px solid gray;
	}

	#selectedFileList{
		border: 1px solid #CCCCCC;
    	height: 100px;
    	overflow-x: hidden;
    	overflow-y: auto;
    	width: 350px;
    	text-align: left;
	}
	.fileToUploadClose{
		width:10px;
	}
	
	#selectedFileList table{
		padding: 0px;
		margin: 0px;
	}
        
	.uploadBtn{
		-moz-border-radius: 4px 4px 4px 4px;
	    background: url("/images/4.0/backgrounds/button-green-background.jpg") repeat-x scroll center bottom #2DBF3D;
    	border: 1px solid #84B96D;
	    color: #FFFFFF;
	    cursor: pointer;
	    font-size: 0.9em;
	    font-weight: bold;
	    padding: 3px 15px;
	    width: 60px;
	}
	#uploadButtonBar{
		text-align: right;
	}
	#pluginManagerPanel {
		padding: 0.5em 1.5em;
	}
	
	#pluginManagerPanel span a {
		font-weight: bold;
		text-decoration: none;
	}
	
	#pluginManagerPanel h1 {
		color: #444;
		font-size: 1.5em;
		font-weight: bold;
		margin-bottom: 0.5em;
	}
	
	#pluginManagerPanel .actionbar {
		padding: 0.25em 1em;
		background: url("/images/4.0/backgrounds/table_header_large.png") repeat-x 0 0 transparent;
		overflow: hidden;
		border: 1px solid gray;
		border-top: 0;
		text-align: right;
	}
	
	#pluginManagerPanel .actionbar input[type="button"],
	#uploadPanel input[type="submit"],
	#confirmationPanel input[type="button"] {
		margin-right: 0.5em;
	}
	
	#pluginManagerPanel ul {
		margin: 0;
		padding: 0;
		list-style: none;
		background-color: #fff;
		border: 1px solid gray;
		border-top: 0;
		border-bottom: 0;
		overflow-y: scroll;
		height: 400px;
	}
	
	#pluginManagerPanel .gridheader {
		background: url("/images/4.0/backgrounds/table_header_large.png") repeat-x 0 0 transparent;
		overflow: hidden;
		font-weight: bold;
		font-size: 1.1em;
		color: #444;
		border: 1px solid gray;
		border-bottom: 0;
		height: 29px;
	}
	
	#pluginManagerPanel .gridheader span.column {
		padding-top: 0.25em;
		padding-left: 0.5em;
		height: 25px;	
	}
	
	#pluginManagerPanel .gridrow {
		overflow: hidden;
	}
	
	#pluginManagerPanel .gridrow span.column {
		padding: 0.5em 0;
		padding-left: 0.5em;
	}
	
	#pluginManagerPanel .gridrow span.first {
		text-align: center;
	}
	
	#pluginManagerPanel li.even {
		background-color: #eee;
	}
	
	#uploadPanel {
		width: 400px;
	}

	#removeErrorPanel, #confirmationPanel {
		width: 400px;
	}
	#removeErrorPanel div{
		text-align: right;
	}
		
	#uploadForm fieldset {
		border: 0;
	}
	
	#uploadForm div {
		text-align: right;
	}
	
	#confirmationPanel div {
		text-align: right;
	}
	
	#validationMessage {
	    font-weight: bold;
	}
	
	#progressMessage {
	    font-weight: bold;
	    margin-right: 1em;
	}
	
	#progressMessage.information {
	    color: #00bb00;
	}
	
	#validationMessage.error,
	#progressMessage.error{
	    color: #bb0000;
	}
	
	#showRemoveConfirmationButton{
		float:left;
	}
	#pluginList .disabled{
		color:red;
	}
	.agentStatusSpan{
		color: #0066CC;
		font-weight: bold;
		height:50px;
	}
	.agentStatusSpan:focus,
	.agentStatusSpan:hover{
		color: #0066FF;
	}
	.errorAgentTip{
		overflow:auto;
	}
	.errorAgentTitle{
		font-weight: bold;
		height:8px;
	}
	.errorAgentList li{
		list-style-image: url("/images/icon_available_red.gif");
	}
	.inProgressAgentList li{
		list-style-image: url("/images/arrow_refresh.png");
	}
	#agentInfo{
		font-weight: bold;
		float: right;
		height: 20px;
		color: #777777;
	}
	.closeButton{
		margin-right:5px;
		float:right;
		visibility:hidden;
	}
	#showStatusPanelDialog{
		width: 405px;
		height: 400px;
	}
	
	#statusButtonBar {
		text-align: right;
		vertical-align:bottom;
	}
	#agentList{
		height: 290px;
		overflow: auto;
		margin: 10px;
		padding: 0px 30px;
		width:320px;
	}
	
	.errorAgent{
		list-style-image: url("/images/icon_available_red.gif");
	}
	.inProgressAgent{
		list-style-image: url("/images/arrow_refresh.png");
	}
	#searchText{
		background: url("/images/4.0/icons/search.png") no-repeat scroll 3px center #FFFFFF;
		color: #444444;
		padding: 0.25em 0.25em 0.25em 20px;
		width:250px;
	}
	input[type="text"]{
		margin:0.5em 0;
	}
	
</style>
<section id="pluginManagerPanel" class="container top">
	<h1><fmt:message key="admin.managers.plugin.title" /></h1>
	<p><fmt:message key="${instruction}" /></p>
	
	<div id="agentInfo">
		<fmt:message key="admin.managers.Plugin.information.agent.count"/>&nbsp;<span id="agentInfoAllCount"></span> <br/>
	</div>
	
	<div class="gridheader clear">
		<span class="first column span-1">&nbsp;</span>
		<span class="column span-3"><fmt:message key="admin.managers.plugin.column.header.product.plugin" /></span>
		<span class="column span-3"><fmt:message key="admin.managers.plugin.column.header.version" /></span>
		<span class="column span-4"><fmt:message key="admin.managers.plugin.column.header.jar.name" /></span>
		<span class="column span-4"><fmt:message key="admin.managers.plugin.column.header.initial.deploy.date" /></span>
		<span class="column span-4"><fmt:message key="admin.managers.plugin.column.header.last.sync.date" /></span>
		<span class="last column span-3"><fmt:message key="admin.managers.plugin.column.header.status" /></span>
	</div>
	
	<form:form id="deleteForm" name="deleteForm" onsubmit="return false;" method="delete">
	
	<ul id="pluginList">
		<c:forEach var="pluginSummary" items="${pluginSummaries}" varStatus="index">
			<li class="gridrow clear<c:if test="${index.count % 2 == 0}"> even</c:if>">
				<span class="first column span-1">
					<c:if test="${mechanismOn}">
                    	<input type="checkbox" name="deleteId" value="${pluginSummary.id}" class="checkbox" />&nbsp; 
					</c:if>
				</span>
				<span class="column span-3">${pluginSummary.name}&nbsp;
					<c:if test="${disabled}">
						<span class="disabled"><fmt:message key="admin.managers.Plugin.column.plugin.disabled"/></span>
					</c:if>
				</span>
				<span class="column span-3">${pluginSummary.version}&nbsp;</span>
				<span class="column span-4">${pluginSummary.jarName}&nbsp;</span>
				<span class="column span-4">${pluginSummary.initialDeployDate}&nbsp;</span>
				<span class="column span-4">${pluginSummary.updatedDate}&nbsp;</span>		
				<span class="last column span-3" >
					<c:if test="${pluginSummary.allAgentCount>0}">
					    <c:if test="${pluginSummary.successAgentCount>0}">
					    	${pluginSummary.successAgentCount}&nbsp;<img src="/images/icon_available_green.gif"/>&nbsp;&nbsp;
					    </c:if>
					    
					    <c:if test="${pluginSummary.inProgressAgentCount>0 ||pluginSummary.errorAgentCount>0 }">
					    	<span id="agentStatus_${pluginSummary.id}" class="agentStatusSpan">				    	
							<c:if test="${pluginSummary.inProgressAgentCount>0}">
						        ${pluginSummary.inProgressAgentCount}&nbsp;<img id="progressIcon_${pluginSummary.id}" src="/images/arrow_refresh.png"/>&nbsp;&nbsp;
						   	</c:if>	
						   	<c:if test="${pluginSummary.errorAgentCount>0}">	   		
					   			${pluginSummary.errorAgentCount}&nbsp;<img id="errorIcon_${pluginSummary.id}" src="/images/icon_available_red.gif"/>
							</c:if>
							</span>
						</c:if>
					</c:if>
				</span>
			</li>
		</c:forEach>
	</ul>
	</form:form>
	
	<c:if test="${mechanismOn}" >
		<div class="actionbar">		
			<input id="showRemoveConfirmationButton" type="button" value="<fmt:message key="admin.managers.plugin.button.remove.plugin" />" />
			<span id="progressMessage"></span>
			<input id="showUploadFormButton" type="button" value="<fmt:message key="admin.managers.plugin.button.add.plugin" />" />
		</div>	
	</c:if>
	<span> <fmt:message key="admin.managers.Plugin.column.plugin.disabled.tip" /></span>
</section>


<c:if test="${mechanismOn}" >
<div id="uploadPanel" style="visibility:hidden;">
		<strong><fmt:message key="admin.managers.plugin.upload.dialog.instruction" /></strong>
		<p>
			<span><fmt:message key="admin.managers.plugin.upload.dialog.label" />&nbsp;</span>
		</p>
		<p>
			<div id="selectFileButton" class="uploadBtn">Select File</div>
			<div id="selectedFileList"></div>
			
		</p>
		<p id="validationMessage" class="error" style="opacity:0;">&nbsp;</p>
		<div id="uploadButtonBar">
			<input id="uploadButton" type="button" value="<fmt:message key='admin.managers.plugin.button.upload' />" /> &nbsp;
			<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
		</div>
</div>
<div id="confirmationPanel" style="visibility:hidden;">
	<p><fmt:message key="admin.managers.plugin.confirmation.message" /></p>
	<div>
		<input id="removeButton" type="button" name="remove" value="<fmt:message key="admin.managers.plugin.button.remove" />" />
		<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
	</div>
</div>

<div id="removeErrorPanel" style="visibility:hidden;">
	<p id="removeErrorMsg"></p>
	<div>
		<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
	</div>
</div>
</c:if>

<div id="showStatusPanel" style="visibility:hidden;">
	<input type="text" id="searchText"/>
	<img id="loadingIcon" src="/static/images/ajax-loader.gif" />
	<ul id="agentList"></ul>
	
	<div id="statusButtonBar">
		<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.close" /></a>
	</div>
	<ul id="unfilteredList" style="visibility:hidden;"></ul>
</div>

<script  djConfig="parseOnLoad: true">
	hqDojo.require("dojo.fx");
	hqDojo.require("dojo.io.iframe");
	hqDojo.require("dijit.dijit");
	hqDojo.require("dijit.Dialog");
	hqDojo.require("dijit.form.Button");
	hqDojo.require("dijit.Tooltip");
	hqDojo.require("dojox.form.FileUploader");
	hqDojo.require("dijit.ProgressBar");
	hqDojo.require("dojo.behavior");
	
	function refreshPage(){
			var infoXhrArgs={
				url:"<spring:url value='/app/admin/managers/plugin/info'/>",
				handleAs:"json",
				headers: { 
                	"Content-Type": "application/json",
                	"Accept": "application/json"
                },				
				load: function(response){
					hqDojo.byId("agentInfoAllCount").innerHTML=response.allAgentCount;
				}
			}
			hqDojo.xhrGet(infoXhrArgs);
			hqDojo.publish("refreshDataGrid");
	}
	
	hqDojo.ready(function() {

		refreshPage();
		function seeStatusDetail(pluginId){
			hqDijit.byId("showStatusPanelDialog").show();
			var agentListUl = hqDojo.byId("agentList");
			var unfilteredUl= hqDojo.byId("unfilteredList");
			var xhrArgs = {
					url: "/app/admin/managers/plugin/status/"+pluginId,
					load: function(response) {
						hqDojo.style(hqDojo.byId("loadingIcon"),"visibility","hidden");
						hqDojo.forEach(response, function(agentStatus) {
							var statusLi = hqDojo.create("li",{
								"innerHTML":agentStatus.agentName
							});
							statusLi.innerHTML+=" <fmt:message key="admin.managers.Plugin.tip.status.sync.date" /> "+agentStatus.syncDate;
							if(response.status=="error"){
								hqDojo.addClass(statusLi,"errorAgent");
							}else{
								hqDojo.addClass(statusLi,"inProgressAgent");
							};
							agentListUl.appendChild(statusLi);
							unfilteredLi = hqDojo.clone(statusLi);
							unfilteredUl.appendChild(unfilteredLi);
						});
					},
					handleAs: "json",
					headers: { 
	 	               	"Content-Type": "application/json",
    	            	"Accept": "application/json"
        	        }
				
			};
			hqDojo.xhrGet(xhrArgs);			
		}
		function buildAgentStatusList(response){
		var agentListUl = hqDojo.byId("agentList");
			hqDojo.forEach(response, function(agentStatus) {
							var statusLi = hqDojo.create("li",{
								"innerHTML":agentStatus.agentName
							},agentListUl);
							
							statusLi.innerHTML+="&nbsp;<fmt:message key="admin.managers.Plugin.tip.status.sync.date" />&nbsp;"+agentStatus.syncDate;
							if(response.status=="error"){
								hqDojo.addClass(statusLi,"errorAgent");
							}else{
								hqDojo.addClass(statusLi,"inProgressAgent");
							};
						});
		}
		hqDojo.behavior.add({
			".agentStatusSpan":{
				onclick: function(evt){
					var anchor = evt.target.id.indexOf("_");
					var pluginId = evt.target.id.substr(anchor+1,evt.target.id.length);
					seeStatusDetail(pluginId);
				},
				found: function(node){hqDojo.style(node,"cursor","pointer");}
			}
		});
		hqDojo.behavior.apply();
		
		var showStatusDialog = new hqDijit.Dialog({
			id: "showStatusPanelDialog",
			title: "<fmt:message key="admin.managers.Plugin.tip.status.title" />"
		});
		
		var showStatusPanel = hqDojo.byId("showStatusPanel");
		hqDojo.style(showStatusDialog.closeButtonNode,"visibility", "hidden" );
		showStatusDialog.setContent(showStatusPanel);
		hqDojo.style(showStatusPanel, "visibility", "visible");
			
		hqDojo.query("#showStatusPanelDialog .cancelLink").onclick(function(e) {
			hqDijit.byId("showStatusPanelDialog").hide();
			hqDojo.empty("unfilteredList");
			hqDojo.empty("agentList");
		});
		
		hqDojo.connect(hqDojo.byId("searchText"),"onkeyup",function(e){
			// filter here
			var statusList = hqDojo.clone(hqDojo.query("#unfilteredList li"));
			var inputText = hqDojo.byId("searchText").value;
			
			var result = hqDojo.filter(statusList,
				function(agent){
					return agent.innerHTML.toLowerCase().indexOf(inputText) !=-1;
				}
			);
			
			hqDojo.empty("agentList");
			//buildAgentStatusList(result);
			
			hqDojo.forEach(result, function(node) {
				var agentListUl = hqDojo.byId("agentList");
				agentListUl.appendChild(node);
			});			
			
			
		});
		
		new hqDijit.Tooltip({
			connectId:["agentInfo"],
			label: "<fmt:message key='admin.managers.Plugin.information.agent.count.tip' />"
		});
		
					

			
		
		if(${!mechanismOn}){
			hqDojo.attr("deleteForm","class","mechanismOff");
		}
	
		if (${mechanismOn}){
		
			var uploader = new hqDojox.form.FileUploader({
				selectMultipleFiles:true,
				fileListId:"selectedFileList",
				isDebug:false,
				uploadUrl:"<spring:url value='/app/admin/managers/plugin/upload'/>",
				force:"html",
				fileMask:[
					["jar File", "*.jar"],
					["xml File", "*.xml"]
				]
			}, "selectFileButton");
	
			hqDojo.connect(hqDojo.byId("uploadButton"), "onclick", function(e){
				var fileTypeCorrect=true;
				var pluginList = hqDojo.query("input[type='file']", hqDojo.byId("hqDijit_FileUploaderForm_0"));
				var newPluginList = pluginList.slice(0,pluginList.length-1);
					
				newPluginList.forEach(function(input) {
			  		//check file type
					var filePath = input.value;
				   	if(!checkFileType(filePath)){
			   			fileTypeCorrect=false;
			   		}
				   	//change name, for backend!
    	   			hqDojo.attr(input, "name", "plugins");
				});
				if(fileTypeCorrect){
					uploader.upload();
					hqDijit.byId("uploadPanelDialog").hide();
				}
			});
		
			hqDojo.connect(uploader, "onComplete", function(dataArray){
				if (dataArray[0].success){
					hqDojo.attr("progressMessage", "class", "information");
				} else {
					hqDojo.attr("progressMessage", "class", "error");
				}
					
				hqDojo.byId("progressMessage").innerHTML=dataArray[0].message;
				var anim = [hqDojo.fadeIn({
								node: "progressMessage",
								duration: 500
							}),
							hqDojo.fadeOut({
								node: "progressMessage",
								delay: 5000,
								duration: 500
							})];
				hqDojo.fx.chain(anim).play();						
			});
				
			function checkFileType(filePath){
				var ext = filePath.substr(filePath.length - 4);
			
				if (ext != ".jar" && ext != ".xml") {
					hqDojo.byId("validationMessage").innerHTML = "<fmt:message key='admin.managers.plugin.message.invalid.file.extension' />";
					var anim = [hqDojo.fadeIn({
									node: "validationMessage",
									duration: 500
								}),
								hqDojo.fadeOut({
									node: "validationMessage",
									delay: 5000,
									duration: 500
								})];
					hqDojo.fx.chain(anim).play();
					return false;
				}
				return true;
			}
		
			var uploadDialog = new hqDijit.Dialog({
				id: "uploadPanelDialog",
				title: "<fmt:message key="admin.managers.plugin.upload.dialog.title" />"
			});
		
			var removeDialog = new hqDijit.Dialog({
				id: "removePanelDialog",
				title: "<fmt:message key="admin.managers.plugin.remove.dialog.title" />"
			});
			var removeErrorPanelDialog = new hqDijit.Dialog({
				id: "removeErrorPanelDialog",
				title: "<fmt:message key="admin.managers.Plugin.remove.error.dialog.title" />"
			});

			
			var uploadPanel = hqDojo.byId("uploadPanel");
			var confirmationPanel = hqDojo.byId("confirmationPanel");
			var removeErrorPanel = hqDojo.byId("removeErrorPanel");
		
			hqDojo.style(uploadDialog.closeButtonNode, "visibility", "hidden");
			hqDojo.style(removeDialog.closeButtonNode, "visibility", "hidden");
			hqDojo.style(removeErrorPanelDialog.closeButtonNode,"visibility", "hidden" );
			uploadDialog.setContent(uploadPanel);
			removeDialog.setContent(confirmationPanel);
			removeErrorPanelDialog.setContent(removeErrorPanel);
		
			hqDojo.style(uploadPanel, "visibility", "visible");
			hqDojo.style(confirmationPanel, "visibility", "visible");
			hqDojo.style(removeErrorPanel, "visibility", "visible");
		
			hqDojo.connect(hqDojo.byId("showUploadFormButton"), "onclick", function(e) {
				hqDijit.byId("uploadPanelDialog").show();
			});
			hqDojo.connect(hqDojo.byId("showRemoveConfirmationButton"), "onclick", function(e) {
				var checkedPlugins = hqDojo.filter(hqDojo.query(".checkbox"), function(e){ return e.checked; });
				if(checkedPlugins.length>0){
					hqDijit.byId("removePanelDialog").show();
				}else{
					hqDojo.byId("removeErrorMsg").innerHTML = '<fmt:message key="admin.managers.Plugin.remove.error.dialog.empty" />';
					hqDijit.byId("removeErrorPanelDialog").show();
				}
			});
			hqDojo.query("#uploadPanelDialog .cancelLink").onclick(function(e) {
				hqDijit.byId("uploadPanelDialog").hide();
			});

			hqDojo.query("#removePanelDialog .cancelLink").onclick(function(e) {
				hqDijit.byId("removePanelDialog").hide();
			});
			hqDojo.query("#removeErrorPanelDialog .cancelLink").onclick(function(e) {
				hqDijit.byId("removeErrorPanelDialog").hide();
			});
		
			hqDojo.connect(hqDojo.byId("removeButton"), "onclick", function(e) {
				var xhrArgs = {
					form: hqDojo.byId("deleteForm"),
					url: "<spring:url value='/app/admin/managers/plugin/delete' />",
					load: function(response) {
						if (response=="success") {
							hqDojo.publish("refreshDataGrid");
							hqDojo.attr("progressMessage", "class", "information");
							hqDojo.byId("progressMessage").innerHTML = "remove success";
							var anim = [hqDojo.fadeIn({
										node: "progressMessage",
										duration: 500
									}),
									hqDojo.fadeOut({
										node: "progressMessage",
										delay: 1000,
										duration: 500
									})];	
						}else{
							hqDojo.attr("progressMessage", "class", "error");
							hqDojo.byId("progressMessage").innerHTML = '<fmt:message key="admin.managers.Plugin.remove.error.dialog.failure" />';
							var anim = [hqDojo.fadeIn({
										node: "progressMessage",
										duration: 500
									}),
									hqDojo.fadeOut({
										node: "progressMessage",
										delay: 10000,
										duration: 500
									})];
						}
						hqDojo.fx.chain(anim).play();	
					},
					error: function(response,arg){
						hqDojo.attr("progressMessage", "class", "error");
						hqDojo.byId("progressMessage").innerHTML = '<fmt:message key="admin.managers.Plugin.remove.error.dialog.failure" />';
						var anim = [hqDojo.fadeIn({
									node: "progressMessage",
									duration: 500
									}),
									hqDojo.fadeOut({
										node: "progressMessage",
										delay: 10000,
										duration: 500
									})];
					
						hqDojo.fx.chain(anim).play();
					}
				};
				hqDojo.xhrPost(xhrArgs);
				refreshPage();
				hqDijit.byId("removePanelDialog").hide(); 
			});
		}
		
		
		hqDojo.subscribe("refreshDataGrid", function() {
			hqDojo.xhrGet({
				url: "<spring:url value="/app/admin/managers/plugin/list" />",
				handleAs: "json",
				headers: { 
                	"Content-Type": "application/json",
                	"Accept": "application/json"
                },
                load: function(response, args) {
                	hqDojo.empty("pluginList");
                	
                	var index = 1;
                	
                	hqDojo.forEach(response, function(summary) {
                		var li = hqDojo.create("li", {
                			"class": "gridrow clear" + (((index) % 2 == 0) ? " even" : "")
                		}, "pluginList");
                		var span = hqDojo.create("span", {
                			"class": "first column span-1"
                		}, li);
                		if(${mechanismOn}){
	                		var input = hqDojo.create("input", {
    	            			"type": "checkbox",
        	        			"value": summary.id,
            	    			"class": "checkbox",
                				"name":"deleteId"
                			}, span);
                		}
                		spanName = hqDojo.create("span", {
                			"class": "column span-3",
                			"innerHTML": summary.name
                		}, li);
                		if(summary.disabled){
                			span = hqDojo.create("span",{
                				"class":"disabled",
                				"innerHTML":"&nbsp;<fmt:message key='admin.managers.Plugin.column.plugin.disabled'/>"
                			},spanName);
                		}
                		span = hqDojo.create("span", {
                			"class": "column span-3",
                			"innerHTML": summary.version
                		}, li);
                		span = hqDojo.create("span", {
                			"class": "column span-4",
                			"innerHTML": summary.jarName
                		}, li);
                		span = hqDojo.create("span", {
                			"class": "column span-4",
                			"innerHTML": summary.initialDeployDate
                		}, li);
                		span = hqDojo.create("span", {
                			"class": "column span-4",
                			"innerHTML": summary.updatedDate
                		}, li);

                		var statusSpan = hqDojo.create("span", {
                			"class": "last column span-3"
                		}, li);
                		
                		if (summary.allAgentCount>0){      
                			if(summary.successAgentCount>0){
                				statusSpan.innerHTML+=summary.successAgentCount+"&nbsp;";
   	            				hqDojo.create("img",{
       	        					"src": "/images/icon_available_green.gif",
           	    				}, statusSpan); 
           	    				statusSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}
                			if (summary.inProgressAgentCount>0 || summary.errorAgentCount > 0){
                				var errorAgentSpan = hqDojo.create("span",{
        	        				"id":"errorAgent_"+summary.id,
            	    				"class":"agentStatusSpan"
                					}, statusSpan);
	                		if (summary.inProgressAgentCount>0) {
	                		    errorAgentSpan.innerHTML+=summary.inProgressAgentCount+"&nbsp;";
    	           				hqDojo.create("img",{
        	       					"src": "/images/arrow_refresh.png",
        	       					"id":"progressIcon_"+summary.id,
	        	       			}, errorAgentSpan);
	        	       			errorAgentSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}			
	                		if (summary.errorAgentCount > 0) {
                				errorAgentSpan.innerHTML+= summary.errorAgentCount+"&nbsp;";
                				hqDojo.create("img",{
                					"src": "/images/icon_available_red.gif",
                					"id":"errorIcon_"+summary.id,
                				}, errorAgentSpan);
                				errorAgentSpan.innerHTML+="</img>";
                			}
                			
                			}
                		}
                		index++;
                	});

		hqDojo.behavior.apply();
		
                
                },
                error: function(response, args) {
                	
                }
			});
		});
	setInterval("refreshPage()",10000);
	});

</script>