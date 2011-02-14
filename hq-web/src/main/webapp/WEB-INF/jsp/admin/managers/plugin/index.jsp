<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<style>
	#pluginManagerPanel {
		margin-top: 1em;
		padding:1em;
		background-color: #fff;
		border: 1px solid #444;
		-moz-border-radius: 10px;
		-webkit-border-radius: 10px;
	}
	
	#pluginManagerPanel h1 {
		color: #444;
		font-size: 2em;
		font-weight: bold;
		margin-top: 1em;
	}
	
	#pluginManagerPanel ul {
		margin: 0;
		padding: 0;
		list-style: none;
		background-color: #fff;
		border: 1px solid gray;
		overflow: hidden;
	}
	
	#pluginManagerPanel li.gridheader {
		background: url("/images/4.0/backgrounds/table_header_large.png") repeat scroll 0 0 transparent;
		overflow: hidden;
		font-weight: bold;
		font-size: 1.1em;
		color: #444;
	}
	
	#pluginManagerPanel li.gridheader span.column {
		padding-top: 0.5em;
		padding-left: 0.5em;
		height: 25px;	
	}
	
	#pluginManagerPanel li.gridrow {
		overflow: hidden;
	}
	
	#pluginManagerPanel li.gridrow span.column {
		padding: 0.5em 0;
		padding-left: 0.5em;
	}
	
	#pluginManagerPanel li.gridrow span.first {
		text-align: center;
	}
	
	#pluginManagerPanel li.even {
		background-color: #eee;
	}
</style>
<section id="pluginManagerPanel" class="container top">
	<span class="span-4 append-20">
		<a href="/Admin.do"><fmt:message key="admin.managers.plugin.back.to.administration" /></a>
	</span>
	<br/>
	<h1><fmt:message key="admin.managers.plugin.title" /></h1>
	<p><fmt:message key="admin.managers.plugin.instructions" /></p>
	<span class="last span-24 actionbar">
		<input id="showUploadFormButton" type="button" value="<fmt:message key="admin.managers.plugin.button.add.plugin" />" />
	</span>
	<ul>
		<li class="gridheader clear">
			<!-- span class="first column span-1">&nbsp;</span -->
			<span class="column span-4"><fmt:message key="admin.managers.plugin.column.header.product.plugin" /></span>
			<span class="column span-4"><fmt:message key="admin.managers.plugin.column.header.jar.name" /></span>
			<span class="column span-5"><fmt:message key="admin.managers.plugin.column.header.initial.deploy.date" /></span>
			<span class="last column span-5"><fmt:message key="admin.managers.plugin.column.header.status" /></span>
		</li>
		<c:forEach var="pluginSummary" items="${pluginSummaries}" varStatus="index">
			<li class="gridrow clear<c:if test="${index.count % 2 == 0}"> even</c:if>">
				<span class="column span-4 header">${pluginSummary.name}&nbsp;</span>
				<span class="column span-4 header">${pluginSummary.jarName}&nbsp;</span>
				<span class="column span-5 header">${pluginSummary.initialDeployDate}&nbsp;</span>
				<span class="last column span-5 header">${pluginSummary.status}&nbsp;</span>
			</li>
		</c:forEach>
	</ul>
	<div id="progressMessage"></div>
</section>
<div id="uploadPanel" style="visibility:hidden;">
	<form id="uploadForm" name="uploadForm" action="<spring:url value="/app/admin/managers/plugin/upload" />" method="POST" enctype="multipart/form-data">
		<fieldset>
			<legend><fmt:message key="admin.managers.plugin.upload.dialog.instruction" /></legend>
			<label for="plugin"><fmt:message key="admin.managers.plugin.upload.dialog.label" /></label>
			<input type="file" id="plugin" name="plugin" />
		</fieldset>
		<input id="uploadButton" type="submit" name="upload" value="<fmt:message key="admin.managers.plugin.button.upload" />" />
		<a href="#" id="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
	</form>
</div>
<script>
	dojo.require("dojo.io.iframe");
	dojo.require("dijit.Dialog");

	dojo.ready(function() {
		var dialog = new dijit.Dialog({
			id: "uploadPanelDialog",
			title: "<fmt:message key="admin.managers.plugin.upload.dialog.title" />"
		});
		
		var uploadPanel = dojo.byId("uploadPanel");
		
		dojo.style(dialog.closeButtonNode, "visibility", "hidden");
		dialog.setContent(uploadPanel);
		dojo.style(uploadPanel, "visibility", "visible");
		dojo.connect(dojo.byId("showUploadFormButton"), "onclick", function(e) {
			dojo.byId("plugin").value = "";
			dijit.byId("uploadPanelDialog").show();
		});
		dojo.connect(dojo.byId("cancelLink"), "onclick", function(e) {
			dijit.byId("uploadPanelDialog").hide();
		});
		
		var upload = function() {
			dojo.io.iframe.send({
				form: "uploadForm",
				handleAs: "json",
				url: "<spring:url value="/app/admin/managers/plugin/upload" />",
				load: function(response, args) {
					dojo.byId("progressMessage").innerHTML = response.message;
				},
				error: function(response, args) {
					dojo.byId("progressMessage").innerHTML = response.message;
				}
			});
		};
		
		dojo.connect(dojo.byId("uploadForm"), "onsubmit", function(e) {
			upload();
			
			dijit.byId("uploadPanelDialog").hide();
			
			return false;
		});
	});
</script>