<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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

<tiles:importAttribute name="portlet" ignore="true"/>
<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>
<tiles:importAttribute name="titleDescription" ignore="true"/>

<c:set var="widgetInstanceName" value="alerts"/>
<s:a action="Alerts" id="viewAlertUrl" name="viewAlertUrl" cssStyle="visibility:hidden;">
	<s:param name="mode" value="viewAlert"/>
	<s:param name="eid" value="{eid}"/>
</s:a>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
	var _hqu_<c:out value="${widgetInstanceName}${portlet.token}"/>_refreshTimeout;
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
	
        function requestRecentAlerts<c:out value="${portlet.token}"/>() {
            hqDojo.xhrGet({
                url: "<s:url value="JsonLoadCriticalAlerts.action" />",
                content: {
                    token: "${portlet.token}",
                    hq: (new Date()).getTime()
                },
                handleAs: "json",
                load: function(response, args) {
                    showRecentAlerts(response, args);
                    setTimeout("requestRecentAlerts<c:out value="${portlet.token}"/>()", portlets_reload_time);
                },
                error: function(response, args) {
                    reportError(response, args);
                    setTimeout("requestRecentAlerts<c:out value="${portlet.token}"/>()", portlets_reload_time);
                }
            });
        }
        
	hqDojo.require("dijit.dijit");
	hqDojo.require("dijit.Dialog");
	hqDojo.require("dijit.ProgressBar");
	
	var MyAlertCenter = null;
</jsu:script>
<jsu:script onLoad="true">
	if (MyAlertCenter == null) {
		MyAlertCenter = new hyperic.alert_center("<fmt:message key="dash.home.CriticalAlerts"/>");
	}
	
	hqDojo.connect("requestRecentAlerts<c:out value="${portlet.token}"/>", function() { MyAlertCenter.resetAlertTable(hqDojo.byId('<c:out value="${widgetInstanceName}${portlet.token}"/>_FixForm')); });
	
	requestRecentAlerts<c:out value="${portlet.token}"/>();
</jsu:script>
<c:set var="rssUrl" value="RssViewCriticalAlerts.action"/>

<div class="effectsPortlet">
	<!-- Content Block  -->
	<tiles:insertDefinition name=".header.tab">
  		<tiles:putAttribute name="tabKey" value="dash.home.CriticalAlerts"/>
  		<tiles:putAttribute name="subTitle" value="${titleDescription}"/>
  		<tiles:putAttribute name="adminUrl" value="${adminUrl}" />
  		<c:if test="${not empty portlet.token}">
    		<tiles:putAttribute name="adminToken" value="${portlet.token}"/>
    		<c:set var="tableName" value="recentAlertsTable${portlet.token}"/>
  		</c:if>
  		<c:if test="${empty portlet.token}">
    		<c:set var="tableName" value="recentAlertsTable"/>
  		</c:if>
  		<tiles:putAttribute name="portletName" value="${portletName}" />
		<tiles:putAttribute name="rssBase" value="${rssUrl}" /> 		
	</tiles:insertDefinition>

  	<!-- JSON available at /dashboard/ViewCriticalAlerts.do -->
  	<s:form name="%{#attr.widgetInstanceName}%{#attr.portlet.token}_FixForm" method="POST" action="RemoveAlerts">
  		<s:hidden theme="simple" property="output" value="json" />
  		<table width="100%" cellpadding="0" cellspacing="0" border="0" id="<c:out value="${tableName}"/>" class="portletLRBorder">
     		<thead>
				<tr class="ListRow">
					<td width="1%" class="ListHeaderCheckbox">
						<input type="checkbox" onclick="MyAlertCenter.toggleAll(this)" name="listToggleAll" id="<c:out value="${widgetInstanceName}${portlet.token}"/>_CheckAllBox">
					</td>
					<td width="30%" class="ListHeaderInactiveSorted" align="left">
						Date / Time<img src='<s:url value="/images/tb_sortdown.gif" />' height="9" width="9" border="0" />
					</td>
					<td width="30%" class="ListHeaderInactive">
						<fmt:message key="dash.home.TableHeader.AlertName"/>
					</td>
					<td width="30%" class="ListHeaderInactive">
						<fmt:message key="dash.home.TableHeader.ResourceName"/>
					</td>
					<td width="4%" class="ListHeaderInactive" align="center">
						<fmt:message key="alerts.alert.AlertList.ListHeader.Fixed"/>
					</td>
					<td width="5%" class="ListHeaderInactive" align="center">
						<fmt:message key="alerts.alert.AlertList.ListHeader.Acknowledge"/>
					</td>
				</tr>
     		</thead>
     		<tbody>
		 		<!-- table rows are inserted here dynamically -->
 	 		</tbody>
     		<tfoot>
         		<tr class="ListRow" id="<c:out value="noCritAlerts${portlet.token}"/>">
      				<td class="ListCell" colspan="6"><fmt:message key="dash.home.alerts.no.resource.to.display"/></td>
    			</tr>
         		<tr class="ListRow" id="<c:out value="ackInstruction${portlet.token}"/>" style="display: none;">
           			<td class="ListCell" colspan="6" align="right" style="font-style: italic;">
              			<c:url var="path" value="/images/icon_ack.gif"/>
            			<fmt:message key="dash.settings.criticalAlerts.ack.instruction">
                			<fmt:param value="${path}"/>
              			</fmt:message>
           			</td>
    			</tr>
        		<tr>
             		<td colspan="5">
    					<tiles:insertDefinition name=".toolbar.list">                
      						<tiles:putAttribute name="noButtons" value="true"/>
      						<tiles:putAttribute name="alerts" value="true"/>
      						<tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
	  						<tiles:putAttribute name="portletToken"><c:out value="${portlet.token}"/></tiles:putAttribute> 
      						<%--none of this is being used--%>
      						<tiles:putAttribute name="pageSizeAction" value="" />
      						<tiles:putAttribute name="pageNumAction" value=""/>    
      						<tiles:putAttribute name="defaultSortColumn" value="1"/>
    					</tiles:insertDefinition>
             		</td>
             		<td id="modifiedCritTime<c:out value="${portlet.token}"/>" class="modifiedDate" nowrap="true"></td>
          		</tr>
      		</tfoot>
  		</table>
  		<jsu:script>
  			if (hqDojo.byId("HQAlertCenterDialog") == null) {
  				document.write('<div id="HQAlertCenterDialog" style="display:none;"></div>');
  			}
  		</jsu:script>
  	</s:form>
</div>
