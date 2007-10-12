<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
 <script language="JavaScript" type="text/javascript">
     function onMouseRow(el) {
             el.style.background="#a6c2e7";
         }

         function offMouseRowEven(el) {
             el.style.background="#F2F4F7";
         }

         function offMouseRowOdd(el) {
             el.style.background="#EBEDF2";
         }
</script>

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
      <td class="tableCell"><html:link page="/ResourceHub.do?ff=1&ft=1:${entry.id}"><c:out value="${entry.name}"/></html:link></td>
      <tiles:insert definition=".admin.config.DefaultsAction">
        <tiles:put name="typeName" value="platform"/>
        <tiles:put name="aetid">1:<c:out value="${entry.id}"/></tiles:put>
      </tiles:insert>
    </tr>
  </c:forEach>
<!--  /  -->

	<tr>
	  <td><html:img page="/images/spacer.gif" width="1" height="15" border="0"/></td>
	  <td></td>
	  <td></td>
	</tr>

<!-- Platform Services -->
	<tr class="ListHeaderDark">
      <td width="85%" class="ListHeaderInactiveSorted"><fmt:message key="resource.hub.PlatformServiceTypeTH"/>s</td>
      <td width="15%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
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
      <td class="tableCell"><html:img page="/images/icon_indent_arrow.gif" width="16" height="16" border="0"/><html:link page="/ResourceHub.do?ff=3&ft=3:${platSvc.id}"><c:out value="${platSvc.name}"/></html:link></td>
      <tiles:insert definition=".admin.config.DefaultsAction">
        <tiles:put name="typeName" value="service"/>
        <tiles:put name="aetid">3:<c:out value="${platSvc.id}"/></tiles:put>
      </tiles:insert>
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
      <td class="ListCellPrimary"><html:img page="/images/icon_indent_arrow.gif" width="16" height="16" border="0"/><html:link page="/ResourceHub.do?ff=3&ft=3:${winSvc.id}"><c:out value="${winSvc.name}"/></html:link></td>
      <tiles:insert definition=".admin.config.DefaultsAction">
        <tiles:put name="typeName" value="service"/>
        <tiles:put name="aetid">3:<c:out value="${winSvc.id}"/></tiles:put>
      </tiles:insert>
    </tr>
    </c:forEach>

	<tr>
	  <td><html:img page="/images/spacer.gif" width="1" height="15" border="0"/></td>
	  <td></td>
	</tr>
<!-- SERVER CONTENTS -->
	<tr>
      <td class="ListCellHeaderSorted"><fmt:message key="resource.hub.ServerTypeTH"/>s</td>
      <td class="ListCellHeader" colspan="2"><html:link page="."><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></html:link></td>
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
      <td class="ListCellPrimary"><html:link page="/ResourceHub.do?ff=2&ft=2:${server.id}"><c:out value="${server.name}"/></html:link></td>
      <tiles:insert definition=".admin.config.DefaultsAction">
        <tiles:put name="typeName" value="server"/>
        <tiles:put name="aetid">2:<c:out value="${server.id}"/></tiles:put>
      </tiles:insert>
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
            <td class="ListCellPrimary"><html:img page="/images/icon_indent_arrow.gif" width="16" height="16" border="0"/>
            <html:link page="/ResourceHub.do?ff=3&ft=3:${serviceType.id}"><c:out value="${serviceType.name}"/></html:link>
            </td>
      <tiles:insert definition=".admin.config.DefaultsAction">
        <tiles:put name="typeName" value="service"/>
        <tiles:put name="aetid">3:<c:out value="${serviceType.id}"/></tiles:put>
      </tiles:insert>
        </tr>
        </c:forEach>   
    </c:if>
    </c:forEach>
<!--  /  -->
</table>

<br/>
<br/>
<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
