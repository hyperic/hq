<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/admin/managers/plugin/pluginMgr.css"/>" />

<section id="pluginManagerPanel" class="container top">
        <h1><fmt:message key="admin.managers.plugin.title" />
     <div id="solution">
      <span style="float:right">

    <a href="https://solutionexchange.vmware.com/store/category_groups/cloud-management?category=cloud-operations&nanosite_id=3&cloud_operations_ids%5b%5d=25&cloud_operations_ids%5b%5d=195&cloud_operations_ids%5b%5d=79&q" target="_blank">Download/Update Plugins</a> 
    </span>
    </div>
    </h1> 
    <p id="instruction"><fmt:message key="${instruction}" /></p>

    <div id="currentTimeInfo">
        <span style="float:left">
                <img id="serverIcon" alt="S" src="<spring:url value="/static/images/icon_hub_s.gif"/>"/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.server"/> &nbsp;
                <img id="customIcon" alt="D" src="<spring:url value="/static/images/icon_hub_c.gif"/>"/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.custom"/>&nbsp;
        </span> 
        
        <span style="float:right;" id="refreshTimeInfo"><fmt:message key="admin.managers.Plugin.information.refresh.time"/> <span id="timeNow"></span>
        </span>
        <span style="float:right;">&nbsp;&nbsp;</span>
        <img style="float:right;" id="refreshIcon" style="float:right;" src="<spring:url value="/static/images/arrow_refresh.png" />" alt="refresh" /> 
    </div>

    <div class="topInfo">
        <span id="agentFailure" style="float:right">
            <c:if test="${info.agentErrorCount>0}">
              (${info.agentErrorCount} <img src="<spring:url value="/static/images/icon_available_red.gif"/>"/>)
            </c:if>               
        </span> 
        
        <span id="agentInfo" style="float: right"> 
          <c:choose>
                <c:when test="${info.totalAgentCount>info.syncableAgentCount}">
                    <span id="unsynchableAgents"> 
                       <img id="warningIcon" alt="Warning. Click to view details." src="<spring:url value="/static/images/warning.png"/>" />&nbsp; 
                       <span id="agentInfoAllCount">${info.syncableAgentCount} / ${info.totalAgentCount}</span>&nbsp;&nbsp; 
                       <fmt:message key="admin.managers.Plugin.information.agent.count" />
                    </span>
                </c:when>
                <c:otherwise>
                    <span id="allAgentsSynched"> 
                       <span id="agentInfoAllCount">${info.syncableAgentCount} / ${info.totalAgentCount}</span>&nbsp;&nbsp;
                        <fmt:message key="admin.managers.Plugin.information.agent.count" />
                    </span>
                </c:otherwise>
          </c:choose>
        </span> 
        
        <span style="float:left">
                <img src="<spring:url value="/static/images/icon_available_green.gif"/>"/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.success"/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <img src="<spring:url value="/static/images/alert.png"/>"/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.in.progress"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <img src="<spring:url value="/static/images/icon_available_red.gif"/>"/> 
                        <fmt:message key="admin.managers.Plugin.tip.icon.error"/>&nbsp;
        </span> 
    </div>
    
    <div class="gridheader clear">
        <span class="first column span-1">&nbsp;</span>
        <span class="column span-1">&nbsp;</span>
        <span class="column span-small"><fmt:message key="admin.managers.plugin.column.header.product.plugin" /></span>
        <span class="column span-small"><fmt:message key="admin.managers.plugin.column.header.version" /></span>
        <span class="column span-med"><fmt:message key="admin.managers.plugin.column.header.jar.name" /></span>
        <span class="column span-med" id="addedTimeHeader"><fmt:message key="admin.managers.plugin.column.header.initial.deploy.date" /></span>
        <span class="column span-med" id="updatedTimeHeader"><fmt:message key="admin.managers.plugin.column.header.last.sync.date" /></span>
        <span class="column span-status"><fmt:message key="admin.managers.plugin.column.header.status" /></span>
    </div>
    
    <form:form id="deleteForm" name="deleteForm" onsubmit="return false;" method="delete">
    
    <ul id="pluginList">
        <li>&nbsp;</li><li>&nbsp;</li>
        <li>
            <img id="tableLoadingIcon" src="<spring:url value="/static/images/ajax-loader-blue.gif"/>"/>
        </li>
    </ul>
    </form:form>
    
    <c:if test="${mechanismOn}" >
        <div class="actionbar">     
            <input id="showRemoveConfirmationButton" type="button" value="<fmt:message key="admin.managers.plugin.button.remove.plugin" />" />
            <span id="progressMessage">&nbsp;</span>
            <input id="showUploadFormButton" type="button" value="<fmt:message key="admin.managers.plugin.button.add.plugin" />" />
        </div>  
    </c:if>
    
    <div>
        <span id="customDirInfo"><fmt:message key="admin.managers.Plugin.title.custom.directory" /> ${customDir}</span>
    </div>

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
        <span id="deletLoadingIcon" style="visibility:hidden;"><img src="<spring:url value="/static/images/ajax-loader-blue.gif"/>"/></span>

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
        <img id="loadingIcon" src="<spring:url value="/static/images/ajax-loader-blue.gif"/>" alt="loading"/>
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
    <img id="summaryLoadingIcon" src="<spring:url value="/static/images/ajax-loader-blue.gif"/>" alt="loading"/>
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
            src="<spring:url value="/static/images/ajax-loader-blue.gif"/>" alt="loading" />
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

<script src="<spring:url value="/static/js/admin/managers/plugin/pluginMgr.js" />" type="text/javascript"></script> 
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
        function refreshPage(focusElement){
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
                    hdDojo.byId().innerHTML=response.syncableAgentCount+"&nbsp;/&nbsp;"+response.totalAgentCount;
                    if(response.agentErrorCount>0){
                        hqDojo.byId("agentFailure").innerHTML="("+response.agentErrorCount+" <img src='<spring:url value="/static/images/icon_available_red.gif"/>'/>)";
                    }else{
                        hqDojo.byId("agentFailure").innerHTML="";
                    }
                }
            };
            
            var deleteIdsString = getIdList(hqDojo.query("input[type=checkbox]:checked"));

            var hashObj = {
                deleteIds: deleteIdsString
            };
            hqDojo.hash(hqDojo.objectToQuery(hashObj));
            
            hqDojo.xhrGet(infoXhrArgs);
            if(focusElement!==undefined && typeof(focusElement)!=="number"){
                refreshDataGrid(focusElement);
            }else{
                refreshDataGrid();
            }
        }

        function resizePluginMgrContentHeight(){
            resizeContentHeight(hqDojo.byId("pluginList"),150,400);
        }
        refreshDataGrid();//load the plugin list
        uncheckCheckboxes(hqDojo.query("input[type=checkbox]"));
        refreshTime(hqDojo.byId("timeNow"),"refreshTimeInfo","#EEEEEE");
        resizePluginMgrContentHeight();
        
        hqDojo.connect(window, "onresize", resizePluginMgrContentHeight);
        
        hqDojo.connect(hqDojo.byId("refreshIcon"),"onclick",function(e){
            refreshPage();
        });
        new hqDijit.Tooltip({
            connectId:["serverIcon"],
            position:["left"],
            label: "<fmt:message key='admin.managers.Plugin.tip.server.plugin' />"
        });
        new hqDijit.Tooltip({
            connectId:["defaultIcon"],
            position:["left"],
            label: "<fmt:message key='admin.managers.Plugin.tip.default.plugin' />"
        }); 
        new hqDijit.Tooltip({
            connectId:["customIcon"],
            position:["left"],
            label: "<fmt:message key='admin.managers.Plugin.tip.custom.plugin' />" 
        });
        new hqDijit.Tooltip({
            connectId:["addedTimeHeader"],
            position:["left"],
            label: "<fmt:message key='admin.managers.plugin.column.header.initial.deploy.date.tip' />"
        });
        
        new hqDijit.Tooltip({
            connectId:["updatedTimeHeader"],
            position:["left"],
            label: "<fmt:message key='admin.managers.plugin.column.header.last.sync.date.tip' />"
        });
        
        new hqDijit.Tooltip({
            connectId:["allAgentsSynched"],
            label: "<fmt:message key='admin.managers.Plugin.information.agent.count.tip' />"
        });
        
        
        new hqDijit.Tooltip({
            connectId:["customDirInfo"],
            label: "<fmt:message key='admin.managers.Plugin.tip.custom.directory' />"
        });

        function seeStatusDetail(pluginId,status){
            hqDijit.byId("showStatusPanelDialog").show();
            var agentListUl = hqDojo.byId("agentList");
            var searchWord = hqDojo.byId("searchText").value;
            
            var statusDetailUrl = "/app/admin/managers/plugin/status/{pluginId}?searchWord={keyword}&status={status}";//"<c:url value='/app/admin/managers/plugin/status/{pluginId}?searchWord={keyword}'/>";
            
            statusDetailUrl = statusDetailUrl.replace("{pluginId}",pluginId);
            statusDetailUrl = statusDetailUrl.replace("{keyword}",searchWord);
            statusDetailUrl = statusDetailUrl.replace("{status}",status);
            
            var xhrArgs = {
                    preventCache:true,
                    url: statusDetailUrl,
                    load: function(response) {
                        hqDojo.empty("agentList");
                        hqDojo.style(hqDojo.byId("loadingIcon"),"visibility","hidden");
                        hqDojo.forEach(response, function(agentStatus) {
                            var statusLi = hqDojo.create("li",{
                                "innerHTML":agentStatus.agentName
                            });
                            if(agentStatus.syncDate===""){
                                statusLi.innerHTML+=" <fmt:message key="admin.managers.Plugin.tip.status.no.sync" />";
                            }else{
                                statusLi.innerHTML+=" <fmt:message key="admin.managers.Plugin.tip.status.sync.date" /> "+agentStatus.syncDate;
                            }
                            if(agentStatus.status==="error"){
                                hqDojo.addClass(statusLi,"errorAgent");
                            }else{
                                hqDojo.addClass(statusLi,"inProgressAgent");
                            }
                            agentListUl.appendChild(statusLi);
                        });
                    },
                    handleAs: "json",
                    headers: { 
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    }
            };
            if (status === "error"){
                hqDojo.style(hqDojo.byId("agentStatusHelp"), "visibility", "visible");
            }else if (status === "inprogress"){
                hqDojo.style(hqDojo.byId("agentStatusHelp"), "visibility", "hidden");
            }
            
            hqDojo.xhrGet(xhrArgs);         
            hqDojo.byId("showStatusPanelDialog_title").innerHTML=hqDojo.byId("pluginName").value + "&nbsp;-&nbsp;"+ "<fmt:message key="admin.managers.Plugin.tip.status.title" />";
        }
        
        function seeAgentSummary(){
            hqDojo.style("summaryLoadingIcon","display","block");
            hqDijit.byId("agentSummaryPanelDialog").show();
        
            var agentUl = hqDojo.byId("agentSummaryList");
            var xhrArgs = {
                    preventCache:true,
                    url: "<spring:url value='/app/admin/managers/plugin/agent/summary'/>",
                    load: function(response) {
                        hqDojo.forEach(response, function(agentName){
                            var li = hqDojo.create("li",{
                                "innerHTML":agentName
                            });         
                            agentUl.appendChild(li);
                        });
                        hqDojo.style("summaryLoadingIcon","display","none");
                    },
                    handleAs: "json",
                    headers: { 
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    }   
                    
            };
            hqDojo.xhrGet(xhrArgs);
        }

        
        function buildAgentNameVersionXhrArgs(url, agentUl) {
        	return {
                preventCache:true,
                url: url,
                load: function(response) {                      
                    hqDojo.forEach(response, function(versionMap){
                        var li = hqDojo.create("li", {
                            "class":"gridrow clear"
                        },agentUl);    
                        var agentVersion = versionMap.version;
                        var agentName = versionMap.agentName;       
                        hqDojo.create("span", {
                            "class": "column span-large",
                            "innerHTML":agentName 
                        },li);     
                        hqDojo.create("span", {
                            "class": "column span-med",
                            "innerHTML": agentVersion
                        },li);                                                                 
                    });
                    hqDojo.style("oldAgentSummaryLoadingIcon","display","none");
                },
                handleAs: "json",
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json"      		
                }
        	};
        }
        
        function seeOldAgentSummary(){
            hqDojo.style("oldAgentSummaryLoadingIcon","display","block");
            hqDijit.byId("unsyncAgentsSummaryPanelDialog").show();
            
            var agentUl = hqDojo.byId("oldAgentSummaryList");
            var xhrArgs = buildAgentNameVersionXhrArgs("<spring:url value='/app/admin/managers/plugin/agent/old/summary'/>", agentUl);
            hqDojo.xhrGet(xhrArgs);
        }

        function seeCurrentNonSyncAgentStatusSummary(){
            hqDojo.style("oldAgentSummaryLoadingIcon","display","block");
            hqDijit.byId("unsyncAgentsSummaryPanelDialog").show();
            
            var agentUl = hqDojo.byId("currentNonSyncAgentSummaryList");
            var xhrArgs = buildAgentNameVersionXhrArgs("<spring:url value='/app/admin/managers/plugin/agent/unsynchable/cur/summary'/>", agentUl);
            hqDojo.xhrGet(xhrArgs);
        }

        var showStatusDialog = new hqDijit.Dialog({
            id: "showStatusPanelDialog"
        });
        
        var showStatusPanel = hqDojo.byId("showStatusPanel");
        hqDojo.style(showStatusDialog.closeButtonNode,"visibility", "hidden" );
        showStatusDialog.setContent(showStatusPanel);
        hqDojo.style(showStatusPanel, "visibility", "visible");
        var showStatusPanelContent = hqDojo.query("#showStatusPanelDialog .dijitDialogPaneContent")[0];

        hqDojo.create("span",{
                             "class":"helpLink",
                             "id":"agentStatusHelp",
                             "innerHTML":"<fmt:message key="admin.managers.Plugin.troubleshooting"/>"
                         },showStatusPanelContent); 
        
        hqDojo.connect(hqDojo.byId("searchText"),"onkeyup",function(e){
            var pluginId = hqDojo.byId("pluginId").value;
            var status = hqDojo.byId("status").value;
            seeStatusDetail(pluginId,status);           
        });
        
        var agentSummaryDialog = new hqDijit.Dialog({
            id:"agentSummaryPanelDialog",
            title: "<fmt:message key="admin.managers.Plugin.summary.title" />"
        });
        var agentSummaryPanel = hqDojo.byId("agentSummaryPanel");
        hqDojo.style(agentSummaryDialog.closeButtonNode,"visibility","hidden");
        agentSummaryDialog.setContent(agentSummaryPanel);
        
        var summaryContent = hqDojo.query("#agentSummaryPanelDialog .dijitDialogPaneContent")[0];
        hqDojo.create("span",{
                             "class":"helpLink",
                             "id":"agentSummaryHelp",
                             "innerHTML":"<fmt:message key="admin.managers.Plugin.troubleshooting"/>"
                         },summaryContent); 
        
        hqDojo.style(agentSummaryPanel,"visibility","visible");
        hqDojo.query("#agentSummaryPanelDialog .cancelLink").onclick(function(e){
            hqDijit.byId("agentSummaryPanelDialog").hide();
            hqDojo.empty("agentSummaryList");
        });
        
        
          
        var oldAgentSummaryDialog = new hqDijit.Dialog({
            id:"unsyncAgentsSummaryPanelDialog",
            title: "<fmt:message key="admin.managers.Plugin.summary.unsynchable.agents.title" />"
        }); 
        var unsyncAgentsSummaryPanel = hqDojo.byId("unsyncAgentsSummaryPanel");
        hqDojo.style(oldAgentSummaryDialog.closeButtonNode,"visibility","hidden");
        oldAgentSummaryDialog.setContent(unsyncAgentsSummaryPanel);
        
        var oldAgentSummaryContent = hqDojo.query("#unsyncAgentsSummaryPanelDialog .dijitDialogPaneContent")[0];
        hqDojo.create("span",{
                             "class":"helpLink",
                             "id":"oldAgentSummaryHelp",
                             "innerHTML":"<fmt:message key="admin.managers.Plugin.troubleshooting"/>"
                         },oldAgentSummaryContent); 
        
        hqDojo.style(unsyncAgentsSummaryPanel,"visibility","visible");
        hqDojo.query("#unsyncAgentsSummaryPanelDialog .cancelLink").onclick(function(e){
            hqDijit.byId("unsyncAgentsSummaryPanelDialog").hide();
            hqDojo.empty("oldAgentSummaryList");
            hqDojo.empty("currentNonSyncAgentSummaryList");
        });        

        hqDojo.behavior.add({
            ".agentStatusProgressSpan":{
                onclick: function(evt){
                    var anchor = evt.target.id.indexOf("_");
                    var pluginName = evt.target.id.substr(anchor+1,evt.target.id.length);
                    var pluginId = evt.target.id.substr(0,anchor);
                    hqDojo.byId("pluginName").value=pluginName;
                    hqDojo.byId("pluginId").value=pluginId;
                    hqDojo.byId("status").value="inprogress";
                    seeStatusDetail(pluginId,"inprogress");
                }
            },
            ".agentStatusFailSpan":{
                onclick: function(evt){
                    var anchor = evt.target.id.indexOf("_");
                    var pluginName = evt.target.id.substr(anchor+1,evt.target.id.length);
                    var pluginId = evt.target.id.substr(0,anchor);
                    hqDojo.byId("pluginName").value=pluginName;
                    hqDojo.byId("pluginId").value=pluginId;
                    hqDojo.byId("status").value="error";
                    seeStatusDetail(pluginId,"error");
                }
            },
            "#agentFailure":{
                onclick: function(evt){
                    seeAgentSummary();
                }
            },
            "#unsynchableAgents":{
                onclick: function(evt){
                    seeOldAgentSummary();
                    seeCurrentNonSyncAgentStatusSummary();
                }
            },          
            "#agentStatusHelp":{
                onclick: function(e){
                     var helpHref = "<hq:help key='.Troubleshooting'/>";
                     var helpWin=window.open(helpHref,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');
                     helpWin.focus();
                }
            },
            "#agentSummaryHelp":{
                onclick: function(e){
                    var helpHref = "<hq:help key='.Troubleshooting'/>";
                    var helpWin=window.open(helpHref,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');
                    helpWin.focus();
                }
            },
            "#oldAgentSummaryHelp":{
                onclick: function(e){
                    var helpHref = "<hq:help key='.Troubleshooting'/>";
                    var helpWin=window.open(helpHref,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');
                    helpWin.focus();
                }
            },          
            "#showStatusPanelDialog .cancelLink":{
                onclick: function(e){
                    hqDijit.byId("showStatusPanelDialog").hide();
                    hqDojo.empty("agentList");
                    hqDojo.byId("searchText").value="";
                }
            }
        });

        hqDojo.behavior.apply();
        
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
            var errorMsgPanelDialog = new hqDijit.Dialog({
                id: "errorMsgPanelDialog",
                title: "<fmt:message key="admin.managers.Plugin.remove.error.dialog.title" />"
            });
            
            var uploadPanel = hqDojo.byId("uploadPanel");
            var confirmationPanel = hqDojo.byId("confirmationPanel");
            var errorMsgPanel = hqDojo.byId("errorMsgPanel");
            var uploader;
        
            hqDojo.style(uploadDialog.closeButtonNode, "visibility", "hidden");
            hqDojo.style(removeDialog.closeButtonNode, "visibility", "hidden");
            hqDojo.style(errorMsgPanelDialog.closeButtonNode,"visibility", "hidden" );
            uploadDialog.setContent(uploadPanel);
            removeDialog.setContent(confirmationPanel);
            errorMsgPanelDialog.setContent(errorMsgPanel);
        
            hqDojo.style(uploadPanel, "visibility", "visible");
            hqDojo.style(confirmationPanel, "visibility", "visible");
            hqDojo.style(errorMsgPanel, "visibility", "visible");

            hqDojo.query("#uploadPanelDialog .cancelLink").onclick(function(e) {
                uncheckCheckboxes(hqDojo.query("input[type=checkbox]"));
                hqDijit.byId("uploadPanelDialog").hide();

            });

            hqDojo.query("#removePanelDialog .cancelLink").onclick(function(e) {
                uncheckCheckboxes(hqDojo.query("input[type=checkbox]"));
                hqDijit.byId("removePanelDialog").hide();
            });
            hqDojo.query("#errorMsgPanelDialog .cancelLink").onclick(function(e) {
                hqDijit.byId("errorMsgPanelDialog").hide();
            });
        
            hqDojo.connect(hqDojo.byId("showUploadFormButton"), "onclick", function(e) {
                
                hqDijit.registry.filter(function(e){return e.id==="selectFileButton";}).forEach(function(entry){
                        hqDijit.registry.remove("selectFileButton");
                });             
                hqDojo.query("div > #selectFileButton").forEach(function(e){
                    hqDojo.removeAttr(e,"class");
                    hqDojo.attr(e,"class","selectFileBtn");
                    hqDojo.removeAttr(e,"wigetid");
                    hqDojo.removeAttr(e,"style");
                });
                hqDojo.byId("selectFileButton").innerHTML = "<fmt:message key='admin.managers.plugin.button.select.files' />";
                hqDojo.byId("selectedFileList").innerHTML = "";
                uploader = new hqDojox.form.FileUploader({
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
            
                hqDojo.connect(uploader, "onComplete", function(dataArray){
                    if (dataArray[0].success){
                        hqDojo.byId("progressMessage").innerHTML=dataArray[0].message;
                        hqDojo.attr("progressMessage", "class", "information");
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
                    } else {
                        hqDojo.byId("errorMsg").innerHTML=dataArray[0].message;
                        hqDijit.byId("errorMsgPanelDialog").show();
                    }                   
                });
                hqDijit.byId("uploadPanelDialog").show();
            });
            
            hqDojo.connect(hqDojo.byId("uploadButton"), "onclick", function(e){
                var fileTypeCorrect=true;
                var pluginList = hqDojo.query("input[type=file]", hqDojo.byId("selectFileButton"));
                var newPluginList = pluginList.slice(0,pluginList.length-1);//To get rid of last item which is empty
                if(newPluginList.length<1){
                    showErrorMessage("validationMessage","<fmt:message key='admin.managers.plugin.message.invalid.no.file' />")
                    return; //no file selected, do nothing
                }
                
                uncheckCheckboxes(hqDojo.query("input[type=checkbox]"));
                newPluginList.forEach(function(input) {
                    //check file type
                    var filePath = input.value;
                    if(!checkFileType(filePath,"validationMessage","<fmt:message key='admin.managers.plugin.message.invalid.file.extension' />")){
                        fileTypeCorrect=false;
                    }
                    //change name, for backend!
                    hqDojo.attr(input, "name", "plugins");
                });
                if(fileTypeCorrect){
                    hqDijit.byId("uploadPanelDialog").hide();
                    uploader.upload();
                }
            });
            
            hqDojo.connect(hqDojo.byId("showRemoveConfirmationButton"), "onclick", function(e) {
                var checkedPlugins = hqDojo.filter(hqDojo.query("input[type=checkbox]"), function(e){ return e.checked; });
                hqDojo.empty("removeList");
                
                if(checkedPlugins.length>0){
                    hqDojo.forEach(checkedPlugins,function(checkedPlugin){
                        var pluginName = checkedPlugin.value.split("_")[1];
                        var pluginId = checkedPlugin.value.split("_")[0];
                        if (pluginName!=="undefined"){
                            hqDojo.create("li", {
                                "innerHTML":pluginName,
                                "id":"remove_"+pluginId
                            }, "removeList");
                        }
                    });
                    showResourceCountForEachPlugin(checkedPlugins);
                    hqDijit.byId("removePanelDialog").show();
                }else{
                    hqDojo.byId("errorMsg").innerHTML='<fmt:message key="admin.managers.Plugin.remove.error.dialog.empty" />';
                    hqDijit.byId("errorMsgPanelDialog").show();
                }
            });
            
            function showResourceCountForEachPlugin(checkedPlugins){
                if(checkedPlugins!==undefined && checkedPlugins.length>0){
                    var pluginIds="";
                    hqDojo.forEach(checkedPlugins, function(checkedPlugin){
                        pluginIds +=  checkedPlugin.value.split("_")[0]+",";
                    });
                    pluginIds = pluginIds.substr(0,pluginIds.length-1);//get rid of the last ,
                    resourceCountUrl = "<spring:url value='/app/admin/managers/plugin/resource/count?deleteIds={pluginIds}' />";
                    resourceCountUrl = resourceCountUrl.replace("{pluginIds}",pluginIds);
                    var xhrArgs = {
                        preventCache:true,
                        url: resourceCountUrl,
                        handleAs: "json",
                        headers: { 
                            "Content-Type": "application/json",
                            "Accept": "application/json"
                        },
                        load: function(response) {
                            var pluginList = pluginIds.split(",");
                            hqDojo.forEach(response,function(pluginCount){
                                if(hqDojo.byId("remove_"+pluginCount.pluginId)!==null){
                                    hqDojo.byId("remove_"+pluginCount.pluginId).innerHTML+=" - "+pluginCount.count+" <fmt:message key="admin.managers.plugin.confirmation.delete.resource" />";                             
                                }
                            });
                        },
                        error: function(response){}
                    };
                    
                    hqDojo.xhrGet(xhrArgs);
                } 
            }

            hqDojo.connect(hqDojo.byId("removeButton"), "onclick", function(e) {
                var checkedPlugins = hqDojo.filter(hqDojo.query("input[type=checkbox]"), function(e){ return e.checked; });
                hqDojo.forEach(checkedPlugins,function(checkedPlugin){
                    checkedPlugin.value=checkedPlugin.value.split("_")[0];
                });
                var xhrArgs = {
                    preventCache:true,
                    form: hqDojo.byId("deleteForm"),
                    url: "<spring:url value='/app/admin/managers/plugin/delete' />",
                    handle: function(response) {
                        var anim;
                        if (response==="success") {
                            hqDojo.attr("progressMessage", "class", "information");
                            hqDojo.byId("progressMessage").innerHTML = '<fmt:message key="admin.managers.plugin.message.remove.success" />';
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
                            hqDojo.byId("progressMessage").innerHTML = '<fmt:message key="admin.managers.Plugin.remove.error.dialog.failure" />';
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
                        refreshPage("row_"+ checkedPlugins[0].value);
                        hqDijit.byId("removePanelDialog").hide();
    
                        hqDojo.style("deletLoadingIcon","visibility","hidden");
                        hqDojo.style("removeButton","visibility","visible");
                        hqDojo.style("removeCancel","visibility","visible");
                        hqDojo.fx.chain(anim).play();
                    }
                };
                hqDojo.xhrPost(xhrArgs);
                hqDojo.style("deletLoadingIcon","visibility","visible");
                hqDojo.style("removeButton","visibility","hidden");
                hqDojo.style("removeCancel","visibility","hidden");
            });
        }
        
        function refreshDataGrid(focusElement) {
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
                    refreshTime(hqDojo.byId("timeNow"),"refreshTimeInfo","#EEEEEE");
                    timer.stop();
                    timer.start();
                    hqDojo.empty("pluginList");
                    var index = 1;
                    
                    hqDojo.forEach(response, function(summary) {
                        var liClass = "";
                        var version;
                        if(summary.deleted){
                            liClass = " grey";
                        }else{
                            liClass=(((index) % 2 === 0) ? " even" : "");
                        }
                        var li = hqDojo.create("li", {
                            "class": "gridrow clear" + liClass 
                        }, "pluginList");
                        var span = hqDojo.create("span", {
                            "class": "first column span-1"
                        }, li);
                        if(${mechanismOn} && (!summary.deleted) && (!summary.inProgress)&& (!summary.isServerPlugin)){
                            hqDojo.create("input", {
                                "type": "checkbox",
                                "name": "deleteId",
                                "value": summary.id+"_"+summary.jarName+" ("+summary.name+")"
                            }, span);
                        }
                        var typeIcon = hqDojo.create("span", {
                            "class": "column span-1"
                        }, li);
                        if(summary.isCustomPlugin){
                            hqDojo.create("img",{
                                "src": "<spring:url value="/static/images/icon_hub_c.gif"/>",
                                "alt": "C",
                                "style":"padding:3px 0px 0px 0px;"
                                }, typeIcon);
                            typeIcon.innerHTML+="&nbsp;";
                        }
                        if(summary.isServerPlugin){
                            hqDojo.create("img",{
                                "src": "<spring:url value="/static/images/icon_hub_s.gif"/>",
                                "alt": "S",
                                "style":"padding:3px 0px 0px 0px;"
                                }, typeIcon);
                            typeIcon.innerHTML+="&nbsp;";
                        }
                        
                        var pluginName = hqDojo.create("span", {
                            "class": "column span-small",
                            "innerHTML": summary.name,
                            "id": "row_"+summary.id
                        }, li);
                        if(summary.deleted){
                            span = hqDojo.create("span",{
                                "class":"deleting",
                                "innerHTML":"<br/><fmt:message key='admin.managers.Plugin.column.plugin.deleting'/>"
                            },pluginName);
                        }
                        if(summary.version===null){
                            version = "";
                        }else{
                            version = summary.version;
                        }
                        span = hqDojo.create("span", {
                            "class": "column span-small",
                            "innerHTML": version
                        }, li);
                        var spanName = hqDojo.create("span", {
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
                            if(summary.successAgentCount > 0){
                                var successfulAgentSpan = hqDojo.create("div",{
                                    "class":"agentStatusSuccessfulSpan"
                                },statusSpan);
                                successfulAgentSpan.innerHTML+=summary.successAgentCount+"&nbsp;";
                                hqDojo.create("img",{
                                    "src": "<spring:url value="/static/images/icon_available_green.gif"/>",
                                    "alt": "successful",
                                    "class": "successIcon"
                                }, successfulAgentSpan); 
                                successfulAgentSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                            }
                            var statusId = summary.id+"_"+summary.name;
                            if (summary.inProgressAgentCount > 0) {
                                var inProgressAgentSpan = hqDojo.create("div",{
                                    "id":statusId,
                                    "class":"agentStatusProgressSpan"
                                    }, statusSpan);
                            
                                inProgressAgentSpan.innerHTML+=summary.inProgressAgentCount+"&nbsp;";
                                hqDojo.create("img",{
                                    "src": "<spring:url value="/static/images/clock.gif"/>",
                                    "alt": "in progress",
                                    "class": "inProgressIcon",
                                    "id":statusId
                                }, inProgressAgentSpan);
                                inProgressAgentSpan.innerHTML+="&nbsp;&nbsp;&nbsp;";
                            }   
                                
                            if (summary.errorAgentCount > 0) {
                                var errorAgentSpan = hqDojo.create("div",{
                                    "id":statusId,
                                    "class":"agentStatusFailSpan"
                                    }, statusSpan);
                                
                                errorAgentSpan.innerHTML+= summary.errorAgentCount+"&nbsp;";
                                hqDojo.create("img",{
                                    "src": "<spring:url value="/images/icon_available_red.gif"/>",
                                    "alt": "failure",
                                    "class": "errorIcon",
                                    "id":statusId
                                    }, errorAgentSpan);
                                errorAgentSpan.innerHTML+="</img>";
                            }
                        }
                        index++;
                    });
                    
                    var hashObj = hqDojo.queryToObject(hqDojo.hash());
                    if(hashObj.deleteIds!==undefined && hashObj.deleteIds!==""){
                        hqDojo.forEach(hashObj.deleteIds.split(","),function(pluginId){
                            var checkbox = hqDojo.query("input[value='"+pluginId+"']");
                            if(checkbox[0]!==undefined){
                                checkbox[0].checked="true";
                            }
                        });
                    }
                    if(focusElement!==undefined){
                        hqDijit.scrollIntoView(focusElement);
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
                            label: "<fmt:message key='admin.managers.Plugin.tip.icon.in.progress' />. <fmt:message key='admin.managers.Plugin.tip.icon.click' />"
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
                            label: "<fmt:message key='admin.managers.Plugin.tip.icon.error' />. <fmt:message key='admin.managers.Plugin.tip.icon.click' />"
                        });     
                    }); 
                }, 
                error: function(response, args) {
                    
                }
            });
        }
        timer.setInterval(120000);
        timer.onTick = refreshPage;
        timer.start();
    });

</script>