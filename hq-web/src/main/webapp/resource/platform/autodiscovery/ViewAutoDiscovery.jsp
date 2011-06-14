<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="selfAction" value="/resource/platform/AutoDiscovery.do?mode=view&rid=${Resource.id}&type=${entityId.type}"/>

<tiles:insert definition=".page.title.resource.platform.full">
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<tiles:insert definition=".tabs.resource.platform.inventory.autoDiscovery">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>

&nbsp;<br>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".resource.platform.autodiscovery.ScanControlPrep">
  <tiles:put name="scanstate" beanName="ScanState" />
  <tiles:put name="selfAction" beanName="selfAction" />
</tiles:insert>
&nbsp;<br>

<c:if test="${canModify && canCreateChild && not empty ScanState}">
    <tiles:insert page="/resource/platform/autodiscovery/ListSchedule.jsp"/>
</c:if>
<tiles:insert definition=".page.footer"/>
