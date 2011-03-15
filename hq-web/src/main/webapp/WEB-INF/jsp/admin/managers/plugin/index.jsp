<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<style>
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
	
	#removeErrorPanel {
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
	#progressMessage.error {
	    color: #bb0000;
	}
	
	#showRemoveConfirmationButton{
		float:left;
	}
	.errorAgentSpan{
		color: #0066CC;
		font-weight: bold;
	}
	.errorAgentSpan:focus,
	.errorAgentSpan:hover{
		color: #0066FF;
	}
	.errorAgentTip{
		overflow:auto;
		height:300px;
		width:400px;
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
</style>
<section id="pluginManagerPanel" class="container top">
	<h1><fmt:message key="admin.managers.plugin.title" /></h1>
	<p><fmt:message key="admin.managers.plugin.instructions" /></p>
	
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
                    <input type="checkbox" name="deleteId" value="${pluginSummary.id}" class="checkbox" />&nbsp; 
				</span>
				<span class="column span-3">${pluginSummary.name}&nbsp;</span>
				<span class="column span-3">${pluginSummary.version}&nbsp;</span>
				<span class="column span-4">${pluginSummary.jarName}&nbsp;</span>
				<span class="column span-4">${pluginSummary.initialDeployDate}&nbsp;</span>
				<span class="column span-4">${pluginSummary.updatedDate}&nbsp;</span>		
				<span class="last column span-3" >
					<c:if test="${pluginSummary.allAgentCount>0}">
						<c:if test="${pluginSummary.inProgressAgentCount>0}">
					        ${pluginSummary.inProgressAgentCount}&nbsp;<img src="/images/arrow_refresh.png"/>&nbsp;&nbsp;
					   	</c:if>	
					    <c:if test="${pluginSummary.successAgentCount>0}">
					    	${pluginSummary.successAgentCount}&nbsp;<img src="/images/icon_available_green.gif"/>&nbsp;&nbsp;
					    </c:if>
				    </c:if>
				    
				   	<c:if test="${pluginSummary.errorAgentCount>0}">
				   		<span id="errorAgent_${index.count}" class="errorAgentSpan">
				   			${pluginSummary.errorAgentCount}&nbsp;<img src="/images/icon_available_red.gif"/>
				    	</span>
					</c:if>
				</span>
			</li>
		</c:forEach>
	</ul>
	</form:form>
	<div class="actionbar">
		<input id="showRemoveConfirmationButton" type="button" value="<fmt:message key="admin.managers.plugin.button.remove.plugin" />" />
		<span id="progressMessage"></span>
		<input id="showUploadFormButton" type="button" value="<fmt:message key="admin.managers.plugin.button.add.plugin" />" />
	</div>	
</section>



<div id="uploadPanel" style="visibility:hidden;">
	<form id="uploadForm" name="uploadForm" onsubmit="return false;" method="POST" enctype="multipart/form-data">
		<strong><fmt:message key="admin.managers.plugin.upload.dialog.instruction" /></strong>
		<p>
			<span><fmt:message key="admin.managers.plugin.upload.dialog.label" /></span>
		</p>
		<p>
			<input type="file" id="plugin" name="plugin" />
		</p>
		<p id="validationMessage" class="error" style="opacity:0;">&nbsp;</p>
		<div>
			<input id="uploadButton" type="submit" name="upload" value="<fmt:message key="admin.managers.plugin.button.upload" />" />
			<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
		</div>
	</form>
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



<script>
	hqDojo.require("dojo.fx");
	hqDojo.require("dojo.io.iframe");
	hqDojo.require("dijit.dijit");
	hqDojo.require("dijit.Dialog");
	hqDojo.require("dijit.form.Button");
	hqDojo.require("dijit.Tooltip");
	
		function refreshPage(){
			var infoXhrArgs={
				url:"<spring:url value='/app/admin/managers/plugin/agentInfo'/>",
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
		
	new hqDijit.Tooltip({
		connectId:["agentInfo"],
		label: "<fmt:message key='admin.managers.Plugin.information.agent.count.tip' />"
	});
	
		<c:forEach var="pluginSummary" items="${pluginSummaries}" varStatus="index">
			if(${pluginSummary.errorAgentCount>0}){
				var errorAgentTip_${index.count} = hqDojo.create("div",{
					"class":"errorAgentTip"
				});
				
				var errorAgentTipTitleSpan_${index.count} = hqDojo.create("p",{
					"class":"errorAgentTitle",
					"innerHTML":"<fmt:message key='admin.managers.Plugin.tip.status.title' /> ${pluginSummary.name}"
				},errorAgentTip_${index.count});
				
				hqDojo.create("input",{
					"type":"button",
					"id": "closeErrorAgentTipButton_${index.count}",
					"class":"closeButton",
					"value":"x"},
					errorAgentTipTitleSpan_${index.count}
				);
				
				<c:if test="${pluginSummary.inProgressAgentCount>0}">
					var errorUl=hqDojo.create("ul",{
						"class":"inProgressAgentList"
						},errorAgentTip_${index.count});
				</c:if>
				<c:if test="${pluginSummary.errorAgentCount>0}">
					var errorUl=hqDojo.create("ul",{
						"class":"errorAgentList"
						},errorAgentTip_${index.count});
					<c:forEach var="agent" items="${pluginSummary.errorAgents}">
						li = hqDojo.create("li",{
							"innerHTML":"${agent.agentName} <fmt:message key='admin.managers.Plugin.tip.status.sync.fail'/> ${agent.syncDate}"
						},errorUl);
					</c:forEach>
				</c:if>
				
				var dialog_${index.count} = new hqDijit.TooltipDialog();
				dialog_${index.count}.setContent(errorAgentTip_${index.count});
				
				hqDojo.connect(hqDojo.byId("errorAgent_${index.count}"),"onmouseenter", function(e){
					hqDijit.popup.open({
						popup: dialog_<c:out value="${index.count}"/>, 
                        around: hqDojo.byId("errorAgent_${index.count}")
					});
				});
				hqDojo.connect(hqDojo.byId("closeErrorAgentTipButton_${index.count}"),"onclick", function(e){
					hqDijit.popup.close(dialog_${index.count});
				});				
			}
		</c:forEach>
	
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
			hqDojo.byId("plugin").value = "";
			hqDijit.byId("uploadPanelDialog").show();
		});
		hqDojo.connect(hqDojo.byId("showRemoveConfirmationButton"), "onclick", function(e) {
			var checkedPlugins = hqDojo.filter(hqDojo.query(".checkbox"), function(e){ return e.checked; });
			if(checkedPlugins.length>0){
				hqDojo.byId("plugin").value = "";
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
                	
                	var index = 0;
                	
                	hqDojo.forEach(response, function(summary) {
                		var li = hqDojo.create("li", {
                			"class": "gridrow clear" + (((index+1) % 2 == 0) ? " even" : "")
                		}, "pluginList");
                		var span = hqDojo.create("span", {
                			"class": "first column span-1"
                		}, li);
                		var input = hqDojo.create("input", {
                			"type": "checkbox",
                			"value": summary.id,
                			"class": "checkbox",
                			"name":"deleteId"
                		}, span);
                		span = hqDojo.create("span", {
                			"class": "column span-3",
                			"innerHTML": summary.name
                		}, li);
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
	                		if (summary.inProgressAgentCount>0) {
	                		    statusSpan.innerHTML+=summary.inProgressAgentCount+"&nbsp;";
    	           				hqDojo.create("img",{
        	       					"src": "/images/arrow_refresh.png",
	        	       			}, statusSpan);
	        	       			statusSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}	
                			if(summary.successAgentCount>0){
                				statusSpan.innerHTML+=summary.successAgentCount+"&nbsp;";
   	            				hqDojo.create("img",{
       	        					"src": "/images/icon_available_green.gif",
           	    				}, statusSpan); 
           	    				statusSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}
	                		if (summary.errorAgentCount > 0) {
    	            			var errorAgentSpan = hqDojo.create("span",{
        	        				"id":"errorAgent_"+(index+1),
            	    				"class":"errorAgentSpan"
                				}, statusSpan);
                				errorAgentSpan.innerHTML+= summary.errorAgentCount+"&nbsp;";
                				hqDojo.create("img",{
                					"src": "/images/icon_available_red.gif",
                				}, errorAgentSpan);
                				errorAgentSpan.innerHTML+="</img>";
                				
                				hqDojo.connect(hqDojo.byId("errorAgent_${index+1}"),"onmouseenter", function(e){
									hqDijit.popup.open({
										popup: dialog_${index+1}, 
                       		 			around: hqDojo.byId("errorAgent_${index+1}")
									});
								});
                			}
                		}
                		

                		index++;
                	});
                
				

				
                },
                error: function(response, args) {
                	
                }
			});
		});
		hqDojo.connect(hqDojo.byId("uploadForm"), "onsubmit", function(e) {
			var filePath = hqDojo.byId("plugin").value;
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
			
			hqDojo.io.iframe.send({
				form: "uploadForm",
				handleAs: "json",
				url: "<spring:url value="/app/admin/managers/plugin/upload" />",
				load: function(response, args) {
					var diplayCount = 5000;
					
					if (response.success) {
						hqDojo.attr("progressMessage", "class", "information");
						hqDojo.publish("refreshDataGrid");
					} else {
						hqDojo.attr("progressMessage", "class", "error");
						diplayCount = 10000;
					}
					
					hqDojo.byId("progressMessage").innerHTML = response.message;
					var anim = [hqDojo.fadeIn({
									node: "progressMessage",
									duration: 500
								}),
								hqDojo.fadeOut({
									node: "progressMessage",
									delay: diplayCount,
									duration: 500
								})];
					hqDojo.fx.chain(anim).play();						
				},
				error: function(response, args) {
					hqDojo.attr("progressMessage", "class", "error");
					hqDojo.byId("progressMessage").innerHTML = response.message;
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
			});
			
			hqDijit.byId("uploadPanelDialog").hide();
			
			return false;
		});

		hqDojo.connect(hqDojo.byId("removeButton"), "onclick", function(e) {
			var xhrArgs = {
				form: hqDojo.byId("deleteForm"),
				url: "/app/admin/managers/plugin/delete",
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
		

	});
	
	setInterval("refreshPage()",10000);
</script>