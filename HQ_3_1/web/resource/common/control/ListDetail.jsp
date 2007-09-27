<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
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

<tiles:importAttribute name="section"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>
<c:set var="entityId" value="${Resource.entityId}"/>

<c:choose>
 <c:when test="${section eq 'service'}">
 <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.service.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
 <!-- CONTROL BAR -->
 <tiles:insert definition=".tabs.resource.service.control.list.detail">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 </tiles:insert>
 </c:when>
 <c:when test="${section eq 'group'}">
 <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.group.full">
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
 <!-- CONTROL BAR -->
 <tiles:insert definition=".tabs.resource.group.control.current">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 </tiles:insert>
 </c:when>
 <c:otherwise>
  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.server.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
  <!-- CONTROL BAR -->
  <tiles:insert definition=".tabs.resource.server.control.list.detail">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>
<br>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<hq:constant symbol="CONTROL_ENABLED_ATTR" var="CONST_ENABLED" />
<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
<c:set var="curStatusTabKey" value="resource.${section}.ControlStatus.Tab"/>
<!-- CURRENT STATUS -->
<tiles:insert definition=".resource.common.control.currentStatus">
 <tiles:put name="tabKey" beanName="curStatusTabKey"/>
 <tiles:put name="section" beanName="section"/>
</tiles:insert>
<br>

<!-- QUICK CONTROL -->
<c:set var="tmpQControl" value=".resource.${section}.control.quickControl"/>
<tiles:insert beanName="tmpQControl">
 <tiles:put name="section" beanName="section"/>
</tiles:insert>

<br>

  <!-- CONTROL ACTION SCHEDULE -->
  <c:if test="${hasControlActions}">
    <c:set var="tmpScheduled" value=".resource.${section}.control.list.scheduled"/>
    <tiles:insert beanName="tmpScheduled">
      <tiles:put name="section" beanName="section"/>
    </tiles:insert>
  </c:if>

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
   <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
   </c:url>
   <c:set var="tmpMessage">
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/> <fmt:message key="resource.common.control.NotEnabled.ToEnable"/> <html:link href="${enableControlLink}"><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/></html:link> <fmt:message key="resource.common.control.NotEnabled.InInventory"/>
   </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insert definition=".portlet.notenabled">
    <tiles:put name="message" beanName="tmpMessage"/>
   </tiles:insert>

</c:otherwise>
</c:choose>

<br>

<tiles:insert definition=".page.footer"/>
