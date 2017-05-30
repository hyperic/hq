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
		
	<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/admin/managers/migration/migrationMgr.css"/>" />
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
		
		#showInstallUCAButton {
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
		
		#showStopAgentButton {
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
		
		#showDeleteAgentButton {
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
		#showDeployUCADetector {
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
		<h1>Migration Manager
			
		</h1> 
    <p id="instruction"></p>

    <div id="currentTimeInfo">
        <span style="float:left">
                <img id="serverIcon" alt="S" src='<spring:url value="/static/images/icon_hub_s.gif"/>'/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.server"/> &nbsp;
                <img id="customIcon" alt="D" src='<spring:url value="/static/images/icon_hub_c.gif"/>'/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.custom"/>&nbsp;
        </span> 
        
        <span style="float:right;" id="refreshTimeInfo"><fmt:message key="admin.managers.Plugin.information.refresh.time"/> <span id="timeNow"></span>
        </span>
        <span style="float:right;">&nbsp;&nbsp;</span>
        <img style="float:right;" id="refreshIcon" style="float:right;" src='<spring:url value="/static/images/arrow_refresh.png" />' alt="refresh" /> 
    </div>

       <div class="gridheader clear">
        <span class="first column span-1">&nbsp;</span>
        <span class="column span-1">&nbsp;</span>
        <span class="column span-small">FQDN</span>
        <span class="column span-small">IP Address</span>
        <span class="column span-med">Version</span>
        
    </div>
    
    <form:form id="migrationForm" name="migrationForm" onsubmit="return false;" method="migrate" >
    
    <ul id="pluginList">
        <li>&nbsp;</li><li>&nbsp;</li>
        
    </ul>
    </form:form>
    
    <c:if test="${mechanismOn}" >
<% if (iemode) { %>	
	<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
	<td>
<% } %>
	<div class="actionbar"> 			
		<input id="showStopAgentButton" type="button" value='Stop Agent'/>
		<input id="showDeleteAgentButton" type="button" value='Delete Agent' />
		
		<span id="progressMessage">&nbsp;</span>
		<input id="showInstallUCAButton" type="button" value='Install UCA' />
		<input id="showDeployUCADetector" type="button" value='Deploy UCA Detector'/>
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

<script src='<spring:url value="/static/js/admin/managers/migration/migrationMgr.js" />' type="text/javascript"></script> 
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
        
        hqDojo.connect(hqDojo.byId("showInstallUCAButton"), "onclick", function(e) {
            
            var xhrArgs = {
                preventCache:true,
                form: hqDojo.byId("migrationForm"),
                url: "<spring:url value='/app/admin/managers/migration/migrate' />",
                handle: function(response) {
                    if (response==="success") {
                        hqDojo.attr("progressMessage", "class", "information");
                        hqDojo.byId("progressMessage").innerHTML = 'Clicked success';
                        anim = [hqDojo.fadeIn({
                                    node: "progressMessage",
                                    duration: 500
                                }),
                                hqDojo.fadeOut({
                                    node: "progressMessage",
                                    delay: 5000,
                                    duration: 500
                                })];    
                    }else{
                        hqDojo.attr("progressMessage", "class", "error");
                        hqDojo.byId("progressMessage").innerHTML = 'Not clicked';
                        anim = [hqDojo.fadeIn({
                                    node: "progressMessage",
                                    duration: 500
                                }),
                                hqDojo.fadeOut({
                                    node: "progressMessage",
                                    delay: 10000,
                                    duration: 500
                                })];
                    }
                }
            };
            hqDojo.xhrPut(xhrArgs);
            
        });
        
		hqDojo.connect(hqDojo.byId("showDeployUCADetector"), "onclick", function(e) {
            
            var xhrArgs = {
                preventCache:true,
                form: hqDojo.byId("migrationForm"),
                url: "<spring:url value='/app/admin/managers/migration/deployTelegrafPlugin' />",
                handle: function(response) {
                    if (response==="success") {
                        hqDojo.attr("progressMessage", "class", "information");
                        hqDojo.byId("progressMessage").innerHTML = 'UCA plugin Deployed successfully';
                        anim = [hqDojo.fadeIn({
                                    node: "progressMessage",
                                    duration: 500
                                }),
                                hqDojo.fadeOut({
                                    node: "progressMessage",
                                    delay: 5000,
                                    duration: 500
                                })];    
                    }else{
                        hqDojo.attr("progressMessage", "class", "error");
                        hqDojo.byId("progressMessage").innerHTML = 'UCA plugin deployment failed';
                        anim = [hqDojo.fadeIn({
                                    node: "progressMessage",
                                    duration: 500
                                }),
                                hqDojo.fadeOut({
                                    node: "progressMessage",
                                    delay: 10000,
                                    duration: 500
                                })];
                    }
                }
            };
            hqDojo.xhrPut(xhrArgs);
            
        });
    });
</script>
