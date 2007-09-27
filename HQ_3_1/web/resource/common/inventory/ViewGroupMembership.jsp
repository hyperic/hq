<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
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


<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="groups"/>
<tiles:importAttribute name="selfAction"/>

<bean:define id="groupCount" name="groups" property="totalSize"/>
<c:set var="widgetInstanceName" value="listServerGroups"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
groupsWidgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="addToListUrl" value="/resource/${resource.entityId.typeName}/Inventory.do?mode=addGroups&rid=${resource.id}&type=${resource.entityId.type}"/>

<c:url var="psgAction" value="${selfAction}">
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<c:url var="pngAction" value="${selfAction}">
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<c:url var="sgAction" value="${selfAction}">
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<!--  GROUPS CONTENTS -->
<div id="<c:out value="${widgetInstanceName}"/>Div">
<display:table items="${groups}" var="group" action="${sgAction}" orderValue="sog" order="${param.sog}" sortValue="scg" sort="${param.scg}" pageValue="png" page="${param.png}" pageSizeValue="psg" pageSize="${param.psg}" width="100%" cellspacing="0" cellpadding="0">
  <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, groupsWidgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
    <display:checkboxdecorator name="g" onclick="ToggleSelection(this, groupsWidgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column property="name" title="common.header.Group" href="/resource/group/Inventory.do?mode=view&type=5" paramId="rid" paramProperty="id" sort="true" sortAttr="5" defaultSort="true" width="25%"/>
  <display:column property="description" title="common.header.Description" width="75%"/>
</display:table>
</div>
<!--  /  -->

<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="listItems" beanName="groups"/>
  <tiles:put name="listSize" beanName="groupCount"/>
  <tiles:put name="pageSizeParam" value="psg"/>
  <tiles:put name="pageSizeAction" beanName="psgAction"/>
  <tiles:put name="pageNumParam" value="png"/>
  <tiles:put name="pageNumAction" beanName="pngAction"/>  
</tiles:insert>
