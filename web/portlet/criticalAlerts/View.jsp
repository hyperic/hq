<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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

<tiles:importAttribute name="portlet"/>

<c:set var="widgetInstanceName" value="alerts"/>
<html:link page="/alerts/Alerts.do?mode=viewAlert&eid=" linkName="viewAlertUrl" styleId="viewAlertUrl" style="visibility:hidden;"></html:link>
<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  

function requestRecentAlerts<c:out value="${portlet.token}"/>() {
    var dummyStr = '&hq=' + new Date().getTime();
    var critAlertUrl = "<html:rewrite page="/dashboard/ViewCriticalAlerts.do?token=${portlet.token}"/>" + dummyStr;
	new Ajax.Request(critAlertUrl, {method: 'get', onSuccess:showRecentAlerts, onFailure :reportError});
}

onloads.push(requestRecentAlerts<c:out value="${portlet.token}"/>);

function acknowledgeAlert(img, eid, aid) {
    //new Effect.Shrink(img, {duration: 1.5});
    var ackAlertUrl = "<html:rewrite page="/alerts/Alerts.do?mode=ACKNOWLEDGE&eid="/>"
    var pars = eid + "&a=" + aid;
    var url = ackAlertUrl + pars;
    new Ajax.Request(url);
}
</script>
<c:set var="rssUrl" value="/rss/ViewCriticalAlerts.rss"/>

<div class="effectsPortlet">
<!-- Content Block  -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.CriticalAlerts"/>
  <tiles:put name="subTitle" beanName="portlet" beanProperty="description"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <c:if test="${not empty portlet.token}">
    <tiles:put name="adminToken" beanName="portlet" beanProperty="token"/>
    <c:set var="tableName" value="recentAlertsTable${portlet.token}"/>
  </c:if>
  <c:if test="${empty portlet.token}">
    <c:set var="tableName" value="recentAlertsTable"/>
  </c:if>
  <tiles:put name="portletName"><c:out value="${portlet.fullUrl}"/></tiles:put>
  <tiles:put name="rssBase" beanName="rssUrl" />
</tiles:insert>

  <!-- JSON available at /dashboard/ViewCriticalAlerts.do -->
    <html:form method="POST" action="/alerts/RemoveAlerts.do">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="<c:out value="${tableName}"/>" class="portletLRBorder">
     <thead>
		<tr class="ListRow">
			<td width="1%" class="ListHeaderCheckbox">
				<input type="checkbox" onclick="ToggleAll(this, widgetProperties, false);" name="listToggleAll" id="listToggleAll">
			</td>
			<td width="30%" class="ListHeaderInactiveSorted" align="left">
				Date / Time<html:img page="/images/tb_sortdown.gif" height="9" width="9" border="0" />
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
              <c:url var="path" value="/"/>
              <fmt:message key="dash.settings.criticalAlerts.ack.instruction">
                <fmt:param value="${path}"/>
              </fmt:message>
    	</tr>
        <tr>
             <td colspan="5">
    <tiles:insert definition=".toolbar.list">                
      <tiles:put name="noButtons" value="true"/>
      <tiles:put name="alerts" value="true"/>
      <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>  
      <%--none of this is being used--%>
      <tiles:put name="pageSizeAction" value="" />
      <tiles:put name="pageNumAction" value=""/>    
      <tiles:put name="defaultSortColumn" value="1"/>
    </tiles:insert>
             </td>
             <td id="modifiedCritTime<c:out value="${portlet.token}"/>" class="modifiedDate" nowrap="true"></td>
          </tr>
      </tfoot>
  </table>
  </html:form>
</div>
