<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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
<tiles:importAttribute name="subTitle" ignore="true"/>
<tiles:importAttribute name="useFromSideBar" ignore="true"/>
<tiles:importAttribute name="useToSideBar" ignore="true"/>
<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>
<tiles:importAttribute name="rssUrl" ignore="true"/>

<!--  TAB HEADER -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <c:if test="${not empty useToSideBar}">
      <td rowspan="2"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
    </c:if>
    <td class="BlockTitle" width="100%" valign="middle">
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
<c:choose><c:when test="${not empty subTitle}"><span class="BlockSubTitle"><c:out value="${subTitle}"/></span></c:when></c:choose>
      <c:if test="${not empty rssUrl}">
        <html:link href="${rssUrl}"><html:img border="0" page="/images/xml.gif"/></html:link>
      </c:if>
    </td>
    <td class="BlockTitle" align="right"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>

    <c:if test="${showUpAndDown}">
      <td class="BlockTitle">
      <c:choose>
        <c:when test="${not empty portletName}">
          <a href="javascript:movePortletUp('<c:out value="${portletName}" escapeXml="false"/>')">
          <html:img page="/images/dash_icon_up.gif" width="16" height="16" border="0"/>
          </a>
        </c:when>
        <c:otherwise>
        <c:if test="${not isFirstPortlet}">
        <html:link page="/dashboard/MovePortletUp.do?portletName=${portletName}"><html:img page="/images/dash_icon_up.gif" border="0" width="17" height="16"/></html:link>
        </c:if>
        </c:otherwise>
      </c:choose>
      </td>
      <td class="BlockTitle">
      <c:choose>
        <c:when test="${not empty portletName}">
          <a href="javascript:movePortletDown('<c:out value="${portletName}" escapeXml="false"/>')">
          <html:img page="/images/dash_icon_down.gif" width="16" height="16" border="0"/>
          </a>
        </c:when>
        <c:otherwise>
        <c:if test="${not isLastPortlet}">
        <html:link page="/dashboard/MovePortletDown.do?portletName=${portletName}"><html:img page="/images/dash_icon_down.gif" border="0" width="17" height="16"/></html:link>
        </c:if>
        </c:otherwise>
      </c:choose>
      </td>
    </c:if>

    <c:if test="${not empty adminUrl}">
    <td class="BlockTitle" align="right"><html:link page="${adminUrl}"><html:img page="/images/dash-icon_edit.gif" width="16" height="16" border="0"/></html:link></td>
    </c:if>
    <c:if test="${not empty portletName}">
      <td class="BlockTitle" align="right">
      <c:choose>
        <c:when test="${not empty portletName}">
          <a href="javascript:removePortlet(<c:out value="'${portletName}', '${title}'" escapeXml="false"/>)">
          <html:img page="/images/dash-icon_delete.gif" width="16" height="16" border="0"/>
          </a>
        </c:when>
        <c:otherwise>
          <html:link page="/dashboard/RemovePortlet.do?portletName=${portletName}"><html:img page="/images/dash-icon_delete.gif" width="16" height="16" border="0"/></html:link>
        </c:otherwise>
      </c:choose>
    </td>
    </c:if>
    <c:if test="${not empty useFromSideBar}">
    <td rowspan="2"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
    </c:if>
  </tr>
</table>
<!--  /  -->
