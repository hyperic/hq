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

<tiles:importAttribute name="criticalAlerts"/>

<c:set var="widgetInstanceName" value="alerts"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<c:url var="rssUrl" value="/rss/ViewCriticalAlerts.rss">
  <c:param name="user" value="${webUser.username}"/>
</c:url>

<div class="effectsPortlet">
<!-- Content Block  -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.CriticalAlerts"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="rssUrl" beanName="rssUrl" />
</tiles:insert>

<c:choose >
  <c:when test="${not empty criticalAlerts}">  
    <html:form method="POST" action="/alerts/RemoveAlerts.do">
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td width="1%" class="ListHeaderCheckbox"><input type="checkbox" onclick="ToggleAll(this, widgetProperties, false)" name="listToggleAll"></td>
        <td width="60%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
        <td width="20%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.AlertName"/></td>
        <td width="20%" class="ListHeaderInactiveSorted" align="center"><fmt:message key="dash.home.TableHeader.DateTime"/><html:img page="/images/tb_sortdown.gif" width="9" height="9" border="0"/></td>
      </tr>
      <c:forEach items="${criticalAlerts}" var="alert">      
      <tr class="ListRow">
        <td class="ListCellCheckbox" width="1%" align="left" valign="top">
          <html:checkbox onclick="ToggleSelection(this, widgetProperties, false)" styleClass="listMember" property="alerts" value="${alert.alertId}"/>
        </td>
        <td class="ListCell">
         <c:choose> 
          <c:when test="{alert.resource eq null}">
            <fmt:message key="dash.home.removed.resource"/>
          </c:when>
          <c:otherwise>
            <html:link page="/Resource.do?eid=${alert.resource.entityId.appdefKey}"><c:out value="${alert.resource.name}"/>&nbsp;</html:link></td>
          </c:otherwise>
        </c:choose>
        <td class="ListCell"><html:link page="/alerts/Alerts.do?mode=viewAlert&eid=${alert.resource.entityId.appdefKey}&a=${alert.alertId}"><c:out value="${alert.alertDefName}"/>&nbsp;</html:link></td>
        <td class="ListCell" align="center" nowrap><hq:dateFormatter value="${alert.ctime}"/>&nbsp;</td>
      </tr>  
      </c:forEach>
    </table>
    <tiles:insert definition=".toolbar.list">                
      <tiles:put name="deleteOnly" value="true"/>
      <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>  
      <%--none of this is being used--%>
      <tiles:put name="pageSizeAction" value="" />
      <tiles:put name="pageNumAction" value=""/>    
      <tiles:put name="defaultSortColumn" value="1"/>
    </tiles:insert>

    <tiles:insert definition=".dashContent.seeAll"/>
  </html:form>
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.alerts.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
