<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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

<hq:pageSize var="pageSize" />

<!--  PAGE TITLE -->
<c:set var="pagetmpname" value="${User.firstName} ${User.lastName}" />
<tiles:insertDefinition name=".page.title.admin.user.view">
 	<tiles:putAttribute name="titleName"  value="${pagetmpname}" /> 
</tiles:insertDefinition>

<!-- USER PROPERTIES -->
<!-- <tiles:insertDefinition name=".portlet.confirm" flush="true" /> -->
<tiles:insertDefinition name=".portlet.error" flush="true" />
<tiles:insertDefinition name=".admin.user.ViewProperties" />

<c:url var="listAction" value="/admin/user/listUser.action">
	<c:param name="mode" value="list" />
</c:url>

<tiles:insertDefinition name=".page.return">
  	<tiles:putAttribute name="returnUrl" value="${listAction}" />
  	<tiles:putAttribute name="returnKey" value="admin.user.ReturnToUsers" />
</tiles:insertDefinition>
<tiles:insertDefinition name=".page.footer" />