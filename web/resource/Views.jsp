<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

<c:set var="entityId" value="${Resource.entityId}"/>

<tiles:insert definition=".page.title.resource.application.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<c:choose>
<c:when test="${true}">
<tiles:insert definition=".tabs.resource.platform.views">
  <tiles:put name="resourceId" beanName="entityId" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:when>
<c:when test="${false}">
<tiles:insert definition=".tabs.resource.platform.views.nocontrol">
  <tiles:put name="resourceId" beanName="entityId" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:when>
<c:when test="${false}">
<tiles:insert definition=".tabs.resource.group.views">
  <tiles:put name="resourceId" beanName="entityId" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:when>
<c:when test="${false}">
  <definition name=".tabs.resource.group.views.nocontrol"
              extends=".tabs.resource.group.nocontrol">                            
     <put name="selectedIndex"  value="1" />
     <putList name="subTabList"/>
  </definition>
</c:when>
</c:choose>
<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/portal/AttachTabBody.jsp">
</tiles:insert>
