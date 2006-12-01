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

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<script type="text/javascript">
function requestRecentAlerts() {
	var critAlertUrl = "<html:rewrite page="/dashboard/ViewCriticalAlerts.do"/>"
	new Ajax.Request(critAlertUrl, {method: 'get', onSuccess:showRecentAlerts, onFailure :reportError});
}
onloads.push(requestRecentAlerts);
Ajax.Responders.register({
	onCreate: function() {
		if($('loading') && Ajax.activeRequestCount > 0)
			Effect.Appear('loading',{duration: 0.50, queue: 'end'});
	},
	onComplete: function() {
		if($('loading') && Ajax.activeRequestCount == 0)
			Effect.Fade('loading',{duration: 0.2, queue: 'end'});
	}
});
</script>
<c:set var="rssUrl" value="/rss/ViewCriticalAlerts.rss"/>

<div class="effectsPortlet">
<!-- Content Block  -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.CriticalAlerts"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <c:if test="${not empty portlet.token}">
    <tiles:put name="adminToken" beanName="portlet" beanProperty="token"/>
  </c:if>
  <tiles:put name="portletName"><c:out value="${portlet.fullUrl}"/></tiles:put>
  <tiles:put name="rssBase" beanName="rssUrl" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

  <!-- JSON available at /dashboard/ViewCriticalAlerts.do -->

  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="recentAlertsTable">
  	<tbody>
		<tr class="ListRow">
			<td width="1%" class="ListHeaderCheckbox">
				<input type="checkbox" onclick="ToggleAll(this, widgetProperties, false)" name="listToggleAll">
			</td>
			<td width="60%" class="ListHeaderInactive">
				Resource Name
			</td>
			<td width="20%" class="ListHeaderInactive">
				Alert Name
			</td>
			<td width="20%" class="ListHeaderInactiveSorted" align="center">
				Date / Time<img src="images/tb_sortdown.gif" height="9" width="9" border="0">
			</td>
		</tr>
		
		
 	 </tbody>
  </table>
    
     <table width="100%" cellpadding="0" cellspacing="0" border="0" style="display:none;" id="noCritAlerts">
     	<tr class="ListRow">
      		<td class="ListCell"><fmt:message key="dash.home.alerts.no.resource.to.display"/></td>
    	</tr>
  </table>

</div>
