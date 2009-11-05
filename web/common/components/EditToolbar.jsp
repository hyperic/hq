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


<tiles:importAttribute name="editUrl"/>
<tiles:importAttribute name="editParamName" ignore="true"/>
<tiles:importAttribute name="editParamValue" ignore="true"/>

<c:choose>
  <c:when test="${not empty editParamName && not empty editParamValue}">
    <c:url var="editUrl" value="${editUrl}">
      <c:param name="${editParamName}" value="${editParamValue}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="editUrl" value="${editUrl}"/>
  </c:otherwise>
</c:choose>

<!-- EDIT TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td><html:link href="${editUrl}"><html:img page="/images/tbb_edit.gif" width="41" height="16" border="0"/></html:link></td>
  </tr>
</table>
<!--  /  -->
