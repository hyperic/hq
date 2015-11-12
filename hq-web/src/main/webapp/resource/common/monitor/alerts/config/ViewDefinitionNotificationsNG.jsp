<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
<tiles:importAttribute name="notificationsTile" ignore="true"/>
<c:if test="${not alertDef.deleted}">
	<tiles:importAttribute name="formAction" ignore="true"/>
	<tiles:importAttribute name="addMode" ignore="true"/>
	<tiles:importAttribute name="defaultSortColumn" ignore="true"/>
	
	<c:url var="viewUrl" value="AlertsConfigPortal.action" >
	  	<c:choose>
	  		<c:when test="${not empty Resource}">
	    		<c:param name="eid" value="${Resource.entityId.appdefKey}"/>
	  		</c:when>
	  		<c:otherwise>
	    		<c:param name="aetid" value="${ResourceType.appdefTypeKey}"/>
	  		</c:otherwise>
	  	</c:choose>
	  	<c:param name="ad" value="${alertDef.id}"/>
	</c:url>
	
	<c:url var="selfUrl" value="${viewUrl}">
	  	<c:param name="mode" value="${param.mode}"/>
	</c:url>
	
	<c:url var="viewUsersUrl" value="viewUsers${viewUrl}.action">
	  	<c:param name="mode" value="viewUsers"/>
	</c:url>
	
	<c:url var="viewOthersUrl" value="viewOthers${viewUrl}.action">
	  	<c:param name="mode" value="viewOthers"/>
	</c:url>
	
	<c:url var="viewEscalationUrl" value="viewEscalation${viewUrl}.action">
	  	<c:param name="mode" value="viewEscalation"/>
	</c:url>
	
	<c:url var="viewOpenNMSUrl" value="viewOpenNMS${viewUrl}.action">
	  	<c:param name="mode" value="viewOpenNMS"/>
	</c:url>
	
	<tiles:insertDefinition name=".events.config.view.notifications.tabs">
	  	<tiles:putAttribute name="viewUsersUrl" value="${viewUsersUrl}"/>
	  	<tiles:putAttribute name="viewOthersUrl" value="${viewOthersUrl}"/>
	  	<tiles:putAttribute name="viewEscalationUrl" value="${viewEscalationUrl}"/>
	  	<tiles:putAttribute name="viewOpenNMSUrl" value="${viewOpenNMSUrl}"/>
	</tiles:insertDefinition>
	<jsu:importScript path="/js/listWidget.js" />
	<c:set var="widgetInstanceName" value="list"/>
	<jsu:script>
		var pageData = new Array();
		initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
		widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	</jsu:script>
	<c:choose>
		<c:when test="${not empty formAction}">
			<!-- FORM -->
			<s:form action="%{#attr.formAction}">
				<s:hidden theme="simple" name="ad" id="ad" value="%{#attr.alertDef.id}"/>
				<c:choose>
					<c:when test="${not empty Resource}">
	  					<s:hidden theme="simple" name="rid" id="rid" value="%{#attr.Resource.id}"/>
	  					<s:hidden theme="simple" name="type" id="type" value="%{#attr.Resource.entityId.type}"/>
					</c:when>
					<c:otherwise>
	  					<s:hidden theme="simple" name="aetid" id="aetid" value="%{#attr.ResourceType.appdefTypeKey}"/>
					</c:otherwise>
				</c:choose>
	
				<tiles:insertDefinition name="${notificationsTile}">
	  				<tiles:putAttribute name="selfUrl" value="${selfUrl}"/>
				</tiles:insertDefinition>
	
				<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
				<c:choose>
	    			<c:when test="${not canModify || null == notifyList || empty listSize}">
	      				<!-- permission error occured -->
	      				<fmt:message key="alert.config.error.no.permission"/>
	    			</c:when>
	    			<c:otherwise>
	      				<s:hidden theme="simple" name="so" id="so" value="%{#attr.param.so}"/>
	      				<s:hidden theme="simple" name="sc" id="sc" value="%{#attr.param.sc}"/>
	    
	      				<tiles:insertDefinition name=".ng.toolbar.addToList">
		  					<c:choose>
		  						<c:when test="${not empty Resource}">
		    						<tiles:putAttribute name="addToListUrl">
		    							<s:url action="%{#attr.addMode}AlertsConfigPortal">
		    								<s:param name="mode" value="%{#attr.addMode}"/>
		    								<s:param name="eid" value="%{#attr.Resource.entityId.appdefKey}"/>
		    								<s:param name="ad" value="%{#attr.alertDef.id}"/>
		    							</s:url>
		    						</tiles:putAttribute>
		  						</c:when>
		  						<c:otherwise>
		    						<tiles:putAttribute name="addToListUrl">
		    							<s:url action="%{#attr.addMode}AlertsConfigPortal">
		    								<s:param name="mode" value="%{#attr.addMode}"/>
		    								<s:param name="aetid" value="%{#attr.ResourceType.appdefTypeKey}"/>
		    								<s:param name="ad" value="%{#attr.alertDef.id}"/>
		    							</s:url>
		    						</tiles:putAttribute>
		  						</c:otherwise>
		  					</c:choose>
		        			<tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
		        			<tiles:putAttribute name="listItems" value="${notifyList}"/>
		        			<tiles:putAttribute name="listSize" value="${listSize}"/>
		        			<tiles:putAttribute name="defaultSortColumn"><c:out value="${defaultSortColumn}"/></tiles:putAttribute>
		        			<tiles:putAttribute name="pageNumAction" value="${param.mode}${selfUrl}"/>
		      				<tiles:putAttribute name="pageSizeAction" value="${param.mode}${selfUrl}"/>
	      				</tiles:insertDefinition>
	    			</c:otherwise>
	  			</c:choose>
			</s:form>
		</c:when>
		<c:otherwise>
		
			<tiles:insertDefinition name="${notificationsTile}">
	  			<tiles:putAttribute name="selfUrl" value="${selfUrl}"/>
			</tiles:insertDefinition>
		</c:otherwise>
	</c:choose>
</c:if>
<br/>
