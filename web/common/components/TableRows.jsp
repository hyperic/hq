<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<tiles:importAttribute name="principalBean" ignore="true" />
<tiles:importAttribute name="rowBean" ignore="true" />
<c:choose>
<!-- header -->
<c:when test="${not empty principalBean}">
    <th>ID Header (.table must be overridden with a tableComp defined)</th>
    <th>Name Header (.table must be overridden with a tableComp defined)</th>
</c:when>
<!-- data -->
<c:when test="${not empty rowBean}">
    <td><c:out value="${rowBean.id}" /></td><td><c:out value="${rowBean.name}" /></td>
</c:when>
<c:otherwise>
    <td>ID empty data (.table must be overridden with a tableComp defined)</td>
    <td>Name empty data (.table must be overridden with a tableComp defined)</td>
</c:otherwise>
</c:choose>
