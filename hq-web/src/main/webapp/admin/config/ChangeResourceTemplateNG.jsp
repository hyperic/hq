<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
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
  
  Copyright (C) [2004-2007], Hyperic, Inc.
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

<tiles:insertTemplate template="/admin/config/AdminHomeNavNG.jsp"/>
<jsu:script>
	function onMouseRow(el) {
    	el.style.background="#a6c2e7";
    }

    function offMouseRowEven(el) {
    	el.style.background="#F2F4F7";
    }

    function offMouseRowOdd(el) {
        el.style.background="#EBEDF2";
    }
</jsu:script>

<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
<!-- PLATFORM CONTENTS -->
	<tr class="ListHeaderDark">
      <td width="85%" class="ListHeaderInactiveSorted"><fmt:message key="resource.hub.PlatformTypeTH"/>s</td>
      <td width="15%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
  	</tr>
    <c:forEach var="entry" varStatus="status" items="${platformTypes}">
    <c:choose>
      <c:when test="${even}">
        <tr class="tableRowEven" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="false"/>
      </c:when>
      <c:otherwise>
        <tr class="tableRowOdd" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="true"/>
      </c:otherwise>
    </c:choose>
      <td class="tableCell">
      	
		<s:a href="/resourceHub.action?pn=0&ff=1&ft=1:%{#attr.entry.id}">${entry.name}</s:a>
      		
      </td>
      <tiles:insertDefinition name=".ng.admin.config.DefaultsAction">
        <tiles:putAttribute name="typeName" value="platform"/>
        <tiles:putAttribute name="aetid">1:<c:out value="${entry.id}"/></tiles:putAttribute>
      </tiles:insertDefinition>
    </tr>
  </c:forEach>
<!--  /  -->
	<tr>
	  <td style="padding-top:5px;" colspan="100%">&nbsp;</td>
	</tr>
<!-- Platform Services -->
	<tr class="ListHeaderDark">
      <td width="85%" class="ListHeaderInactiveSorted" colspan="100%"><fmt:message key="resource.hub.PlatformServiceTypeTH"/>s</td>
  	</tr>
	<c:forEach var="platSvc" varStatus="psStatus" items="${platformServiceTypes}">
    <c:choose>
      <c:when test="${even}">
        <tr class="tableRowEven" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="false"/>
      </c:when>
      <c:otherwise>
        <tr class="tableRowOdd" onmouseover=onMouseRow(this); onmouseout=offMouseRowOdd(this);>
        <c:set var="even" value="true"/>
      </c:otherwise>
    </c:choose>
      <td class="tableCell">
      	<img src='<s:url value="/images/icon_indent_arrow.gif"/>' width="16" height="16" border="0"/>
      	<s:a action="resourceHub.action">
      		<s:param name="pn" value="0" />
      		<s:param name="ff" value="3" />
      		<s:param name="ft" >3:${platSvc.id} </s:param>
      		${platSvc.name}
      	</s:a>
      </td>
      <tiles:insertDefinition name=".ng.admin.config.DefaultsAction">
        <tiles:putAttribute name="typeName" value="service"/>
        <tiles:putAttribute name="aetid">3:<c:out value="${platSvc.id}"/></tiles:putAttribute>
      </tiles:insertDefinition>
    </tr>
    </c:forEach>
    <c:forEach var="winSvc" varStatus="wsStatus" items="${windowsServiceTypes}">
    <c:choose>
      <c:when test="${even}">
        <tr class="tableRowEven" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="false"/>
      </c:when>
      <c:otherwise>
        <tr class="tableRowOdd" onmouseover=onMouseRow(this); onmouseout=offMouseRowOdd(this);>
        <c:set var="even" value="true"/>
      </c:otherwise>
    </c:choose>
      <td class="ListCellPrimary">
      	<img src='<s:url value="/images/icon_indent_arrow.gif"/>' width="16" height="16" border="0"/>
      	<s:a action="resourceHub.action">
      		<s:param name="pn" value="0" />
      		<s:param name="ff" value="3" />
      		<s:param name="ft" >3:${winSvc.id}</s:param>
      		${winSvc.name}
      	</s:a>
      </td>
      <tiles:insertDefinition name=".ng.admin.config.DefaultsAction">
        <tiles:putAttribute name="typeName" value="service"/>
        <tiles:putAttribute name="aetid">3:<c:out value="${winSvc.id}"/></tiles:putAttribute>
      </tiles:insertDefinition>
    </tr>
    </c:forEach>

	<tr>
	  <td style="padding-top:5px;" colspan="100%">&nbsp;</td>
	</tr>
<!-- SERVER CONTENTS -->
	<tr>
      <td class="ListCellHeaderSorted" colspan="100%"><fmt:message key="resource.hub.ServerTypeTH"/>s</td>
	</tr>
    <c:forEach var="entry" varStatus="status" items="${serverTypes}">
    <c:set var="server" value="${entry.key}"/>
    <c:set var="services" value="${entry.value}"/>
    <c:if test="${server.virtual == false}">
    <c:choose>
      <c:when test="${even}">
        <tr class="tableRowEven" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="false"/>
      </c:when>
      <c:otherwise>
        <tr class="tableRowOdd" onmouseover=onMouseRow(this); onmouseout=offMouseRowOdd(this);>
        <c:set var="even" value="true"/>
      </c:otherwise>
    </c:choose>
      <td class="ListCellPrimary">
      	<s:a action="resourceHub.action">
      		<s:param name="pn" value="0" />
      		<s:param name="ff" value="2" />
      		<s:param name="ft" >2:${server.id}</s:param>
      		${server.name}
      	</s:a>
      </td>
      <tiles:insertDefinition name=".ng.admin.config.DefaultsAction">
        <tiles:putAttribute name="typeName" value="server"/>
        <tiles:putAttribute name="aetid">2:<c:out value="${server.id}"/></tiles:putAttribute>
      </tiles:insertDefinition>
    </tr>
    <tr class="ListRow">
        <c:forEach var="serviceType" varStatus="status" items="${services}">
    <c:choose>
      <c:when test="${even}">
        <tr class="tableRowEven" onmouseover=onMouseRow(this); onmouseout=offMouseRowEven(this);>
        <c:set var="even" value="false"/>
      </c:when>
      <c:otherwise>
        <tr class="tableRowOdd" onmouseover=onMouseRow(this); onmouseout=offMouseRowOdd(this);>
        <c:set var="even" value="true"/>
      </c:otherwise>
    </c:choose>
            <td class="ListCellPrimary"><img src='<s:url value="/images/icon_indent_arrow.gif"/>' width="16" height="16" border="0"/>
	            <s:a action="resourceHub">
	            	<s:param name="pn" value="0" />
	            	<s:param name="ff" value="3" />
	            	<s:param name="ft" >3:${serviceType.id}</s:param>
	            	${serviceType.name}
	            </s:a>
            </td>
      <tiles:insertDefinition name=".ng.admin.config.DefaultsAction">
        <tiles:putAttribute name="typeName" value="service"/>
        <tiles:putAttribute name="aetid">3:<c:out value="${serviceType.id}"/></tiles:putAttribute>
      </tiles:insertDefinition>
        </tr>
        </c:forEach>   
    </c:if>
    </c:forEach>
<!--  /  -->
</table>

<br/>
<br/>
<tiles:insertTemplate template="/admin/config/AdminHomeNavNG.jsp"/>
