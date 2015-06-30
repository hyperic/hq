<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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


<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="NetworkServer" ignore="true"/>
<tiles:importAttribute name="FileServer" ignore="true"/>
<tiles:importAttribute name="WindowsServer" ignore="true"/>
<tiles:importAttribute name="ProcessServer" ignore="true"/>

<c:set var="resource" value="${Resource}"/>
<c:if test="${not empty resource}">

<hq:userResourcePermissions debug="false" resource="${Resource}"/>

<table border="0"><tr><td class="LinkBox">
    <c:if test="${canModify}">
            <s:a action="/resource/platform/Inventory">
            	<s:param name="mode" value="'editConfig'"/>
            	<s:param name="eid" value="%{#attr.resource.entityId}"/>
            	<fmt:message key="resource.platform.inventory.link.Configure"/>
            	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
            </s:a><br/>
    </c:if>
    <c:if test="${canRemove}" >
    	<tiles:insertDefinition name=".resource.common.quickDelete">
      		<tiles:putAttribute name="resource" value="${resource}"/>
	  		<tiles:putAttribute name="deleteMessage">
				<fmt:message key="resource.platform.inventory.link.DeletePlatform"/>
	  		</tiles:putAttribute>
    	</tiles:insertDefinition>
		<br>
	</c:if>
    <c:choose>
        <c:when test="${canCreateChild}" >
            <s:a action="/resource/server/Inventory">
            	<s:param name="mode" value="'new'"/>
            	<s:param name="eid" value="%{#attr.resource.entityId}"/>
            	<fmt:message key="resource.platform.inventory.NewServerLink"/>
            	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
            </s:a><br/>
            <s:a action="/resource/service/Inventory">
            	<s:param name="mode" value="'new'"/>
            	<s:param name="eid" value="%{#attr.resource.entityId}"/>
            	<fmt:message key="resource.platform.inventory.NewServiceLink"/>
            	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
            </s:a><br/>
        </c:when>
        <c:otherwise>
            <fmt:message key="resource.platform.inventory.NewServerLink"/><img src='<s:url value="/images/tbb_new_locked.gif"/>' alt="" border="0"/><br>
            <fmt:message key="resource.platform.inventory.NewServiceLink"/><img src='<s:url value="/images/tbb_new_locked.gif"/>' alt="" border="0"/><br>
        </c:otherwise>
    </c:choose>
    <c:choose>
        <c:when test="${canModify && canCreateChild}" >            
            <s:a action="/resource/platform/AutoDiscovery">
            	<s:param name="mode" value="'new'"/>
            	<s:param name="rid" value="%{#attr.resource.id}"/>
            	<s:param name="type" value="%{#attr.resource.entityId.type}"/>
            	<fmt:message key="resource.platform.inventory.NewDiscoveryLink"/>
            	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
            </s:a><br/>
        </c:when>
        <c:otherwise>
            <fmt:message key="resource.platform.inventory.NewDiscoveryLink"/><img src='<s:url value="/images/tbb_new_locked.gif"/>' alt="" border="0"/><br>
        </c:otherwise>
    </c:choose>
    <c:choose>
 	 	<c:when test="${canModify}">
	 	 	<s:a action="/alerts/EnableAlerts">
	 	 		<s:param name="alertState" value="'enabled'"/>
	 	 		<s:param name="eid" >${resource.entityId.type}:${resource.id}</s:param>
	 	 		<fmt:message key="resource.platform.alerts.EnableAllAlerts"/>
	 	 	</s:a>
	 	 	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/><br/>
	 	 	<s:a action="/alerts/EnableAlerts">
	 	 		<s:param name="alertState" value="'disabled'"/>
	 	 		<s:param name="eid" >${resource.entityId.type}:${resource.id}</s:param>
	 	 		<fmt:message key="resource.platform.alerts.DisableAllAlerts"/>
	 	 	</s:a>
	 	 	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/><br/>
 	 	</c:when>
 	 	<c:otherwise>
	 	 	<fmt:message key="resource.platform.alerts.EnableAllAlerts"/><img src='<s:url value="/images/tbb_new_locked.gif"/>' alt="" border="0"/><br/>
	 	 	<fmt:message key="resource.platform.alerts.DisableAllAlerts"/><img src='<s:url value="/images/tbb_new_locked.gif"/>' alt="" border="0"/><br/>
 	 	</c:otherwise>
 	 </c:choose>
 	  <tiles:insertDefinition name=".resource.common.quickFavorites">
      <tiles:putAttribute name="resource" value="${resource}"/>
    </tiles:insertDefinition>
	<br />
	<s:a href="#" styleClass="AddToGroupMenuLink"><fmt:message key="resource.group.AddToGroup.Title"/><img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/></s:a>
</td></tr></table>

<tiles:insertDefinition name=".resource.common.addToGroup">
	<tiles:putAttribute name="resource" value="${resource}"/>
</tiles:insertDefinition>

</c:if>
