<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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


<tiles:importAttribute name="users"/>
<tiles:importAttribute name="userCount"/>
<tiles:importAttribute name="formName"/>
<tiles:importAttribute name="selfUrl"/>

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

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.chown.ChangeOwnerTab"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<display:table items="${users}" var="user" action="${sAction}" width="100%" cellpadding="0" cellspacing="0">
  <display:column width="1%" property="id" isLocalizedTitle="false" styleClass="ListCell" headerStyleClass="ListHeader" >
    <display:imagebuttondecorator form="${formName}" input="o" page="/images/fb_select.gif"/>
  </display:column>
  <display:column width="20%" property="firstName" title="admin.role.users.FirstNameTH"/>
  <display:column width="20%" property="lastName" title="admin.role.users.LastNameTH"/>
  <display:column width="20%" property="name" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true"/>
  <display:column width="20%" property="emailAddress" title="admin.role.users.EmailTH"/>
  <display:column width="20%" property="department" title="admin.role.users.DepartmentTH"/>
</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="noButtons" value="true"/>
  <tiles:put name="listItems" beanName="users"/>
  <tiles:put name="listSize" beanName="userCount"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>  
  <tiles:put name="defaultSortColumn" value="3"/>
</tiles:insert>

<html:hidden property="o"/>
