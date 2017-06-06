<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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


<%-- @param tabKey Key for table header --%>
<%-- @param tabName argument to pass to tabKey localization i.e. "This is {0}." replaces {0}. --%>
<%-- @param subTitle String for subtitle string --%>

<tiles:importAttribute name="tabKey"/>
<tiles:importAttribute name="tabName" ignore="true"/>
<tiles:importAttribute name="icon" ignore="true"/>
<tiles:importAttribute name="subTitle" ignore="true"/>
<tiles:importAttribute name="useFromSideBar" ignore="true"/>
<tiles:importAttribute name="useToSideBar" ignore="true"/>
<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="adminToken" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>
<tiles:importAttribute name="rssBase" ignore="true"/>
<tiles:importAttribute name="dragDrop" ignore="true"/>
<tiles:importAttribute name="cancelAdvanced" ignore="true"/>
<tiles:importAttribute name="enableDelete" ignore="true"/>

<c:set var="enableDelete" value="${sessionScope.modifyDashboard}"/>
<c:set var="dragDrop" value="${sessionScope.modifyDashboard}"/>

<c:if test="${not empty rssBase}">
  <c:url var="rssUrl" value="${rssBase}">
    <c:param name="user" value="${webUser.username}"/>
    <c:param name="token" value="${rssToken}"/>
  </c:url>
</c:if>

<!--  TAB HEADER -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <c:if test="${not empty useToSideBar}">
      <td rowspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
    </c:if>
    <c:if test="${not empty rssUrl}">
    <td class="BlockTitle">
      <s:a action="%{#attr.rssUrl}"><img border="0" src='<s:url value="/images/icon_feed.gif"/>'/></s:a>
    </td>
    </c:if>

    <c:if test="${not empty icon}">
    <td class="BlockTitle">
      <c:out value="${icon}" escapeXml="false"/>
    </td>
    </c:if>

    <td class="BlockTitle" width="100%" valign="middle">
    <c:if test="${dragDrop}">
        <div class="widgetHandle">
    </c:if>

<c:choose>
  <c:when test="${not empty tabKey}">
    <fmt:message var="title" key="${tabKey}">
      <c:if test="${not empty tabName}">
        <fmt:param value="${tabName}"/>
      </c:if>
    </fmt:message>
  </c:when>
  <c:otherwise>
    <c:set var="title" value="${tabName}"/>
  </c:otherwise>
</c:choose>
<c:out value="${title}" escapeXml="false"/>
<c:if test="${not empty subTitle}">
  <span id="<c:out value="${subTitle}_span${adminToken}"/>" class="BlockSubTitle"><c:out value="${subTitle}"/></span>
</c:if>
    <c:if test="${dragDrop}">
      </div>
    </c:if>
    </td>
    <td class="BlockTitle" align="right"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>

    <c:if test="${showUpAndDown}">
      <td class="BlockTitle">
        <c:if test="${empty portletName && not isFirstPortlet}">
	        <s:a action="/dashboard/MovePortletUp">
	        	<s:param name="portletName" value="%{#request.portletName}"/>
	        	<img src='<s:url value="/images/dash_icon_up.gif"/>' border="0" width="17" height="16"/>
	        </s:a>
        </c:if>
      </td>
      <td class="BlockTitle">
        <c:if test="${empty portletName && not isLastPortlet}">
        	<s:a action="/dashboard/MovePortletDown">
        		<s:param name="portletName" value="%{#request.portletName}"/>
        		<img src='<s:url value="/images/dash_icon_down.gif"/>' border="0" width="17" height="16"/>
        	</s:a>
        </c:if>
      </td>
    </c:if>

    <c:if test="${not empty adminUrl}">
    <td class="BlockTitle" align="right">
      <c:choose>
        <c:when test="${not empty adminToken}">
          <s:a action="%{#attr.adminUrl}" >
		  <img src='<s:url value="/images/4.0/icons/properties.gif"/>' width="16" height="16" border="0" />
		  <s:param name="adminToken" value="%{#request.token}"/>
		  </s:a>
        </c:when>
        <c:otherwise>
          <s:a action="%{#attr.adminUrl}">
		  <img src='<s:url value="/images/4.0/icons/properties.gif"/>' width="16" height="16" border="0" />
		  </s:a>
        </c:otherwise>
      </c:choose>
    </td>
    </c:if>
    <c:if test="${not empty portletName}">
      <td class="BlockTitle" align="right">
         <c:choose>
         <c:when test='${enableDelete eq "true"}'>
            <a href="javascript:removePortlet(<c:out value="'${portletName}', '${title}'" escapeXml="false"/>)">
			<img src='<s:url value="/images/4.0/icons/cross.gif"/>' width="16" height="16" border="0" />
            </a>
         </c:when>
         <c:otherwise>
			<img src='<s:url value="/images/btn_close_disabled.gif"/>' width="16" height="16" border="0" />
         </c:otherwise>
         </c:choose>
    </td>
    </c:if>
    <c:if test="${not empty useFromSideBar}">
    <td rowspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0" /></td>
    </c:if>
    <c:if test="${not empty cancelAdvanced}">
    <td class="BlockTitle" align="right">
      <a href="javascript:cancelAdvanced()"><img src='<s:url value="/images/4.0/icons/cross.gif"/>'  border="0" />
    </td>
    </c:if>
  </tr>
</table>
<!--  /  -->
