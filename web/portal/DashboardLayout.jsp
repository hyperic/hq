<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
  This file is part of HQ.
  
  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>

<script src='<html:rewrite page="/js/dash.js"/>' type="text/javascript"></script>
<script src='<html:rewrite page="/js/scriptaculous.js"/>' type="text/javascript"></script>
<script src="<html:rewrite page='/js/requests.js'/>" type="text/javascript" id="requests"></script>
<script src='<html:rewrite page="/js/dashboard.js"/>' type="text/javascript"></script>
<script src='<html:rewrite page="/js/effects.js"/>' type="text/javascript"></script>

<script type="text/javascript">
if (top != self){
    top.location.href = self.document.location;
}
autoLogout = false;

function removePortlet(name, label) {
    dojo.xhrGet({
        url: '<html:rewrite page="/dashboard/RemovePortlet.do"/>',
        load: function(){postRemovet(name,label)}
    });
}
  
function postRemovet(name, label){
    new Effect.BlindUp(dojo.byId(name));
    var wide = isWide(name);
    if (!wide && !isNarrow(name)) {
        return;
    }
    var portletOptions;
    for (i = 0; i < document.forms.length; i++) {
      if (document.forms[i].wide) {
        if (wide == (document.forms[i].wide.value == 'true')) {
            portletOptions = document.forms[i].portlet.options;
            break;
        }
      }
    }
    if (portletOptions) {
        // Make sure that we are not re-inserting
        for (var i = 0; i < portletOptions.length; i++) {
            if (portletOptions[i].value == name) {
                return;
            }
        }
        portletOptions[portletOptions.length] = new Option(label, name);

        // Make sure div is visible
        dojo.byId('addContentsPortlet' + wide).style.visibility='visible';
    }
}

function refreshPortlets() {

    var problemPortlet = dojo.byId('problemResourcesTable');
    var favoritePortlet = dojo.byId('favoriteTable');

    var nodes = document.getElementsByTagName('table');
    var getRecentForm = document.getElementsByTagName('form')

    for (i = 0; i < nodes.length; i++) {
        if (/metricTable/.test(nodes[i].id)) {
            //alert('in metric table')
            var metricTblId = nodes[i].id;
            var getId = metricTblId.split('_');
            var metricIdPart = getId[1];

            if (metricIdPart) {
                var metricIdToken = '_' + metricIdPart;

                setInterval("requestMetricsResponse" + metricIdToken + "()", 30000);
            } else {
                setInterval("requestMetricsResponse()", 30000);
            }
        }
    }


    for (i = 0; i < nodes.length; i++) {
        if (/availTable/.test(nodes[i].id)) {
            // alert('in avail table')
            var availTblId = nodes[i].id;
            var getId = availTblId.split('_');
            var availIdPart = getId[1];

            if (availIdPart) {
                var availIdToken = '_' + availIdPart;

                setInterval("requestAvailSummary" + availIdToken + "()", 30000);
            } else {
                setInterval("requestAvailSummary()", 30000);
            }
        }
    }

    for (i = 0; i < getRecentForm.length; i++) {

        if (/RemoveAlerts/.test(getRecentForm[i].action)) {
            for (i = 0; i < nodes.length; i++) {
                if (/recentAlertsTable/.test(nodes[i].id)) {
                     //alert('in recent alerts table')
                    var alertTblId = nodes[i].id;
                    var getId = alertTblId.split('_');
                    var alertIdPart = getId[1];

                    var alertIdToken = '';
                    if (alertIdPart) {
                        var alertIdToken = '_' + alertIdPart;
                    }

                    setInterval("requestRecentAlerts" + alertIdToken + "()", 30000);
                }
            }
        }
    }


    if (problemPortlet) {
        setInterval("requestProblemResponse()", 30000);
    }

    if (favoritePortlet) {
        setInterval("requestFavoriteResources()", 30000);
    }
}

function fixSelect(){
    dojo.byId("dashSelect").value = '<c:out value="${DashboardForm.selectedDashboardId}"/>';
}
onloads.push(refreshPortlets);
dojo.require("dojo.widget.Dialog");
    dojo.event.connect(window, "onload", function(){
    <c:if test="${DashboardForm.dashboardSelectable}">
        var dialogWidget = dojo.widget.createWidget("Dialog", {}, dojo.byId("dashboardSelectDialog"));
    if(<c:out value="${DashboardForm.popDialog}"/>){
       dialogWidget.startup();
    }
    fixSelect();
 </c:if>
});

</script>
<html:link page="/Resource.do?eid=" linkName="viewResUrl" styleId="viewResUrl" style="display:none;"></html:link>

<%
  String divStart;
  String divEnd;
  String narrowWidth;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE")) {
    divStart = ""; 
    divEnd = "";
    narrowWidth = "width='25%'";
  }
  else {
    divStart = "<div style=\"margin:0px;\">";
    divEnd = "</div>";
    narrowWidth = "width='100%'";
  }
%>
<c:choose>
<c:when test="${portal.columns ne null}">
<c:set var="headerColspan" value="${portal.columns + 3}"/>
</c:when>
<c:otherwise>
<c:set var="headerColspan" value="${5}"/>
</c:otherwise>
</c:choose>


<div class="effectsContainer ">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="<c:out value="${headerColspan}"/>"><tiles:insert page="/portal/DashboardHeader.jsp"/></td>
  </tr>
  <tr> <!-- Role based config dashboard area -->
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
    <td class="rowSpanLeft"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td colspan="2">
  <c:choose>
  <c:when test="${DashboardForm.dashboardSelectable}">
  <html:form method="post" action="/SetDashboard.do" styleId="DashboardForm">
        <div class="messagePanel dashboard">
	        <span style="font-weight: bold; margin-right: 4px;"><fmt:message key="dash.home.SelectDashboard"/></span>
	        <html:select property="selectedDashboardId" name="selectedDashboardId" value="selectedDashboardId" onchange="changeDashboard('DashboardForm');" styleId="dashSelect">
	            <html:optionsCollection property="dashboards" value="id" label="name"></html:optionsCollection>
	        </html:select>
	        <input type="hidden" name="defaultDashboard" id="defaultDashboard"/>
	        <div class="message" style="margin-left:10px;display:inline;">
	        <c:choose>
	        <c:when test="${not sessionScope.modifyDashboard}">
	           <span style="font-weight:bold;"><fmt:message key="note"/></span>&nbsp;<span><fmt:message key="dash.home.ReadOnlyMessage"/></span>
	        </c:when>
	        <c:when test="${sessionScope.modifyDashboard and requestScope.roleDashboard}">
	           <span style="font-weight:bold;"><fmt:message key="note"/></span>&nbsp;<span><fmt:message key="dash.home.ModifyRoleDashboardMessage"/></span>
	        </c:when>
	        </c:choose>
	        </div>
        </div>
        <div id="dashboardSelectDialog" class="hidden">
		    <div class="dojoDialog">
		        <div class="dojoDialogBody">
		            <c:if test="${requestScope.isDashRemoved}">
		              <div class="dojoDialogMessage"><fmt:message key="dash.home.DefalutDashboardRemoved"/></div>
		            </c:if>
		            <div id="dashboardSelectionErrorPanel" class="hidden">
		                <span class="ErrorBlock"><img width="10" height="11" border="0" alt="" src="/images/tt_error.gif"/></span>
		                <span class="ErrorBlock"><fmt:message key="dash.home.DashboardSelectionDialogError"/></span>            
		            </div>
		            <div class="fieldSetStacked" style="margin-bottom:8px;">
		                <span class="DashboardSelectBoxLabel"><strong><fmt:message key="dash.home.DashboardSelectBoxLabel"/></strong></span>
		                <html:select property="defaultDashboard" name="defaultDashboard" value="defaultDashboard" size="9" style="width:288px;" styleId="defaultDash">
                            <html:optionsCollection property="dashboards" value="id" label="name"></html:optionsCollection>
                        </html:select>
		            </div>
		        </div>
		        <div class="dojoDialogFooter">
		           <div class="right">
		            <html:image property="ok" styleId="selectDashboard" src="/images/fb_ok.gif" onmouseout="javscript:this.src='/images/fb_ok.gif'" onmouseover="javscript:this.src='/images/fb_ok_over.gif'" onmousedown="javascript:this.src='/images/fb_ok_down.gif'" onclick="javascript:selectDefaultDashboard('defaultDash', 'DashboardForm');"></html:image>
		           </div>
		        </div>
		    </div>
	  </div>
	  </html:form>
    </c:when>
    <c:otherwise>
    	<div style="margin: 16px 0px;"></div>
    </c:otherwise>
    </c:choose>
    </td>
    <td class="rowSpanRight"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
  </tr>
  <tr>
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
      <td class="rowSpanLeft"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
<%-- Multi-columns Layout
  This layout render lists of tiles in multi-columns. Each column renders its tiles
  vertically stacked.  
--%>
<c:set var="narrow" value="true" scope="page" />
<c:set var="width"  value=""     scope="page" />
<c:set var="hr"     value=""     scope="page" />
<c:set var="showUpAndDown" value="true" scope="request"/>

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >  
	  <c:choose>    
	    <c:when test="${portal.columns eq 1}">    
	      <c:set var="narrow" value="false" />
	      <c:set var="hr" value="95%" />
	      <c:set var="width" value="width='100%'" />
	    </c:when>
	  
	    <c:otherwise>
	      <c:set var="hr" value="50%" />
	      <c:set var="width" value="width='50%'" />
	    </c:otherwise>
	  </c:choose>
	
	  <td valign="top" name="specialTd" <c:out value="${width}" escapeXml="false"/>>      
	    <%= divStart %>
	
	<ul id="<c:out value="narrowList_${narrow}"/>" class="boxy">
	  <c:forEach var="portlet" items="${columnsList}">
	  <c:set var="isFirstPortlet" value="${portlet.isFirst}" scope="request"/>
	  <c:set var="isLastPortlet"  value="${portlet.isLast}"  scope="request"/>
	  <li id="<c:out value="${portlet.fullUrl}"/>">
	    <div class="DashboardPadding">
	    <tiles:insert beanProperty="url" beanName="portlet" flush="true">
	      <tiles:put name="portlet" beanName="portlet"/>
	    </tiles:insert>
	    </div>
	  </li>
	  </c:forEach>
	</ul>
	<c:if test="${sessionScope.modifyDashboard}">
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
	  <tr><td valign="top" class="DashboardPadding">
	  <c:choose>
	  <c:when test="${narrow eq 'true'}">      
	    <tiles:insert name=".dashContent.addContent.narrow" flush="true"/>
	  </c:when>
	  <c:otherwise>
	    <tiles:insert name=".dashContent.addContent.wide" flush="true"/>
	  </c:otherwise>
	  </c:choose>
	  </td></tr>
	</table>
	      <script type="text/javascript">
	        Sortable.create("<c:out value="narrowList_${narrow}"/>",
	          {dropOnEmpty: true,
	           format: /^(.*)$/,
	           containment: ["<c:out value="narrowList_${narrow}"/>"],
	           onUpdate: function() {
	                dojo.xhrPost({
	                    url: "<html:rewrite page="/dashboard/ReorderPortlets.do"/>?"+Sortable.serialize('<c:out value="narrowList_${narrow}"/>'),
	                    load: function(){ }
	                });},
	           constraint: 'vertical'});
	      </script>
	</c:if>      
	      <c:choose >
	        <c:when test="${narrow eq 'true'}">              
	          <c:set var="narrow" value="false" />
	        </c:when>
	        <c:otherwise>              
	          <c:set var="narrow" value="true" />
	        </c:otherwise>
	      </c:choose>
	
	    <small name="footer"><br></small><html:img page="/images/spacer.gif" width="${hr}" height="1" border="0"/>
	    <%= divEnd %>
	    <script  type="text/javascript">
	      if (!dojo.isIE) {
	        resizeToCorrectWidth();
	      }
	    </script>
	  </td> 
  
</c:forEach>

  <td class="rowSpanRight"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
</tr>
<tr>
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td class="rowSpanLeft"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td colspan="2">
        <div style=""></div>
    </td>
    <td class="rowSpanRight"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
</tr>
</table>

</div>
<!-- /Content Block --> 

