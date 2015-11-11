<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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

<tiles:importAttribute name="section" ignore="true"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>
<jsu:importScript path="/js/listWidget.js" />

<c:set var="widgetInstanceName" value="listServerControl"/>

<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</jsu:script>
<hq:pageSize var="pageSize"/>
<c:if test="${ section=='server'}">
 <c:set var="section" value="Server"/>
</c:if>
<c:if test="${ section=='group'}">
 <c:set var="section" value="Group"/>
</c:if>
<c:if test="${ section=='service'}">
 <c:set var="section" value="Service"/>
</c:if>
<c:if test="${ section=='platform'}">
 <c:set var="section" value="Platform"/>
</c:if>
<c:url var="selfAction" value="controlStatusHistory${section}Controller.action">
	<c:param name="mode" value="history"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${Resource.entityId.type}"/>
</c:url>
<c:if test="${ section=='Group'}">
 <c:set var="section" value="group"/>
</c:if>
<c:if test="${ section=='Server'}">
 <c:set var="section" value="server"/>
</c:if>
<c:if test="${ section=='Service'}">
 <c:set var="section" value="service"/>
</c:if>
<c:if test="${ section=='Platform'}">
 <c:set var="section" value="platform"/>
</c:if>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<%-- now add the context path --%>
<c:url var="selfActionUrl" value="${selfAction}"/>

<c:set var="entityId" value="${Resource.entityId}"/>

<c:choose>
	<c:when test="${section eq 'platform'}">
  		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.platform.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
  		</tiles:insertDefinition>
  		<!-- CONTROL BAR -->
  		<tiles:insertDefinition name=".ng.tabs.resource.platform.control.list.history">
   			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  		</tiles:insertDefinition>
		<c:set var="localSection" value="Platform"/>
 	</c:when>
  	<c:when test="${section eq 'service'}">
  		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.service.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
  		</tiles:insertDefinition>
  		<!-- CONTROL BAR -->
  		<tiles:insertDefinition name=".ng.tabs.resource.service.control.list.history">
   			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  		</tiles:insertDefinition>
		<c:set var="localSection" value="Service"/>
 	</c:when>
 	<c:when test="${section eq 'group'}">
  		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.group.full">
   			<tiles:putAttribute name="titleName" value="${Resource.name}"/>
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
  		</tiles:insertDefinition>
  		<!-- CONTROL BAR -->
  		<tiles:insertDefinition name=".ng.tabs.resource.group.control.list.history">
   			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  		</tiles:insertDefinition>
		<c:set var="localSection" value="Group"/>
 	</c:when>
 	<c:otherwise>
  		<!--  PAGE TITLE -->
 		<tiles:insertDefinition name=".page.title.resource.server.full">
			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
 		</tiles:insertDefinition>
  		<!-- CONTROL BAR -->
  		<tiles:insertDefinition name=".ng.tabs.resource.server.control.list.history">
   			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
 		</tiles:insertDefinition>
		<c:set var="localSection" value="Server"/>
 	</c:otherwise>
</c:choose>
<br>


<hq:constant symbol="CONTROL_ENABLED_ATTR" var="CONST_ENABLED" />
<c:choose>
<c:when test="${requestScope[CONST_ENABLED]}">

<!-- MENU BAR -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.server.ControlHistory.Title"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.error"/>
<tiles:insertDefinition name=".portlet.confirm"/>

<!-- Table Content -->
<s:form action="executeRemove%{#attr.localSection}History">
<s:hidden theme="simple" name="rid" value="%{#attr.Resource.id}"/>
<s:hidden theme="simple" name="type" value="%{#attr.Resource.entityId.type}"/>

<c:set var="tmpNoErrors"><fmt:message key="resource.common.control.NoErrors"/></c:set>

<div id="listDiv">
  <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfActionUrl}"
                  orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}" pageValue="pn" 
                  page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" items="${hstDetailAttr}" var="hstDetail" >
   <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="controlActions" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
   </display:column>
  <c:choose>
   <c:when test="${section eq 'group'}">
    <display:column width="12%" property="action" sort="true" sortAttr="9"
                    defaultSort="true" title="resource.server.ControlHistory.ListHeader.Action" 
                    href="controlStatusHistoryServerController.action?mode=hstDetail&type=${Resource.entityId.type}&rid=${Resource.id}" paramId="bid" paramProperty="id" nowrap="true" />
    </c:when>
    <c:otherwise>
     <display:column width="12%" property="action"  
                     title="resource.server.ControlHistory.ListHeader.Action"/> 
    </c:otherwise>
   </c:choose>
   <display:column width="12%" property="args" title="resource.server.ControlHistory.ListHeader.Arguments">
   </display:column>
   <display:column width="10%" property="status" title="resource.server.ControlHistory.ListHeader.Status" sort="true" sortAttr="10" nowrap="true">
   </display:column> 
   <display:column width="16%" property="startTime" title="resource.server.ControlHistory.ListHeader.Started" 
                   sort="true" defaultSort="false" sortAttr="11" nowrap="true" >
       <display:datedecorator/>
   </display:column>
   <display:column width="12%" property="duration" title="resource.server.ControlHistory.ListHeader.Elapsed"  >
      <display:datedecorator isElapsedTime="true"/>
   </display:column>
   <display:column width="8%" property="subject" title="resource.server.ControlHistory.ListHeader.Subject">
   </display:column>
   <display:column title="resource.server.ControlHistory.ListHeader.Message" headerStyleClass="ListHeaderInactive" property="message">
     <display:alternateDecorator secondChoice="${tmpNoErrors}"/>
   </display:column>
  </display:table>
</div>

<tiles:insertDefinition name=".toolbar.list">
  <tiles:putAttribute name="listNewUrl" value="${selfAction}"/>
  <tiles:putAttribute name="listItems" value="${hstDetailAttr}"/>
  <tiles:putAttribute name="listSize" value="${hstDetailAttrCount}"/>
  <tiles:putAttribute name="pageSizeAction" value="${psAction}" />
  <tiles:putAttribute name="pageNumAction" value="${pnAction}"/>   
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="defaultSortColumn" value="9"/>
  <tiles:putAttribute name="deleteOnly" value="true"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".page.footer"/>
</s:form>

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
   <c:url var="enableControlLink" value="viewResourceInventory${localSection}Visibility.action">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
	<c:param name="eid" value="${Resource.entityId}"/>
   </c:url>
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/> <fmt:message key="resource.common.control.NotEnabled.ToEnable"/> <s:a href="%{enableControlLink}"><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/></s:a> <fmt:message key="resource.common.control.NotEnabled.InInventory"/>
   </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insertDefinition name=".ng.portlet.notenabled">
    <tiles:putAttribute name="message" value="tmpMessage"/>
   </tiles:insertDefinition>

</c:otherwise>
</c:choose>
