<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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
<tiles:insert definition=".page.title.admin.user.view">
 	<tiles:put name="titleName"  beanName="pagetmpname" />
</tiles:insert>

<!-- USER PROPERTIES -->
<tiles:insert definition=".portlet.confirm" flush="true" />
<tiles:insert definition=".portlet.error" flush="true" />
<tiles:insert definition=".admin.user.ViewProperties" />

<c:url var="listAction" value="/admin/user/UserAdmin.do">
	<c:param name="mode" value="list" />
</c:url>

<tiles:insert definition=".page.return">
  	<tiles:put name="returnUrl" beanName="listAction" />
  	<tiles:put name="returnKey" value="admin.user.ReturnToUsers" />
</tiles:insert>
<tiles:insert definition=".page.footer" />