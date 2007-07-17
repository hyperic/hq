<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>
</tiles:insert>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Name"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.firstName} ${User.lastName}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Username"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.name}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Email"/></td>
    <td width="30%" class="BlockContent">
     <html:link href="mailto:${User.emailAddress}">
      <c:out value="${User.emailAddress}"/>
     </html:link>
      <fmt:message key="parenthesis">
        <c:choose>
        <c:when test="${User.htmlEmail}">
          <fmt:param><fmt:message key="admin.user.generalProperties.format.HTML"/></fmt:param>
        </c:when>
        <c:otherwise>
          <fmt:param><fmt:message key="admin.user.generalProperties.format.TEXT"/></fmt:param>
        </c:otherwise>
        </c:choose>
      </fmt:message>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Phone"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.phoneNumber}"/></td>
  </tr>
  <tr>
<c:choose>
  <c:when test="${not User.hasPrincipal}">
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </c:when>
  <c:otherwise>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Password"/></td>
    <td width="30%" class="BlockContent">
     <html:link page="/admin/user/UserAdmin.do?mode=editPass&u=${User.id}">
      <fmt:message key="admin.user.generalProperties.Change"/>
     </html:link>
    </td>
  </c:otherwise>
</c:choose>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Department"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.department}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.EnableLogin"/></td>
    <td width="30%" class="BlockContent">
     <c:choose>
      <c:when test="${User.active}">
       <fmt:message key="admin.user.generalProperties.enableLogin.Yes"/>
      </c:when>
      <c:otherwise>
       <fmt:message key="admin.user.generalProperties.enableLogin.No"/>
      </c:otherwise>
     </c:choose>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.smsAddress"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.smsaddress}"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<!--  GENERAL PROPERTIES TOOLBAR -->
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" value="/admin/user/UserAdmin.do?mode=edit"/>
  <tiles:put name="editParamName" value="u"/>
  <tiles:put name="editParamValue" beanName="User" beanProperty="id"/>
</tiles:insert>
<br>
