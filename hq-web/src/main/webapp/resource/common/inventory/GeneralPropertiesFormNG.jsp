<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
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


<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="showLocation" ignore="true"/>
<tiles:importAttribute name="locationRequired" ignore="true"/>

<c:choose>
  <c:when test="${not empty resourceOwner}">
    <hq:owner var="ownerStr" owner="${resourceOwner}"/>
  </c:when>
  <c:otherwise>
    <hq:owner var="ownerStr" owner="${sessionScope.webUser}"/>
  </c:otherwise>
</c:choose>

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="GROUP" />

	
<!--  GENERAL PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.common.inventory.props.GeneralPropertiesTab"/>
</tiles:insertDefinition>
<!--  /  -->


<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">

  <tr valign="top">
    <td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="common.label.Name"/>
	</td>

    <td width="30%" class="BlockContent">
      <s:textfield size="30" id="name" name="name" value="%{#attr.resourceForm.name}" errorPosition="bottom" /><br>
    </td>

    <td width="20%" class="BlockLabel"><fmt:message key="resource.common.inventory.props.OwnerLabel"/></td>
    <td width="30%" class="BlockContent"><c:out value="${ownerStr}" escapeXml="false"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>


    <td width="30%" class="BlockContent">
     <s:textarea cols="35" rows="3" name="description" value="%{#attr.resourceForm.description}" errorPosition="bottom"/> 
    </td>


<c:choose>
  <c:when test="${not empty showLocation}">
    <td width="20%" class="BlockLabel">
        <c:if test="${not empty locationRequired}">
            <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
        </c:if>
    <fmt:message key="resource.common.inventory.props.LocationLabel"/></td>
       <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="50" name="location" value="%{location}"/></td>
    
  </c:when>
  <c:otherwise>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </c:otherwise>
</c:choose>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
