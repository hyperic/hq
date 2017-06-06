<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="CONST_SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="CONST_APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:importScript path="/js/schedule.js" />
<c:set var="widgetInstanceName" value="listAlerts"/>
<jsu:script>
  	var jsPath = "/js/";
  	var cssPath = "/css/";
  	var isMonitorSchedule = true;
  	var pageData = new Array();

    initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');

    widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="hyphenStr" value="--"/>

<c:url var="pnAction" value="listAlertsAlertPortal.action">
  	<c:param name="mode" value="list"/>
  	<c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  	<c:if test="${not empty param.ps}">
    	<c:param name="ps" value="${param.ps}"/>
  	</c:if>
  	<c:if test="${not empty param.so}">
    	<c:param name="so" value="${param.so}"/>
  	</c:if>
  	<c:if test="${not empty param.sc}">
    	<c:param name="sc" value="${param.sc}"/>
  	</c:if>
  	<c:if test="${not empty param.year}">
    	<c:param name="year" value="${param.year}"/>
  	</c:if>
  	<c:if test="${not empty param.month}">
    	<c:param name="month" value="${param.month}"/>
  	</c:if>
  	<c:if test="${not empty param.day}">
    	<c:param name="day" value="${param.day}"/>
  	</c:if>
</c:url>
<c:url var="sortAction" value="listAlertsAlertPortal.action">
  	<c:param name="mode" value="list"/>
  	<c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  	<c:if test="${not empty param.pn}">
    	<c:param name="pn" value="${param.pn}"/>
  	</c:if>
  	<c:if test="${not empty param.ps}">
    	<c:param name="ps" value="${param.ps}"/>
  	</c:if>
  	<c:if test="${not empty param.year}">
    	<c:param name="year" value="${param.year}"/>
  	</c:if>
  	<c:if test="${not empty param.month}">
    	<c:param name="month" value="${param.month}"/>
  	</c:if>
  	<c:if test="${not empty param.day}">
    	<c:param name="day" value="${param.day}"/>
  	</c:if>
</c:url>
<c:url var="psAction" value="listAlertsAlertPortal.action">
  	<c:param name="mode" value="list"/>
  	<c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  	<c:if test="${not empty param.so}">
    	<c:param name="so" value="${param.so}"/>
  	</c:if>
  	<c:if test="${not empty param.sc}">
	    <c:param name="sc" value="${param.sc}"/>
  	</c:if>
  	<c:if test="${not empty param.year}">
    	<c:param name="year" value="${param.year}"/>
  	</c:if>
  	<c:if test="${not empty param.month}">
    	<c:param name="month" value="${param.month}"/>
  	</c:if>
  	<c:if test="${not empty param.day}">
    	<c:param name="day" value="${param.day}"/>
  	</c:if>
</c:url>
<c:url var="calAction" value="listAlertsAlertPortal.action">
  	<c:param name="mode" value="list"/>
  	<c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  	<c:if test="${not empty param.pn}">
	    <c:param name="pn" value="${param.pn}"/>
  	</c:if>
  	<c:if test="${not empty param.ps}">
    	<c:param name="ps" value="${param.ps}"/>
  	</c:if>
</c:url>

<c:if test="${ CONST_PLATFORM == entityId.type}">
	<c:set var="entityId" value="${Resource.entityId}"/>
	<tiles:insertDefinition name=".page.title.events.list.platform">
	    <tiles:putAttribute name="resource" value="${Resource}"/>
	    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
	    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
	    <tiles:putAttribute name="eid"  value="${entityId.appdefKey}" />
	</tiles:insertDefinition>
    <c:choose>
        <c:when test="${ canControl }">
			<tiles:insertDefinition name=".tabs.resource.platform.alert.alerts">
			    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
			    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
			</tiles:insertDefinition>
        </c:when>
        <c:otherwise>
            <tiles:insertDefinition name=".tabs.resource.platform.alert.alerts.nocontrol">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${CONST_SERVER == entityId.type}">
	<tiles:insertDefinition name=".page.title.events.list.server">
	    <tiles:putAttribute name="resource" value="${Resource}"/>
	    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
	    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
	    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
	</tiles:insertDefinition>
    <c:choose>
        <c:when test="${ canControl }">
            <tiles:insertDefinition name=".tabs.resource.server.alert.alerts">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:when>
        <c:otherwise>
            <tiles:insertDefinition name=".tabs.resource.server.alert.alerts.nocontrol">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${CONST_SERVICE == entityId.type}">
<tiles:insertDefinition name=".page.title.events.list.service">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
</tiles:insertDefinition>
    <c:choose>
        <c:when test="${ canControl }">
            <tiles:insertDefinition name=".tabs.resource.service.alert.alerts">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:when>
        <c:otherwise>
            <tiles:insertDefinition name=".tabs.resource.service.alert.alerts.nocontrol">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${CONST_APPLICATION == entityId.type}">
<tiles:insertDefinition name=".page.title.events.list.application">
    <tiles:putAttribute name="titleName" value="${Resource.name}"/>
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
</tiles:insertDefinition>
<tiles:insertDefinition name=".tabs.resource.application.monitor.alerts">
        <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
        <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
</tiles:insertDefinition>
</c:if>
<c:if test="${CONST_GROUP == entityId.type}">
    <tiles:insertDefinition name=".page.title.events.list.group">
        <tiles:putAttribute name="titleName" value="${Resource.name}"/>
        <tiles:putAttribute name="resource" value="${Resource}"/>
        <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
        <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    </tiles:insertDefinition>
    
</c:if>
<jsu:script>
  	function nextDay() {
    	var tomorrow = new Date(<c:out value="${date}"/> + 86400000);
    	var url = '<c:out value="${calAction}" escapeXml="false"/>' +
              '&year=' + tomorrow.getFullYear() +
              '&month=' + tomorrow.getMonth() +
              '&day=' + tomorrow.getDate();
    	document.location = url;
  	}

  	function previousDay() {
    	var yesterday = new Date(<c:out value="${date}"/> - 86400000);
    	var url = '<c:out value="${calAction}" escapeXml="false"/>' +
              '&year=' + yesterday.getFullYear() +
              '&month=' + yesterday.getMonth() +
              '&day=' + yesterday.getDate();
    	document.location = url;
  	}

  	function popupCal() {
    	var today = new Date(<c:out value="${date}"/>);
    	writeCal(today.getMonth(), today.getFullYear(),
             '<c:out value="${calAction}" escapeXml="false"/>');
  	}
</jsu:script>
<!-- FORM -->
<s:form id="listAlerts_FixForm" name="listAlerts_FixForm" method="POST" action="alertsRemoveAction">
	<s:hidden theme="simple" id="eid" name="eid" value="%{#attr.Resource.entityId}"/>
  	<c:if test="${not empty param.year}">
    	<input type="hidden" name="year" value="<c:out value="${param.year}"/>"/>
  	</c:if>
  	<c:if test="${not empty param.month}">
    	<input type="hidden" name="month" value="<c:out value="${param.month}"/>"/>
  	</c:if>
  	<c:if test="${not empty param.day}">
    	<input type="hidden" name="day" value="<c:out value="${param.day}"/>"/>
  	</c:if>
	
	<tiles:insertDefinition name=".portlet.confirm"/>
	<tiles:insertDefinition name=".portlet.error"/>
	<jsu:script>
		hqDojo.require("dijit.dijit");	
		hqDojo.require("dijit.Dialog");
  		hqDojo.require("dijit.ProgressBar");
          	
		var MyAlertCenter = null;
	</jsu:script>
	<jsu:script onLoad="true">
		MyAlertCenter = new hyperic.alert_center("Alerts");          		
	</jsu:script>
	<table width="100%" style="background-color:#fff;border-left:1px solid gray;border-right:1px solid gray">
		<tr>
			<td>
				<a href="javascript:previousDay()"><img src='<s:url value="/images/schedule_left.gif"/>' border="0"/></a>
			</td>
			<td nowrap="true" class="BoldText"><hq:dateFormatter value="${date}" showTime="false"/></td>
			<td><a href="javascript:nextDay()"><img src='<s:url value="/images/schedule_right.gif"/>' border="0"/></a></td>
			<td><s:a href="javascript:popupCal()"><img src='<s:url value="/images/schedule_iconCal.gif"/>' width="19" height="17" alt="" border="0"/></s:a></td>
			<td class="ButtonCaptionText" width="100%" style="text-align: right; font-style: italic;">
    			<c:url var="path" value="/images/icon_ack.gif"/>
    			<fmt:message key="dash.settings.criticalAlerts.ack.instruction">
      				<fmt:param value="${path}"/>
    			</fmt:message>
			</td>
		</tr>
	</table>
	<c:choose>
  		<c:when test="${not empty param.so}">
    		<c:set var="so" value="${param.so}"/>
  		</c:when>
  		<c:otherwise>
    		<c:set var="so" value="dec"/>
  		</c:otherwise>
	</c:choose>
	<display:table cellspacing="0" cellpadding="0" width="100%" order="${so}" action="${sortAction}" items="${Alerts}" var="Alert">
		<display:column width="1%" property="id" 
	                title="<input type=\"checkbox\" onclick=\"MyAlertCenter.toggleAll(this)\" id=\"${widgetInstanceName}_CheckAllBox\">" 
	                isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
			<display:alertcheckboxdecorator name="alerts" onclick="MyAlertCenter.toggleAlertButtons(this)"
		                                elementId="${widgetInstanceName}_FixForm|${Resource.entityId.appdefKey}|${Alert.id}|${Alert.maxPauseTime}"
										fixable="${!Alert.fixed}" acknowledgeable="${Alert.acknowledgeable}" styleClass="listMember"/> 
		</display:column>
		<display:column width="10%" property="priority" title="alerts.alert.AlertList.ListHeader.Priority">
			<display:prioritydecorator flagKey="alerts.alert.alertlist.listheader.priority"/>
		</display:column>
		<display:column width="20%" property="ctime" sort="true" sortAttr="2" defaultSort="true" 
	                title="alerts.alert.AlertList.ListHeader.AlertDate" 
	                href="viewAlertAlertPortal.action?mode=viewAlert&eid=${Resource.entityId.appdefKey}" 
	                paramId="a" paramProperty="id" >
	    	<display:datedecorator/>
		</display:column>
		<display:column width="20%" property="name" sort="true" sortAttr="1" defaultSort="false" 
	                title="alerts.alert.AlertList.ListHeader.AlertDefinition">
			<display:conditionallinkdecorator test="${Alert.viewable}"
					                      href="viewEscalationAlertsConfigPortal.action?mode=viewDefinition&eid=${Resource.entityId.appdefKey}&ad=${Alert.alertDefId}" />
		</display:column>
		<display:column width="20%" property="conditionFmt" title="alerts.alert.AlertList.ListHeader.AlertCondition"/>
		<display:column width="12%" property="value" title="alerts.alert.AlertList.ListHeader.ActualValue" />
		<display:column width="7%" property="fixed" title="alerts.alert.AlertList.ListHeader.Fixed">
  			<display:booleandecorator flagKey="yesno"/>
		</display:column>
		<display:column width="11%" property="acknowledgeableAndCanTakeAction" title="alerts.alert.AlertList.ListHeader.Acknowledge"
                    href="alertsRemoveAction.action?eid=${Resource.entityId.appdefKey}&alerts=${Alert.id}&buttonAction=ACKNOWLEDGE">
  			<display:booleandecorator flagKey="acknowledgeable"/>
		</display:column>
	</display:table>

	
	<c:if test="${canTakeAction}">
		<tiles:insertDefinition name=".toolbar.list">
  			<tiles:putAttribute name="listItems" value="${Alerts}"/>
  			<tiles:putAttribute name="noButtons" value="true"/>
	  		<tiles:putAttribute name="alerts" value="true"/>
	  		<tiles:putAttribute name="listSize" value="${listSize}"/>
	  		<tiles:putAttribute name="pageNumAction" value="${pnAction}"/>
	  		<tiles:putAttribute name="pageSizeAction" value="${psAction}"/>
	  		<tiles:putAttribute name="defaultSortColumn" value="2"/>
	  		<tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
		</tiles:insertDefinition>
	</c:if>
	<div id="HQAlertCenterDialog" style="display:none;"></div>
	<tiles:insertDefinition name=".page.footer"/>
</s:form>