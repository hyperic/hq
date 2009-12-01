<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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


<tiles:importAttribute name="selectedIndex" ignore="true"/>
<tiles:importAttribute name="tabList" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>

<c:if test="${not empty tabList}">
  <tiles:importAttribute name="tabUrl"/>
  <tiles:importAttribute name="entityIds" ignore="true"/>
  <tiles:importAttribute name="autogroupResourceType" ignore="true"/>
  <%-- 
    backwards compatibility to support linking with the old
    style, i.e. with the "rid" and "type" parameters
  --%>
  <c:if test="${empty entityIds}">
    <tiles:importAttribute name="resourceId" ignore="true"/>
    <tiles:importAttribute name="resourceType" ignore="true"/>
  </c:if>
</c:if>
<tiles:importAttribute name="subTabList" ignore="true"/>
<c:if test="${not empty subTabList}">
  <tiles:importAttribute name="subTabUrl"/>
</c:if>

<c:if test="${empty selectedIndex}">
  <c:set var="selectedIndex" value="0"/>
</c:if>

<!-- MINI-TABS -->

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td class="MiniTabEmpty"><html:img page="/images/spacer.gif" width="20" height="1" alt="" border="0"/></td>

<c:forEach var="tab" items="${tabList.list}">
  <c:choose>
    <c:when test="${not empty tab.key}">
      <fmt:message var="tabText" key="${tab.key}">
        <c:if test="${not empty tab.param}">
          <fmt:param value="${tab.param}"/>
        </c:if>
      </fmt:message>
    </c:when>
    <c:otherwise>
      <c:set var="tabText" value="${tab.name}"/>
    </c:otherwise>
  </c:choose>
    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
  <c:choose>
    <c:when test="${tab.selected}">
          <td valign="top" width="15"><html:img page="/images/MiniTab_Monitor_lefton.gif" width="11" height="19" alt="" border="0"/></td>
          <td class="MiniTabOn" nowrap><c:out value="${tabText}"/></td>
          <td valign="top" width="17"><html:img page="/images/MiniTab_Monitor_righton.gif" width="11" height="19" alt="" border="0"/></td>
    </c:when>
    <c:otherwise>
      <c:url var="tabLink" value="${tabUrl}">
        <c:choose>
          <c:when test="${not empty tab.mode}">
            <c:param name="mode" value="${tab.mode}"/>
          </c:when>
          <c:otherwise>
            <c:param name="mode" value="currentHealth"/>
          </c:otherwise>
        </c:choose>
        <c:choose>
          <c:when test="${not empty entityIds}">
            <c:forEach var="eid" items="${entityIds}">
              <c:param name="eid" value="${eid}"/>
            </c:forEach>
          </c:when>
          <c:otherwise>
             <c:if test="${not empty resourceId and not empty resourceType}">
               <c:param name="eid" value="${resourceType}:${resourceId}"/>
             </c:if>
          </c:otherwise>
        </c:choose>
        <c:if test="${not empty autogroupResourceType}">
	        <c:param name="ctype" value="${autogroupResourceType}"/>
        </c:if>
      </c:url>
          <td valign="top" width="15"><html:img page="/images/MiniTab_Monitor_leftoff.gif" width="11" height="19" alt="" border="0"/></td>
          <td class="MiniTabOff" nowrap><html:link href="${tabLink}"><c:out value="${tabText}"/></html:link></td>
          <td valign="top" width="17"><html:img page="/images/MiniTab_Monitor_rightoff.gif" width="11" height="19" alt="" border="0"/></td>
    </c:otherwise>
  </c:choose>
        </tr>
      </table>
    </td>
</c:forEach>

    <td width="100%" class="MiniTabEmpty"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
<c:choose>
  <c:when test="${not empty subTabList}">  
  <tr> 
    <td colspan="<c:out value="${tabList.colspan}" />" width="100%" class="SubMiniTab">
    <c:forEach var="tab" varStatus="status" items="${subTabList}">
      <c:choose>
        <c:when test="${not empty tab.key}">
          <fmt:message var="tabText" key="${tab.key}">
            <c:if test="${not empty tab.param}">
              <fmt:param value="${tab.param}"/>
            </c:if>
          </fmt:message>
        </c:when>
        <c:otherwise>
          <c:set var="tabText" value="${tab.name}"/>
        </c:otherwise>
      </c:choose>
      <c:choose>
        <c:when test="${tab.count <= 0}">
      <span class="InactiveText"><c:out value="${tabText}"/></span>
        </c:when>
        <c:when test="${tab.selected}">
          <c:out value="${tabText}"/>
        </c:when>
        <c:otherwise>
          <c:url var="tabLink" value="${subTabUrl}">
            <c:choose>
              <c:when test="${not empty entityIds}">
                <c:forEach var="eid" items="${entityIds}">
                  <c:param name="eid" value="${eid}"/>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:if test="${not empty resourceId and not empty resourceType}">
                  <c:param name="eid" value="${resourceType}:${resourceId}"/>
                </c:if>
              </c:otherwise>
            </c:choose>
            <c:param name="ctype" value="${tab.id}"/>
          </c:url>
      <html:link href="${tabLink}"><c:out value="${tabText}"/></html:link>
        </c:otherwise>
      </c:choose>
      <c:if test="${not status.last}">
       |
      </c:if>
    </c:forEach>
    </td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr> 
    <td colspan="<c:out value="${tabList.colspan}" />" width="100%" class="SubTabCell"><html:img page="/images/spacer.gif" width="1" height="3" alt="" border="0"/></td>
  </tr>
  </c:otherwise>
</c:choose>
</table>
<!-- / MINI-TABS -->
