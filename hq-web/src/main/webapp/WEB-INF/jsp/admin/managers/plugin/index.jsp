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
        
	.selectFileBtn{
		-moz-border-radius: 4px 4px 4px 4px;
	    background: url("<spring:url value="/images/4.0/backgrounds/button-green-background.jpg"/>") repeat-x scroll center bottom #2DBF3D;
    	border: 1px solid #84B96D;
	    color: #FFFFFF;
	    /*cursor: pointer;*/
	    font-size: 0.9em;
	    font-weight: bold;
	    padding: 3px 15px;
	    width: 90px;
	    height: 16px;
	    float:right;
	}
	#uploadButtonBar{
		text-align: right;
	}

	
	
	#pluginManagerPanel {
		padding: 0.5em 1.5em;
		width:90%;
		min-width:400px;
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
		background: url("<spring:url value="/images/4.0/backgrounds/table_header_large.png"/>") repeat-x 0 0 transparent;
		overflow: hidden;
		border: 1px solid gray;
		border-top: 0;
	}
	
	#pluginManagerPanel .actionbar input[type="button"],
	#uploadPanel input[type="submit"],
	#confirmationPanel input[type="button"] {
		margin-right: 0.5em;
	}
	
	#pluginManagerPanel .actionbar input[type="button"]{
		margin: 0.25em 1em;
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
		overflow-x: hidden;
		height: 400px;
	}

	#pluginManagerPanel .gridheader {
		background: url("<spring:url value="/images/4.0/backgrounds/table_header_large.png"/>") repeat-x 0 0 transparent;
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
		text-align: left;
	}

	#removeErrorPanel, #confirmationPanel {
		width: 400px;
		text-align: left;
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
	#showUploadFormButton {
		float:right;
	}	
	#showRemoveConfirmationButton{
		float:left;
	}
	#pluginList .notFound{
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
	.topInfo, #currentTimeInfo{
		font-weight: bold;
		/* float: right;*/
		height: 20px;
		color: #777777;
	}
	.closeButton{
		margin-right:5px;
		float:right;
		visibility:hidden;
	}
	#showStatusPanelDialog{
		width: 505px;
		height: 410px;
		text-align: left;
	}
	
	#statusButtonBar {
		text-align: right;
		vertical-align:bottom;
	}
	#agentList{
		border: solid 1px;
		border-color: #BBBBBB;
		height: 290px;
		overflow: auto;
		margin: 10px;
		padding: 0px 30px;
		width:410px;
	}
	
	.errorAgent{
		list-style-image: url("<spring:url value="/images/icon_available_red.gif"/>");
	}
	.inProgressAgent{
		list-style-image: url("<spring:url value="/images/4.0/icons/alert.png"/>");
	}
	#searchText{
		background: url("<spring:url value="/images/4.0/icons/search.png"/>") no-repeat scroll 3px center #FFFFFF;
		color: #444444;
		padding: 0.25em 0.25em 0.25em 20px;
		width:250px;
	}
	input[type="text"]{
		margin:0.5em 0;
	}
	.selectInstruction{
		font-weight:bold;
		width:290px;
		float:left;
	}
	#selectedFileList{
		width:400px;
	}
	.uploaderInsideNode{
	}
	#selectFileBtn{
		width:60px;
		float:right;
	}
	.fileToUploadClose{
		background: url("<spring:url value="/images/dash_movecontent_del-on.gif"/>") no-repeat scroll 5px center #FFFFFF
	}
	#afterSelectFileInstruction{
		text-align:center;
		color:#777777;
	}
	.mechanismOffInstruction{
		background:yellow; 
	}	
	#loadingIcon{
		width:15px;
	}
	.infoIcon{
		width:12px;
	}

    #pluginManagerPanel .gridheader, #pluginManagerPanel .gridrow, 
    	#pluginManagerPanel .actionbar, #pluginManagerPanel ul{
    	min-width:980px;
	}
	.span-small{
		width:10%;
	}
	.span-med{
		width:15%;
	}
	.span-status{
		min-width: 120px;
		width:10%;
	}
	#selectFileButton{
		width:60px;
	}
	#progressMessage{
		float:left; 
		margin:0px 100px; 
		padding:5px 0px;
	}

</style>
<section id="pluginManagerPanel" class="container top">
	<h1><fmt:message key="admin.managers.plugin.title" /></h1>
	<p id="instruction"><fmt:message key="${instruction}" /></p>
	
	<div id="currentTimeInfo">
		<span style="float:right;" id="refreshTimeInfo"><fmt:message key="admin.managers.Plugin.information.refresh.time"/> <span id="timeNow"></span>
		</span>
		<span style="float:right;">&nbsp;&nbsp;</span>
		<img style="float:right;" id="refreshIcon" style="float:right;" src="<spring:url value="images/icon_info_small.gif" />" alt="refresh" /> 
	</div>

	<div class="topInfo">
		<span id="agentInfo" style="float:right">
			<fmt:message key="admin.managers.Plugin.information.agent.count"/>:&nbsp;
		    <span id="agentInfoAllCount">${allAgentCount}</span>
		    <img src="<spring:url value="/images/icon_info_small.gif"/>" class="infoIcon"/> <br/>
		</span>
	    <span style="float:left">
	        <fmt:message key="admin.managers.Plugin.information.legend"/>
	    	<img src="<spring:url value="/images/icon_available_green.gif"/>"/> <fmt:message key="admin.managers.Plugin.tip.icon.success"/>
	    	&nbsp; <img src="<spring:url value="/images/4.0/icons/alert.png"/>"/> <fmt:message key="admin.managers.Plugin.tip.icon.in.progress"/>
	   		&nbsp;<img src="<spring:url value="/images/icon_available_red.gif"/>"/> <fmt:message key="admin.managers.Plugin.tip.icon.error"/>
	    </span>		
	</div>
	
	<div class="gridheader clear">
		<span class="first column span-1">&nbsp;</span>
		<span class="column span-small"><fmt:message key="admin.managers.plugin.column.header.product.plugin" /></span>
		<span class="column span-med"><fmt:message key="admin.managers.plugin.column.header.version" /></span>
		<span class="column span-med"><fmt:message key="admin.managers.plugin.column.header.jar.name" /></span>
		<span class="column span-med" id="addedTimeHeader"><fmt:message key="admin.managers.plugin.column.header.initial.deploy.date" /><img src="/images/icon_info_small.gif" class="infoIcon"></span>
		<span class="column span-med" id="updatedTimeHeader"><fmt:message key="admin.managers.plugin.column.header.last.sync.date" /><img src="/images/icon_info_small.gif" class="infoIcon"></span>
		<span class="column span-status"><fmt:message key="admin.managers.plugin.column.header.status" /></span>
	</div>
	
	<form:form id="deleteForm" name="deleteForm" onsubmit="return false;" method="delete">
	
	<ul id="pluginList">
		<c:forEach var="pluginSummary" items="${pluginSummaries}" varStatus="index">
			<li class="gridrow clear<c:if test="${index.count % 2 == 0}"> even</c:if>">
				<span class="first column span-1">
					<c:if test="${mechanismOn}">
                    	<input type="checkbox" value="${pluginSummary.id}_${pluginSummary.jarName} (${pluginSummary.name})" name="deleteId"/>&nbsp; 
					</c:if>
				</span>
				<span class="column span-small">${pluginSummary.name}</span>
				<span class="column span-med">${pluginSummary.version}&nbsp;</span>
				<span class="column span-med">${pluginSummary.jarName}&nbsp;
					<c:if test="${pluginSummary.disabled}">
						<br/><span class="notFound"><fmt:message key="admin.managers.Plugin.column.plugin.disabled"/></span>
					</c:if>
				</span>
				<span class="column span-med">${pluginSummary.initialDeployDate}&nbsp;</span>
				<span class="column span-med">${pluginSummary.updatedDate}&nbsp;</span>		
				<span class="last column span-status" >
					<c:if test="${pluginSummary.allAgentCount>0}">
					    <c:if test="${pluginSummary.successAgentCount>0}">
					    	${pluginSummary.successAgentCount}&nbsp;<img class="successIcon" src="<spring:url value="/images/icon_available_green.gif" />"/>&nbsp;&nbsp;
					    </c:if>
					    
					    <c:if test="${pluginSummary.inProgressAgentCount>0 ||pluginSummary.errorAgentCount>0 }">
					    	<span id="${pluginSummary.name}_${pluginSummary.id}" class="agentStatusSpan">				    	
							<c:if test="${pluginSummary.inProgressAgentCount>0}">
						        ${pluginSummary.inProgressAgentCount}&nbsp;<img id="${pluginSummary.name}_${pluginSummary.id}" class="inProgressIcon" src="<spring:url value="/images/arrow_refresh.png"/>"/>&nbsp;&nbsp;
						   	</c:if>	
						   	<c:if test="${pluginSummary.errorAgentCount>0}">	   		
					   			${pluginSummary.errorAgentCount}&nbsp;<img id="${pluginSummary.name}_${pluginSummary.id}" class="errorIcon" src="<spring:url value="/images/icon_available_red.gif"/>"/>
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
			<span id="progressMessage">&nbsp;</span>
			<input id="showUploadFormButton" type="button" value="<fmt:message key="admin.managers.plugin.button.add.plugin" />" />
		</div>	
	</c:if>
</section>


<c:if test="${mechanismOn}" >
	<div id="uploadPanel" style="visibility:hidden;">
			<p>
				<span><fmt:message key="admin.managers.plugin.upload.dialog.label" />&nbsp;</span>
			</p>
			<p>
				<div class="selectInstruction"><fmt:message key="admin.managers.plugin.upload.dialog.instruction" />&nbsp;</div>
				<div id="selectFileButton" class="selectFileBtn"><fmt:message key="admin.managers.plugin.button.select.files" /></div>
			</p>
			<br/>
			<p>
			    <div><fmt:message key="admin.managers.plugin.upload.dialog.files.title" /></div>
				<div id="selectedFileList"></div>
				<p id="afterSelectFileInstruction"><fmt:message key="admin.managers.plugin.upload.dialog.instruction.after" /></p>
			</p>
			<p id="validationMessage" class="error" style="opacity:0; filter:alpha(opacity=0);zoom: 1;">&nbsp;</p>
			<div id="uploadButtonBar">
				<input id="uploadButton" type="button" value="<fmt:message key='admin.managers.plugin.button.upload' />" /> &nbsp;
				<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
			</div>
	</div>
	<div id="confirmationPanel" style="visibility:hidden;">
		<p><fmt:message key="admin.managers.plugin.confirmation.title" /></p>
		<ul id="removeList">
		</ul>
		<p><fmt:message key="admin.managers.plugin.confirmation.message" /></p>
		<div>
			<input id="removeButton" type="button" name="remove" value="<fmt:message key="admin.managers.plugin.button.remove" />" />
			<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
		</div>
	</div>
	<div id="removeErrorPanel" style="visibility:hidden;">
		<p id="removeErrorMsg"><fmt:message key="admin.managers.Plugin.remove.error.dialog.empty" /></p>
		<div>
			<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
		</div>
	</div>
</c:if>

<div id="showStatusPanel" style="visibility:hidden;">
	<div style="text-align:center; margin:0px;">
		<input type="text" id="searchText"/>
		<img id="loadingIcon" src="<spring:url value="/images/4.0/icons/ajax-loader-blue.gif"/>" style=""/>
	</div>
	<input type="hidden" id="pluginId"/>
	<input type ="hidden" id="pluginName"/>
	
	<ul id="agentList"></ul>
	
	<div id="statusButtonBar">
	    <span style="float:left">&nbsp;&nbsp; <img src="/images/4.0/icons/alert.png"/> <fmt:message key="admin.managers.Plugin.tip.icon.in.progress"/>
	   							  &nbsp;<img src="/images/icon_available_red.gif"/> <fmt:message key="admin.managers.Plugin.tip.icon.error"/>
	    </span>
		<a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.close" /></a>
	</div>
	
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
	hqDojo.require("dojo.hash");
	hqDojo.require("dojox.timing._base");
	hqDojo.require("dojo.date.locale");

	hqDojo.ready(function() {
		var timer = new hqDojox.timing.Timer();
		timer.setInterval(120000);
		timer.onTick = refreshPage;
		
		
		uncheckCheckboxes();
		updateTime();
		function uncheckCheckboxes(){
			hqDojo.forEach( hqDojo.query("input[type=checkbox]"), function(e){
					e.checked=false;
			});	    
		}
		
		hqDojo.connect(hqDojo.byId("refreshIcon"),"onclick",function(e){
			refreshPage();
		});
		
		function dateFormat(date){
			return hqDojo.date.locale.format(date,{
				selector: "date",
				datePattern: "hh:mm:ss aa"
			});
		};
		
		function updateTime(){
			var now = new Date();
			hqDojo.byId("timeNow").innerHTML=dateFormat(now);
			var anim = [
				hqDojo.animateProperty({
					node:"refreshTimeInfo",
					properties:{
						backgroundColor:"yellow"},
					duration:600
				}),
				hqDojo.animateProperty({
					node:"refreshTimeInfo",
					properties:{
						backgroundColor:"#EEEEEE"},
					duration:600
				})
			];
			hqDojo.fx.chain(anim).play();					
		}		
		function refreshPage(){
			hqDojo.style(hqDojo.byId("pluginList"), "color","#AAAAAA");
			var infoXhrArgs={
				preventCache:true,
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
				
		    var deleteIdsString = "";
			hqDojo.query("input[type=checkbox]:checked").forEach(function(entry){
					deleteIdsString+=entry.value+",";
			});
			if(deleteIdsString.length>1){
				deleteIdsString = deleteIdsString.substr(0,deleteIdsString.length-1);
			}else{
				deleteIdsString="";
			}
			
			var hashObj = {
				deleteIds: deleteIdsString
			}
			hqDojo.hash(hqDojo.objectToQuery(hashObj));
			
			hqDojo.xhrGet(infoXhrArgs);
			hqDojo.publish("refreshDataGrid");
		}

		new hqDijit.Tooltip({
			connectId:["addedTimeHeader"],
			label: "<fmt:message key='admin.managers.plugin.column.header.initial.deploy.date.tip' />"
		});
		
		new hqDijit.Tooltip({
			connectId:["updatedTimeHeader"],
			label: "<fmt:message key='admin.managers.plugin.column.header.last.sync.date.tip' />"
		});
		
		new hqDijit.Tooltip({
			connectId:["agentInfo"],
			label: "<fmt:message key='admin.managers.Plugin.information.agent.count.tip' />"
		});

		hqDojo.query(".notFound").forEach(function(e){
			new hqDijit.Tooltip({
				connectId: [e],
				label: "<fmt:message key='admin.managers.Plugin.column.plugin.disabled.tip' />"
			});		
		});	
		hqDojo.query(".inProgressIcon").forEach(function(e){
			new hqDijit.Tooltip({
				connectId: [e],
				label: "<fmt:message key='admin.managers.Plugin.tip.icon.in.progress' />"
			});		
		});
		hqDojo.query(".successIcon").forEach(function(e){
			new hqDijit.Tooltip({
				connectId: [e],
				label: "<fmt:message key='admin.managers.Plugin.tip.icon.success' />"
			});		
		});	
		hqDojo.query(".errorIcon").forEach(function(e){
			new hqDijit.Tooltip({
				connectId: [e],
				label: "<fmt:message key='admin.managers.Plugin.tip.icon.error' />"
			});		
		});
		function seeStatusDetail(pluginId,keyword){
			hqDijit.byId("showStatusPanelDialog").show();
			var agentListUl = hqDojo.byId("agentList");
			var searchWord = hqDojo.byId("searchText").value;
			var xhrArgs = {
					preventCache:true,
					url: "/app/admin/managers/plugin/status/"+pluginId+"?searchWord="+searchWord,
					load: function(response) {
						hqDojo.empty("agentList");
						hqDojo.style(hqDojo.byId("loadingIcon"),"visibility","hidden");
						hqDojo.forEach(response, function(agentStatus) {
							var statusLi = hqDojo.create("li",{
								"innerHTML":agentStatus.agentName
							});
							statusLi.innerHTML+=" <fmt:message key="admin.managers.Plugin.tip.status.sync.date" /> "+agentStatus.syncDate;
							if(agentStatus.status=="error"){
								hqDojo.addClass(statusLi,"errorAgent");
							}else{
								hqDojo.addClass(statusLi,"inProgressAgent");
							};
							agentListUl.appendChild(statusLi);
						});
					},
					handleAs: "json",
					headers: { 
	 	               	"Content-Type": "application/json",
    	            	"Accept": "application/json"
        	        }
				
			};
			hqDojo.xhrGet(xhrArgs);			
			hqDojo.byId("showStatusPanelDialog_title").innerHTML=hqDojo.byId("pluginName").value + "&nbsp;-&nbsp;"+ "<fmt:message key="admin.managers.Plugin.tip.status.title" />";
		}

		hqDojo.behavior.add({
			".agentStatusSpan":{
				onclick: function(evt){
					var anchor = evt.target.id.indexOf("_");
					var pluginId = evt.target.id.substr(anchor+1,evt.target.id.length);
					var pluginName = evt.target.id.substr(0,anchor);
					hqDojo.byId("pluginName").value=pluginName;
					hqDojo.byId("pluginId").value=pluginId;
					seeStatusDetail(pluginId);
				},
				found: function(node){hqDojo.style(node,"cursor","pointer");}
			}
		});
		
		hqDojo.behavior.apply();
		
		var showStatusDialog = new hqDijit.Dialog({
			id: "showStatusPanelDialog"
		});
		
		var showStatusPanel = hqDojo.byId("showStatusPanel");
		hqDojo.style(showStatusDialog.closeButtonNode,"visibility", "hidden" );
		showStatusDialog.setContent(showStatusPanel);
		hqDojo.style(showStatusPanel, "visibility", "visible");
			
		hqDojo.query("#showStatusPanelDialog .cancelLink").onclick(function(e) {
			uncheckCheckboxes();
			hqDijit.byId("showStatusPanelDialog").hide();
			hqDojo.empty("agentList");
			hqDojo.byId("searchText").value="";
		});
		 
		hqDojo.connect(hqDojo.byId("searchText"),"onkeyup",function(e){
			var pluginId = hqDojo.byId("pluginId").value;
			seeStatusDetail(pluginId,hqDojo.byId("searchText").value);			
		});
		
		if(${!mechanismOn}){
			hqDojo.attr("deleteForm","class","mechanismOff");
			hqDojo.addClass(hqDojo.byId("instruction"),"mechanismOffInstruction");
		}
	
		if (${mechanismOn}){
		
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

			hqDojo.query("#uploadPanelDialog .cancelLink").onclick(function(e) {
				uncheckCheckboxes();
				hqDijit.byId("uploadPanelDialog").hide();

			});

			hqDojo.query("#removePanelDialog .cancelLink").onclick(function(e) {
				uncheckCheckboxes();
				hqDijit.byId("removePanelDialog").hide();
			});
			hqDojo.query("#removeErrorPanelDialog .cancelLink").onclick(function(e) {
				hqDijit.byId("removeErrorPanelDialog").hide();
			});

		
			hqDojo.connect(hqDojo.byId("showUploadFormButton"), "onclick", function(e) {
				
				hqDijit.registry.filter(function(e){return e.id=="selectFileButton";}).forEach(function(entry){
						hqDijit.registry.remove("selectFileButton");
				});				
				hqDojo.query("div > #selectFileButton").forEach(function(e){
					hqDojo.removeAttr(e,"class");
					hqDojo.attr(e,"class","selectFileBtn");
					hqDojo.removeAttr(e,"wigetid");
					hqDojo.removeAttr(e,"style");
				});
				hqDojo.byId("selectFileButton").innerHTML = "<fmt:message key='admin.managers.plugin.button.select.files' />"
				hqDojo.byId("selectedFileList").innerHTML = "";
				uploader = new hqDojox.form.FileUploader({
					selectMultipleFiles:true,
					fileListId:"selectedFileList",
					isDebug:false,
					uploadUrl:"<spring:url value='/app/admin/managers/plugin/upload'/>",
					force:"html",
					closable: true,
					fileMask:[
						["jar File", "*.jar"],
						["xml File", "*.xml"]
					]
				}, "selectFileButton");
			
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
				hqDijit.byId("uploadPanelDialog").show();
			});
			
			hqDojo.connect(hqDojo.byId("uploadButton"), "onclick", function(e){
				var fileTypeCorrect=true;
				var pluginList = hqDojo.query("input[type=file]", hqDojo.byId("hqDijit_FileUploaderForm_0"));
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
					hqDijit.byId("uploadPanelDialog").hide();
					uploader.upload();
					hqDijit.registry.remove("selectFileButton");
				}
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
			
			function pluginMapping(id,content){
				this.id=id;
				this.content=content;
			}
			
			hqDojo.connect(hqDojo.byId("showRemoveConfirmationButton"), "onclick", function(e) {
				var checkedPlugins = hqDojo.filter(hqDojo.query("input[type=checkbox]"), function(e){ return e.checked; });
				hqDojo.empty("removeList");
				
				if(checkedPlugins.length>0){
					hqDojo.forEach(checkedPlugins,function(checkedPlugin){
						var pluginName = checkedPlugin.value.split("_")[1];

						if (pluginName!=undefined){
							hqDojo.create("li", {
                				"innerHTML":pluginName
                			}, "removeList");
						}
					});
					hqDijit.byId("removePanelDialog").show();
				}else{
					hqDijit.byId("removeErrorPanelDialog").show();
				}
			});

			hqDojo.connect(hqDojo.byId("removeButton"), "onclick", function(e) {
				var checkedPlugins = hqDojo.filter(hqDojo.query("input[type=checkbox]"), function(e){ return e.checked; });
       			hqDojo.forEach(checkedPlugins,function(checkedPlugin){
       				checkedPlugin.value=checkedPlugin.value.split("_")[0];
       			});
				var xhrArgs = {
					preventCache:true,
					form: hqDojo.byId("deleteForm"),
					url: "<spring:url value='/app/admin/managers/plugin/delete' />",
					load: function(response) {
						if (response=="success") {
							hqDojo.attr("progressMessage", "class", "information");
							hqDojo.byId("progressMessage").innerHTML = '<fmt:message key="admin.managers.plugin.message.remove.success" />';
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
						refreshPage();
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
						refreshPage();
					}
				};
				hqDojo.xhrPost(xhrArgs);
				hqDijit.byId("removePanelDialog").hide(); 
			});
		}
		
		hqDojo.subscribe("refreshDataGrid", function() {			
			hqDojo.xhrGet({
				preventCache:true,
				url: "<spring:url value='/app/admin/managers/plugin/list' />",
				handleAs: "json",
				headers: { 
                	"Content-Type": "application/json",
                	"Accept": "application/json"
                },
                load: function(response, args) {
                	hqDojo.style(hqDojo.byId("pluginList"), "color","#000000");
                	updateTime();
                	timer.stop();
                	timer.start();
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
    	            			"name": "deleteId",
        	        			"value": summary.id+"_"+summary.jarName+" ("+summary.name+")"
                			}, span);
                		}
                		span = hqDojo.create("span", {
                			"class": "column span-small",
                			"innerHTML": summary.name
                		}, li);
						if(summary.version==null){
							var version = "";
						}else{
							var version = summary.version;
						}
                		span = hqDojo.create("span", {
                			"class": "column span-med",
                			"innerHTML": version
                		}, li);
                		spanName = hqDojo.create("span", {
                			"class": "column span-med",
                			"innerHTML": summary.jarName
                		}, li);
                		if(summary.disabled){
                			span = hqDojo.create("span",{
                				"class":"notFound",
                				"innerHTML":"<br/><fmt:message key='admin.managers.Plugin.column.plugin.disabled'/>"
                			},spanName);
                		}
                		span = hqDojo.create("span", {
                			"class": "column span-med",
                			"innerHTML": summary.initialDeployDate
                		}, li);
                		span = hqDojo.create("span", {
                			"class": "column span-med",
                			"innerHTML": summary.updatedDate
                		}, li);

                		var statusSpan = hqDojo.create("span", {
                			"class": "last column span-status"
                		}, li);
                		
                		if (summary.allAgentCount>0){      
                			if(summary.successAgentCount>0){
                				statusSpan.innerHTML+=summary.successAgentCount+"&nbsp;";
   	            				hqDojo.create("img",{
       	        					"src": "/images/icon_available_green.gif",
       	        					"class": "successIcon"
           	    				}, statusSpan); 
           	    				statusSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}
                			if (summary.inProgressAgentCount>0 || summary.errorAgentCount > 0){
                				var errorAgentSpan = hqDojo.create("span",{
        	        				"id":summary.name+"_"+summary.id,
            	    				"class":"agentStatusSpan"
                					}, statusSpan);
	                		if (summary.inProgressAgentCount>0) {
	                		    errorAgentSpan.innerHTML+=summary.inProgressAgentCount+"&nbsp;";
    	           				hqDojo.create("img",{
        	       					"src": "/images/4.0/icons/alert.png",
        	       					"class": "inProgressIcon",
        	       					"id":summary.name+"_"+summary.id
	        	       			}, errorAgentSpan);
	        	       			errorAgentSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                			}			
	                		if (summary.errorAgentCount > 0) {
                				errorAgentSpan.innerHTML+= summary.errorAgentCount+"&nbsp;";
                				hqDojo.create("img",{
                					"src": "/images/icon_available_red.gif",
                					"class": "errorIcon",
                					"id":summary.name+"_"+summary.id
                				}, errorAgentSpan);
                				errorAgentSpan.innerHTML+="</img>";
                			}
                			
                			}
                		}
                		index++;
                	});
                	
					var hashObj = hqDojo.queryToObject(hqDojo.hash());
					if(hashObj.deleteIds!=""){
						hqDojo.forEach(hashObj.deleteIds.split(","),function(pluginId){
							var checkbox = hqDojo.query("input[value='"+pluginId+"']");
							if(checkbox[0]!=null){
								checkbox[0].checked="true";
							}
						});
					}

					hqDojo.behavior.apply();
					hqDojo.query(".notFound").forEach(function(e){
						new hqDijit.Tooltip({
							connectId: [e],
							label: "<fmt:message key='admin.managers.Plugin.column.plugin.disabled.tip' />"
						});		
					});
					
					hqDojo.query(".inProgressIcon").forEach(function(e){
						new hqDijit.Tooltip({
							connectId: [e],
							label: "<fmt:message key='admin.managers.Plugin.tip.icon.in.progress' />"
						});		
					});
					hqDojo.query(".successIcon").forEach(function(e){
						new hqDijit.Tooltip({
							connectId: [e],
							label: "<fmt:message key='admin.managers.Plugin.tip.icon.success' />"
						});		
					});	
					hqDojo.query(".errorIcon").forEach(function(e){
						new hqDijit.Tooltip({
							connectId: [e],
							label: "<fmt:message key='admin.managers.Plugin.tip.icon.error' />"
						});		
					});
                },
                error: function(response, args) {
                	
                }
			});
		});

		timer.start();
	});

</script>