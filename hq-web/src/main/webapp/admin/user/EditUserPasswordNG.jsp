<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<c:set var="fullName" value="${User.firstName} ${User.lastName}"/>
<tiles:importAttribute name="administrator" ignore="true"/>
<!-- FORM -->
<s:form action="editPassword">  

  <!--  PAGE TITLE 
  <tiles:insertDefinition name=".page.title.admin.user.view">
    <tiles:putAttribute name="titleKey" value="common.title.Edit"/>  
    <tiles:putAttribute name="titleName" value="fullName"/>   
  </tiles:insertDefinition>
    -->

  <!--  HEADER TITLE -->
  <tiles:insertDefinition name=".header.tab">  
    <tiles:putAttribute name="tabKey" value="admin.user.changePassword.ChangePasswordTab"/>  
  </tiles:insertDefinition>

  <!--  /  -->
  <c:set var="tmpu" value="${param.u}" scope="request"/>
  <c:if test="${empty tmpu}">
	<c:set var="tmpu" value="${User.id}" scope="request"/>
  </c:if>
  <c:set var="userId" value="${tmpu}" scope="request"/>
  
  <table width="100%" cellspacing="0" class="TableBottomLine">
    <tr>
    <td width="50%" class="BlockContent" style="padding-bottom: 10px;">
  <tiles:insertTemplate template="/admin/user/UserPasswordFormNG.jsp">
    <tiles:putAttribute name="userId" value="tmpu"/>    
    <tiles:putAttribute name="administrator" value="administrator"/>    
  </tiles:insertTemplate>
    </td>
    <td width="50%" class="BlockContent">&nbsp;</td>
    </tr>
  </table>

  <tiles:insertDefinition name=".form.buttons">
	<tiles:putAttribute name="cancelAction"  value="cancelPassword" />
	<tiles:putAttribute name="resetAction"  value="resetPassword" />
  </tiles:insertDefinition>

  <tiles:insertDefinition name=".page.footer"/>

</s:form>
<!-- /  -->
