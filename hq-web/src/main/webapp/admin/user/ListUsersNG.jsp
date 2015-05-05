<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

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
<jsu:importScript path="/js/listWidget.js" />
<c:set var="widgetInstanceName" value="listUser"/>
<jsu:script>
	var pageData = new Array();
	
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</jsu:script>
<hq:pageSize var="pageSize"/>
<c:url var="pnAction" value="listUser.action">
  <c:param name="mode" value="list"/>
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
<c:url var="psAction" value="listUser.action">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.ps}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="listUser.action">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<!-- FORM -->
<s:form action="removeUser.action">
<c:if test="${not empty param.so}">
  <s:hidden name="so" value="%{param.so}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <s:hidden name="sc" value="%{param.sc}"/>
</c:if>

<tiles:insertDefinition name=".portlet.error"/>
<tiles:insertDefinition name=".portlet.confirm"/>

<tiles:insertDefinition name=".ng.admin.auth.functions"/>

  <display:table cellspacing="0" cellpadding="0" width="100%" action="${sortAction}" pageSize="${pageSize}" items="${AllUsers}" var="user">

    <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="users" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
    </display:column>
    <display:column width="20%" property="firstName" sort="true" sortAttr="7"
                    defaultSort="false" title="admin.user.list.First" /> 
       
    <display:column width="20%" property="lastName" sort="true" sortAttr="8"
                    defaultSort="false" title="admin.user.list.Last" /> 

    <display:column width="20%" property="name" sort="true" sortAttr="3"
                    defaultSort="true" title="username" 
                    href="/admin/user/viewUser.action" paramId="u" paramProperty="id" />
    <display:column width="20%" property="emailAddress" title="admin.user.list.Email" autolink="true" />
    <display:column width="20%" property="department" title="admin.user.list.Department" />
  </display:table>

	
<tiles:insertDefinition name=".toolbar.list">
    <tiles:putAttribute name="listNewUrl" value="startNewUser.action?mode=new"/>
    <tiles:putAttribute name="deleteOnly"><c:out value="${!useroperations['createSubject']}"/></tiles:putAttribute>
    <tiles:putAttribute name="newOnly"><c:out value="${!useroperations['removeSubject']}"/></tiles:putAttribute>
    <tiles:putAttribute name="listItems" value="AllUsers"/>
    <tiles:putAttribute name="listSize" value="AllUsers.totalSize" />
    <tiles:putAttribute name="widgetInstanceName" value="widgetInstanceName"/>  
    <tiles:putAttribute name="pageNumAction" value="pnAction"/>    
    <tiles:putAttribute name="pageSizeAction" value="psAction" />
    <tiles:putAttribute name="defaultSortColumn" value="3"/>
  </tiles:insertDefinition>
  
</s:form>
<!-- /  -->
