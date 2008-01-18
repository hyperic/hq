<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="struts-tiles" prefix="tiles"%>
<%@ taglib uri="struts-html-el" prefix="html"%>
<%@ taglib uri="jstl-c" prefix="c"%>
<%@ taglib uri="jstl-fmt" prefix="fmt"%>
<%@ taglib uri="hq" prefix="hq"%>
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


<tiles:importAttribute name="listItems" ignore="true" />
<tiles:importAttribute name="listSize" ignore="true" />
<tiles:importAttribute name="pageSizeMenuDisabled" ignore="true" />
<tiles:importAttribute name="pageSizeParam" ignore="true" />
<tiles:importAttribute name="pageSizeAction" ignore="true" />
<tiles:importAttribute name="pageNumParam" ignore="true" />
<tiles:importAttribute name="pageNumAction" ignore="true" />
<tiles:importAttribute name="defaultSortColumn" ignore="true" />

<c:if test="${empty pageSizeParam}">
  <c:set var="pageSizeParam" value="ps" />
</c:if>

<c:if test="${empty defaultSortColumn}">
  <c:set var="defaultSortColumn" value="1" />
</c:if>
<c:if test="${empty pageNumParam}">
  <c:set var="pageNumParam" value="pn" />
</c:if>

<c:set var="pageNumber" value="${param[pageNumParam]}" />

<td width="100%">
<table width="100%" cellpadding="0" cellspacing="0" border="0"
  class="ToolbarContent">
  <tr>
    <c:choose>
      <c:when test="${empty pageSizeMenuDisabled}">
        <td width="100%" align="right" nowrap><b><fmt:message
          key="ListToolbar.Total" />&nbsp;<span id="pagingTotal"><c:out
          value="${listSize}" default="0" /></span></b></td>
        <td><html:img page="/images/spacer.gif" width="10" height="1"
          border="0" /></td>
        <td align="right" nowrap><b><fmt:message
          key="ListToolbar.ItemsPerPageLabel" /></b></td>
        <td><html:img page="/images/spacer.gif" width="10" height="1"
          border="0" /></td>
        <td><html:select property="${pageSizeParam}" size="1"
          onchange="goToSelectLocation(this, '${pageSizeParam}',  '${pageSizeAction}');">
          <html:option value="15" key="ListToolbar.ItemsPerPage.15" />
          <c:if test="${listSize > 30}">
            <html:option value="30" key="ListToolbar.ItemsPerPage.30" />
          </c:if>
          <c:if test="${listSize > 45}">
            <html:option value="45" key="ListToolbar.ItemsPerPage.45" />
          </c:if>
          <html:option value="-1" key="ListToolbar.ItemsPerPage.ALL" />
        </html:select></td>
      </c:when>
      <c:otherwise>
        <td width="100%" align="right">&nbsp;</td>
      </c:otherwise>
    </c:choose>
    <td>
      <hq:paginate action="${pageNumAction}" items="${listItems}"
        defaultSortColumn="${defaultSortColumn}" listTotalSize="${listSize}"
        pageValue="${pageNumParam}" pageNumber="${pageNumber}"
        pageSizeValue="${pageSizeParam}" />
    </td>
  </tr>
</table>
</td>
