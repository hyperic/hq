<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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
<c:url var="pnAction" value="/admin/user/UserAdmin.do">
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
<c:url var="psAction" value="/admin/user/UserAdmin.do">
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
<c:url var="sortAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<!-- FORM -->
<html:form action="/admin/user/Remove">
<c:if test="${not empty param.so}">
  <html:hidden property="so" value="${param.so}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <html:hidden property="sc" value="${param.sc}"/>
</c:if>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<tiles:insert definition=".admin.auth.functions"/>

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
                    href="/admin/user/UserAdmin.do?mode=view" paramId="u" paramProperty="id" />
    <display:column width="20%" property="emailAddress" title="admin.user.list.Email" autolink="true" />
    <display:column width="20%" property="department" title="admin.user.list.Department" />
  </display:table>

	<c:url var="newAction" value="/admin/user/UserAdmin.do">
		<c:param name="mode" value="new" />
	</c:url>

  <tiles:insert definition=".toolbar.list">
    <tiles:put name="listNewUrl" beanName="newAction"/>  
    <tiles:put name="deleteOnly"><c:out value="${!useroperations['createSubject']}"/></tiles:put>
    <tiles:put name="newOnly"><c:out value="${!useroperations['removeSubject']}"/></tiles:put>
    <tiles:put name="listItems" beanName="AllUsers"/>
    <tiles:put name="listSize" beanName="AllUsers" beanProperty="totalSize"/>
    <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>  
    <tiles:put name="pageNumAction" beanName="pnAction"/>    
    <tiles:put name="pageSizeAction" beanName="psAction" />
    <tiles:put name="defaultSortColumn" value="3"/>
  </tiles:insert>
  
</html:form>
<!-- /  -->
