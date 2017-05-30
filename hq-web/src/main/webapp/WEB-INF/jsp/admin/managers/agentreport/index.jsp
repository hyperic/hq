<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

 <% 
	String agentType = request.getHeader("User-Agent");
	boolean iemode=false;
	if (agentType.contains("MSIE")) {
		iemode = true;
	}
 %>

	<link rel="stylesheet" type="text/css" href='<spring:url value="/static/css/core/theme.css" />'>
		
	<link rel="stylesheet" href="<spring:url value="/static/css/blueprint/screen.css" />" type="text/css" media="screen, projection">
	<link rel="stylesheet" href="<spring:url value="/static/css/blueprint/print.css" />" type="text/css" media="print">	
	<link rel="stylesheet" type="text/css" href="/static/css/core/type.css">
	
	<link rel="stylesheet" type="text/css" href="/static/css/xstyle/overrides.css">
	<!--[if lt IE 8]><link rel="stylesheet" href="/static/css/core/ie.css"><![endif]-->
		
	<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/admin/managers/plugin/pluginMgr.css"/>" />
	<style>
		body {
			line-height: 1.2 !important;
			background: #EFEFEF;
		}
		
		#migContainer {
			padding-top: 0px;
		}
		
		#internalContainer {
			background: #EFEFEF !important;
<% if (iemode) { %>
			padding-left: 75px !important;
<% } else { %>
			padding-left: 0px !important;
<% } %>
			vertical-align: top;
		}
		
		#aboutAnchor {
			text-align: center;
		}
		
		input[type="submit"], input[type="button"] {
			padding: 3px 15px;
		}
		
		#pageTitle {
			display:none;
		}
<% if (iemode) { %>
		#pluginManagerPanel .gridrow span.column {
			padding-top: 0em;
			padding-left: 0.5em;
		}
		
		#showUploadFormButton {
			color: #fff;
			border: 1px solid #84B96D;
			background: #2DBF3D url('../../images/4.0/button-green-background.jpg') repeat-x bottom;
			-moz-border-radius: 4px;
			-webkit-border-radius: 4px;
			border-radius: 3px;
			font-weight: 700;
			font-size: 11px;
			cursor: pointer;
			padding: 3px 15px;
			line-height: 1.5;
			font-family: arial, sans-serif;
			margin-bottom:0.25em;
			margin-left:1em;
			margin-right:1em;
			margin-top:0.25em;
		}
		
		#showRemoveConfirmationButton {
			color: #fff;
			border: 1px solid #84B96D;
			background: #2DBF3D url('../../images/4.0/button-green-background.jpg') repeat-x bottom;
			-moz-border-radius: 4px;
			-webkit-border-radius: 4px;
			border-radius: 3px;
			font-weight: 700;
			font-size: 11px;
			cursor: pointer;
			padding: 3px 15px;
			line-height: 1.5;
			font-family: arial, sans-serif;
			margin-bottom:0.25em;
			margin-left:1em;
			margin-right:1em;
			margin-top:0.25em;
		}
<% } %>		
	</style>
<% if (iemode) { %>	
	<div id="pluginManagerPanel" class="container top" style="width:97%">
<% } else { %>
	<section id="pluginManagerPanel" class="container top">
<% } %>
		<h1>
		Report	
		</h1> 
    
    

    
    
    <div class="gridheader clear">
        <span class="column span-30">FQDN</span>
        <span class="column span-8">Address</span>
        <span class="column span-20">OS Type</span>
        <span class="column span-42">Plugins</span>
    </div>
    
    <form:form id="deleteForm" name="deleteForm" onsubmit="return false;" method="delete" >
    
    <ul id="pluginList">
        <li>&nbsp;</li><li>&nbsp;</li>
        <li>
            <img id="tableLoadingIcon" src='<spring:url value="/static/images/ajax-loader-blue.gif"/>'/>
        </li>
    </ul>
    </form:form>
    
    <c:if test="${mechanismOn}" >
<% if (iemode) { %>	
	<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
	<td>
<% } %>
	<div class="actionbar"> 			
		
	</div>  
<% if (iemode) { %>	
	</td>
	</tr>
	</table>
<% } %>
    </c:if>

    

<% if (iemode) { %>	
	</div>
<% } else { %>
	</section>
<% } %>


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
        <span id="deletLoadingIcon" style="visibility:hidden;"><img src='<spring:url value="/static/images/ajax-loader-blue.gif"/>'/></span>

        <p><fmt:message key="admin.managers.plugin.confirmation.title" /></p>
        <ul id="removeList">
        </ul>
        <p><fmt:message key="admin.managers.plugin.confirmation.message" /></p>
        <div>
            <input id="removeButton" type="button" name="remove" value="<fmt:message key="admin.managers.plugin.button.remove" />" />
            <a href="#" id="removeCancel" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
        </div>
    </div>
    <div id="errorMsgPanel" style="visibility:hidden;">
        <p id="errorMsg"></p>
        <div>
            <a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.cancel" /></a>
        </div>
    </div>
</c:if>

<div id="showStatusPanel" style="visibility:hidden;">
    <div style="text-align:center; margin:0px;">
        <input type="text" id="searchText"/>
        <img id="loadingIcon" src='<spring:url value="/static/images/ajax-loader-blue.gif"/>' alt="loading"/>
    </div>
    <input type="hidden" id="pluginId"/>
    <input type ="hidden" id="pluginName"/>
    <input type="hidden" id="status"/>
    
    <ul id="agentList"></ul>
    
    <div id="statusButtonBar">
        <a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.close" /></a>
    </div>
</div>
<div id="agentSummaryPanel" style="visibility:hidden;">
    <div>
    <img id="summaryLoadingIcon" src='<spring:url value="/static/images/ajax-loader-blue.gif"/>' alt="loading"/>
    <ul id="agentSummaryList"></ul>
    </div>
    <a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.close" /></a>
</div>
<div id="unsyncAgentsSummaryPanel" style="visibility:hidden;">
    <div>
        <h3>
            <fmt:message key="admin.managers.Plugin.summary.oldagents.server" />
            &nbsp;${info.serverVersion}
        </h3>
        <p><fmt:message key="admin.managers.Plugin.summary.oldagents.content" /></p>
    </div>
    <div class="gridheader clear">       
        <span class="column span-large"><fmt:message key="admin.managers.Plugin.summary.oldagents.agent.name" /></span> 
        <span class="column span-med"><fmt:message key="admin.managers.Plugin.summary.oldagents.agent.version" /></span>
    </div>
    <img id="oldAgentSummaryLoadingIcon"
            src='<spring:url value="/static/images/ajax-loader-blue.gif"/>' alt="loading" />
    <div class="oldSummaryListDiv">
	    <ul id="oldAgentSummaryList">
	    </ul>
	</div>
    <div>
      <p><fmt:message key="admin.managers.Plugin.summary.unsynchable.curagents.titlebreak" /></p>
      <p><fmt:message key="admin.managers.Plugin.summary.unsynchable.curagents.content" /></p>
    </div>
    <div class="gridheader clear">       
        <span class="column span-large"><fmt:message key="admin.managers.Plugin.summary.unsynchable.curagents.agent.name" /></span> 
        <span class="column span-med"><fmt:message key="admin.managers.Plugin.summary.unsynchable.curagents.agent.version" /></span>
    </div>
    <div class="curSummaryListDiv">
    	<ul id="currentNonSyncAgentSummaryList">
    	</ul>
	</div>

    <a href="#" class="cancelLink"><fmt:message key="admin.managers.plugin.button.close" /></a>
</div>

<script src='<spring:url value="/static/js/admin/managers/plugin/pluginMgr.js" />' type="text/javascript"></script> 
<script>
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
    hqDojo.require("dijit._base.scroll");
    
    hqDojo.ready(function() {
        var timer = new hqDojox.timing.Timer();
            hqDojo.xhrGet({
                preventCache:true,
                url: "<spring:url value='/app/admin/managers/agentreport/list' />",
                handleAs: "json",
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                load: function(response, args) {
                    hqDojo.style(hqDojo.byId("pluginList"), "color","#000000");
                    
                    timer.stop();
                    timer.start();
                    hqDojo.empty("pluginList");
                    var index = 1;
                    
                    hqDojo.forEach(response, function(summary) {
                        var liClass = "";
                        var version;
                        liClass=(((index) % 2 === 0) ? " even" : "");
                        var li = hqDojo.create("li", {
							"class": "gridrow clear" + liClass
                        }, "pluginList");
						 var pluginName = hqDojo.create("span", {
                            "class": "column span-30",
                            "innerHTML": summary.fqdn,
                            "id": "row_"+summary.id,
                            "style" :"padding-right: 0.414723473%;"
                        }, li);
						hqDojo.create("span", {
                            "class": "column span-8",
                            "innerHTML": summary.ip,
                            "style" :"padding-right: 0.122067043%;"
                        }, li);
						var spanName = hqDojo.create("span", {
                            "class": "column span-20",
                            "innerHTML": summary.os,
                            "style" :"padding-right: 0.122067043%;"
                        }, li);
                        hqDojo.create("span", {
                            "class": "column span-42",
                            "innerHTML": summary.plugins,
                            "style" :"padding-right: 0.414723473%;"
                        }, li);
                        index++;
                    });
                    
                    
                    
                     
                }, 
                error: function(response, args) {
                    
                }
            });
        timer.setInterval(120000);
        timer.start();
    });

</script>
<style>
.span-40{
  width:32%;
}

.span-12{
  width:9.6%;
}
.span-8{
  width:6.4%;
}
.span-30{
  width:24%;
}
.span-20{
  width:16%;
}
.span-42{
  width:33.6%;
}
</style>