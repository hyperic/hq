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
  
  Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_APP" var="CONST_ADHOC_APP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_PS" var="CONST_COMPAT_PS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="CONST_SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="CONST_APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/> 
<c:set var="entityId" value="${Resource.entityId}"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

<c:choose>
	<c:when test="${CONST_APPLICATION == entityId.type}">
	 
	   <tiles:insert definition=".page.title.resource.application.full">
		  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
		  <tiles:put name="resource" beanName="Resource"/>
		  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
		  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
		</tiles:insert>

		<tiles:insert definition=".tabs.resource.application.views">
	      <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
	      <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
	    </tiles:insert>
	    
	</c:when>
	<c:when test="${CONST_GROUP == entityId.type}">
		
		<tiles:insert definition=".page.title.resource.group.full">
		  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}"/></tiles:put>
		  <tiles:put name="resource" beanName="Resource"/>
		  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
		  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
		</tiles:insert>
		<c:choose>
		   <c:when test="${Resource.groupType == CONST_ADHOC_PSS ||
                           Resource.groupType == CONST_ADHOC_GRP ||
                           Resource.groupType == CONST_ADHOC_APP }"> 
                <tiles:insert definition=".tabs.resource.group.views.inventoryonly">
                    <tiles:put name="resource" beanName="Resource" />
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                </tiles:insert>
            </c:when>
		    <c:when test="${ canControl }">
		        <tiles:insert definition=".tabs.resource.group.views">
		          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		        </tiles:insert>
		    </c:when>
		    <c:otherwise>
		        <tiles:insert definition=".tabs.resource.group.views.nocontrol">
		          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		        </tiles:insert>
		    </c:otherwise>
		</c:choose>    
		
	</c:when>
	<c:when test="${CONST_PLATFORM == entityId.type}">
	   
	   <tiles:insert definition=".page.title.resource.platform.full">
		  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
		  <tiles:put name="resource" beanName="Resource"/>
		  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
		  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
		</tiles:insert>
		
		<tiles:insert definition=".tabs.resource.platform.views">
		  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		</tiles:insert>
		
	</c:when>
	<c:when test="${CONST_SERVER == entityId.type}">
	
		<tiles:insert definition=".page.title.resource.server.full">
		  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
		  <tiles:put name="resource" beanName="Resource"/>
		  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
		  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
		</tiles:insert>
		<c:choose>
		    <c:when test="${canControl}">
		        <tiles:insert definition=".tabs.resource.server.views">
		          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		        </tiles:insert>
		    </c:when>
		    <c:otherwise>
		        <tiles:insert definition=".tabs.resource.server.views.nocontrol">
		          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		        </tiles:insert>
		    </c:otherwise>
        </c:choose>
        
    </c:when>
    <c:when test="${CONST_SERVICE == entityId.type}">
    
		<tiles:insert definition=".page.title.resource.service.full">
		  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
		  <tiles:put name="resource" beanName="Resource"/>
		  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
		  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
		</tiles:insert>
		<c:choose>
		 <c:when test="${ canControl }">
		  <tiles:insert definition=".tabs.resource.service.monitor.visibility">
		   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		  </tiles:insert>
		 </c:when>
		 <c:otherwise>
		  <tiles:insert definition=".tabs.resource.service.monitor.visibility.nocontrol">
		   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
		   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		  </tiles:insert>
		 </c:otherwise>
		</c:choose>
		
    </c:when>
</c:choose>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<tiles:insert page="/portal/AttachTabBody.jsp">
</tiles:insert>
