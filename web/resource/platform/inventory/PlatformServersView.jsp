<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="display" prefix="display" %>
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


<tiles:importAttribute name="platform"/>
<tiles:importAttribute name="servers"/>
<tiles:importAttribute name="serverCount"/>
<tiles:importAttribute name="selfAction"/>

<c:set var="widgetInstanceName" value="listPlatformServers"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
serversWidgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="listNewUrl" value="/resource/server/Inventory.do?mode=new&rid=${platform.id}&type=${platform.entityId.type}"/>

<c:url var="fsAction" value="${selfAction}">
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<c:url var="pssAction" value="${selfAction}">
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<c:url var="pnsAction" value="${selfAction}">
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<c:url var="ssAction" value="${selfAction}">
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
</c:url>

<tiles:insert definition=".toolbar.filter.resource">
  <tiles:put name="defaultKey" value="resource.platform.inventory.servers.filter.AllServerTypes"/>
  <tiles:put name="filterAction" beanName="fsAction"/>
  <tiles:put name="filterParam" value="fs"/>
</tiles:insert>

<!--  SERVERS CONTENTS -->
<div id="<c:out value="${widgetInstanceName}"/>Div">
  <display:table items="${servers}" var="server" action="${ssAction}" orderValue="sos" order="${param.sos}" sortValue="scs" sort="${param.scs}" pageValue="pns" page="${param.pns}" pageSizeValue="pss" pageSize="${param.pss}" width="100%" cellspacing="0" cellpadding="0">
    <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, serversWidgetProperties)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
      <display:checkboxdecorator name="r" onclick="ToggleSelection(this, serversWidgetProperties)" styleClass="listMember"/>
    </display:column>
    <display:column property="name" title="resource.platform.inventory.servers.ServerTH" href="/resource/server/Inventory.do?mode=view&rid=${server.id}&type=${server.entityId.type}" sort="true" sortAttr="5" defaultSort="true" width="20%"/>
    <display:column value="${server.serverType.name}" title="resource.platform.inventory.servers.TypeTH" width="20%"/>
    <display:column property="installPath" title="resource.platform.inventory.servers.InstallPathTH" width="20%"/>
    <display:column property="description" title="common.header.Description" width="20%"/>
    <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="20%" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
      <display:availabilitydecorator resource="${server}"/>
    </display:column>
  </display:table>
<!--  / -->
</div>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" beanName="listNewUrl"/>
  <tiles:put name="listItems" beanName="servers"/>
  <tiles:put name="listSize" beanName="serverCount"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="pageSizeAction" beanName="pssAction"/>
  <tiles:put name="pageSizeParam" value="pss"/>
  <tiles:put name="pageNumAction" beanName="pnsAction"/>
  <tiles:put name="pageNumParam" value="pns"/>
  <tiles:put name="defaultSortColumn" value="1"/>
</tiles:insert>
