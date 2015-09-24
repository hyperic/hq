<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<table border="0"><tr><td class="LinkBox">

<c:if test="${useroperations['createApplication']}">
  <s:a action="newResourceInventoryApplicationVisibility.action">
  	<s:param name="mode" value="'new'"/>
  	<fmt:message key="resource.hub.NewApplicationLink"/>
  	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
  </s:a><br/>
</c:if>
<%-- everyone can create groups --%>
  <s:a action="newResourceInventoryGroupVisibility.action">
  	<s:param name="mode" value="'new'"/>
  	<fmt:message key="resource.hub.NewGroupLink"/>
  	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
  </s:a><br/>
<c:if test="${useroperations['createPlatform']}">
  <s:a action="/resource/platform/Inventory">
  	<s:param name="mode" value="'new'"/>
  	<fmt:message key="resource.hub.NewPlatformLink"/>
  	<img src='<s:url value="/images/title_arrow.gif"/>' width="11" height="9" alt="" border="0"/>
  </s:a><br/>
</c:if>

</td></tr></table>
