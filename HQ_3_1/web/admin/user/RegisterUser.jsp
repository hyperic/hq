<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_REGISTER" var="MODE_REGISTER"/>

<c:set var="User" value="${webUser}"/>

<html:form action="/admin/user/Register">

<tiles:insert definition=".page.title.admin.user">
  <tiles:put name="titleKey" value="admin.user.RegisterUserPageTitle"/>  
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="monitorBlockContainer">
  <tr>
    <td><fmt:message key="admin.user.generalProperties.WelcomeEtc"/></td>
  </tr>
</table>

&nbsp;<br>

<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>  
</tiles:insert>

<tiles:insert page="/admin/user/UserForm.jsp">
  <tiles:put name="User" beanName="User"/>
  <tiles:put name="mode" beanName="MODE_REGISTER"/>
</tiles:insert>

<tiles:insert definition=".form.buttons.logout"/>

<tiles:insert definition=".page.footer"/>

</html:form>
