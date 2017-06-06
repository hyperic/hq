<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:importAttribute name="users" ignore="true"/>
<tiles:importAttribute name="userCount" ignore="true"/>
<tiles:importAttribute name="formName" ignore="true"/>
<tiles:importAttribute name="selfUrl" ignore="true"/>

<c:url var="psAction" value="${selfUrl}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfUrl}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="sAction" value="${selfUrl}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
</c:url>


<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.common.inventory.chown.ChangeOwnerTab"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.error"/>
<display:table items="${users}" var="user" action="${sAction}" width="100%" cellpadding="0" cellspacing="0">
  <display:column width="1%" property="id" isLocalizedTitle="false" styleClass="ListCell" headerStyleClass="ListHeader" >
    <display:imagebuttondecorator form="${formName}" input="owner" page="/images/fb_select.gif"/>
  </display:column>
  <display:column width="20%" property="firstName" title="admin.role.users.FirstNameTH"/>
  <display:column width="20%" property="lastName" title="admin.role.users.LastNameTH"/>
  <display:column width="20%" property="name" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true"/>
  <display:column width="20%" property="emailAddress" title="admin.role.users.EmailTH"/>
  <display:column width="20%" property="department" title="admin.role.users.DepartmentTH"/>
</display:table>

<tiles:insertDefinition name=".toolbar.list">                
	<tiles:putAttribute name="noButtons" value="true"/>
	<tiles:putAttribute name="listItems" value="${users}"/>
	<tiles:putAttribute name="listSize" value="${userCount}"/>
	<tiles:putAttribute name="pageSizeAction" value="${psAction}"/>
	<tiles:putAttribute name="pageNumAction" value="${pnAction}"/>  
	<tiles:putAttribute name="defaultSortColumn" value="3"/>
</tiles:insertDefinition>

<s:hidden theme="simple" name="owner" />
