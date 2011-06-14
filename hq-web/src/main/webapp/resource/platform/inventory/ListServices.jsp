<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<hq:pageSize var="pageSize"/>

<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="services"/>

<c:choose>
  <c:when test="${empty ctype}">
    <c:url var="newServiceUrl" value="/resource/service/Inventory.do">
    	<c:param name="mode" value="new"/>
    	<c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="newServiceUrl" value="/resource/service/Inventory.do">
    	<c:param name="mode" value="new"/>
    	<c:param name="ctype" value="${ctype}"/>
    	<c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
    </c:url>
  </c:otherwise>
</c:choose>

<c:set var="widgetInstanceName" value="listServices"/>

<c:url var="pssAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="pnsAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="ssAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="fsAction" value="${selfAction}"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	<c:out value="wp"/> = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<html:form action="/resource/platform/inventory/RemoveServices">
<input type="hidden" name="rid" value="<c:out value="${Resource.id}"/>"/>
<input type="hidden" name="type" value="1"/>

<tiles:insert definition=".toolbar.filter.resource">
  <tiles:put name="defaultKey" value="resource.hub.filter.AllServiceTypes"/>
  <tiles:put name="filterAction" beanName="fsAction"/>
  <tiles:put name="filterParam" value="resourceType"/>
</tiles:insert>

<!--  GENERAL PROPERTIES TITLE -->
<!--  /  -->

<!-- tiles:insert page="View_FilterToolbar.jsp"/ -->

<!--  SERVICES CONTENTS -->
  <display:table items="${services}" cellspacing="0" cellpadding="0" width="100%" action="${ssAction}" var="service" pageSizeValue="ps" pageSize="${param.ps}" pageValue="pn" page="${param.pn}" orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}">

    <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, wp)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, wp)" styleClass="listMember"/>
    </display:column>
    <display:column width="55%" property="name" sort="true" sortAttr="5"
                    defaultSort="true" title="resource.server.inventory.services.ServiceTH" 
                    href="/resource/service/Inventory.do?mode=view&rid=${service.id}&type=${service.entityId.type}" />
       
    <display:column width="30%" property="description" title="common.header.Description" />
    <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="15%" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
      <display:availabilitydecorator resource="${service}"/>
    </display:column>
  </display:table>

<!--  /  -->
<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" beanName="newServiceUrl"/>
    <tiles:put name="deleteOnly"><c:out value="${!useroperations['createService']}"/>"</tiles:put>
  <tiles:put name="listItems" beanName="services"/>
  <tiles:put name="listSize" beanName="services" beanProperty="totalSize"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="pageSizeAction" beanName="pssAction"/>
  <tiles:put name="pageSizeParam" value="ps"/>
  <tiles:put name="pageNumAction" beanName="pnsAction"/>
  <tiles:put name="pageNumParam" value="pn"/>
  <tiles:put name="defaultSortColumn" value="5"/>
</tiles:insert>

</html:form>

