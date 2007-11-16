<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
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


<tiles:importAttribute name="User" ignore="true"/>
<tiles:importAttribute name="mode" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="${param.mode}"/>
</c:if>

<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_NEW" var="MODE_NEW"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_REGISTER" var="MODE_REGISTER"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_VIEW" var="MODE_VIEW"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_EDIT" var="MODE_EDIT"/>

<%
  int textBoxSize;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE"))
    textBoxSize = 12;
  else
    textBoxSize = 14;
%>

<c:set var="textBoxSize">
<%= textBoxSize %>
</c:set>

<tiles:insert definition=".portlet.error"/>
<logic:messagesPresent property="exception.user.alreadyExists">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
    <td class="ErrorBlock" width="100%"><html:errors property="exception.user.alreadyExists"/></td>
    <td class="ErrorBlock" width="100%">
  </tr>
</table>
</logic:messagesPresent>
<!-- CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
 <tr class="BlockContent">  
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="common.label.Name"/>
  </td>
  <td width="30%" rowspan="2" class="BlockContent">
   <table cellpadding="0" cellspacing="0" border="0">
    <tr>
     <td>
      <fmt:message key="admin.user.generalProperties.First"/>
     </td>
     <td>
      <html:img page="/images/spacer.gif" width="5" height="1" border="0"/>
     </td>
     <td>
      <fmt:message key="admin.user.generalProperties.Last"/>
     </td>
    </tr>
    <tr>
     <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="5" border="0"/></td>
    </tr>
    <tr>
     <logic:messagesPresent property="firstName">
      <td class="ErrorField">
       <html:text size="${textBoxSize}" maxlength="50" property="firstName" tabindex="1"/><br>
        <span class="ErrorFieldContent">- <html:errors property="firstName"/></span>
      </td>
     </logic:messagesPresent>
     <logic:messagesNotPresent property="firstName">
      <td><html:text size="${textBoxSize}" maxlength="50" property="firstName" tabindex="1"/></td>
      </logic:messagesNotPresent>
      <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
      <logic:messagesPresent property="lastName">
        <td class="ErrorField">
          <html:text size="${textBoxSize}" maxlength="50" property="lastName" tabindex="2"/><br>
          <span class="ErrorFieldContent">- <html:errors property="lastName"/></span>
	</td>
	</logic:messagesPresent>
	<logic:messagesNotPresent property="lastName">
	<td><html:text size="${textBoxSize}" maxlength="50" property="lastName" tabindex="2"/></td>
	</logic:messagesNotPresent>
      </tr>
      </table>
    </td>
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="admin.user.generalProperties.Username"/></td>
   <c:choose>
   <c:when test="${mode eq MODE_EDIT || mode eq MODE_REGISTER}">
    <td width="30%" class="BlockContent"><c:out value="${User.name}"/><br>
     <c:if test="${mode eq MODE_EDIT}">
    <html:hidden property="name"/>
     </c:if> 
    </td>
   </c:when>
   <c:otherwise>   
    <logic:messagesPresent property="name">
     <td width="30%" class="ErrorField">
      <html:text size="31" maxlength="40" property="name" tabindex="8"/><br>
      <span class="ErrorFieldContent">- <html:errors property="name"/></span>
     </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="name">
     <td width="30%" class="BlockContent">
      <html:text size="31" maxlength="40" property="name" tabindex="8"/>
     </td>
    </logic:messagesNotPresent>
   </c:otherwise>
   </c:choose>
    </tr>
    <tr>
     <td width="20%" class="BlockLabel">&nbsp;</td>
     <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Phone"/></td>
     <logic:messagesPresent property="phoneNumber">
      <td width="30%" class="ErrorField"><html:text size="31" maxlength="50" property="phoneNumber" tabindex="9"/><br><span class="ErrorFieldContent">- <html:errors property="phoneNumber"/></span></td>
     </logic:messagesPresent>
     <td width="30%" class="BlockContent"><html:text size="31" maxlength="50" property="phoneNumber" tabindex="9"/></td>
     </tr>

    <tr>
  <c:choose>
   <c:when test="${mode eq MODE_NEW}">
     <td colspan="2">
      <c:set var="tmpu" value="${param.u}" />
      <tiles:insert page="/admin/user/UserPasswordForm.jsp">
       <tiles:put name="userId" beanName="tmpu"/>  
      </tiles:insert>
     </td>
   </c:when>
   <c:when test="${mode eq MODE_EDIT and User.hasPrincipal}">
     <td class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="common.label.Password"/></td>
      <td class="BlockContent"><span class="CaptionText">
	<fmt:message key="admin.user.generalProperties.ReturnTo"/>
	<html:link page="/admin/user/UserAdmin.do?mode=${MODE_VIEW}&u=${param.u}">
	 <fmt:message key="admin.user.generalProperties.ViewUser"/>
	</html:link>
	<fmt:message key="admin.user.generalProperties.ToAccess"/></span>
      </td>
    </c:when>
  </c:choose>
      <td class="BlockLabel" valign="top"><fmt:message key="admin.user.generalProperties.Department"/></td>
      <td class="BlockContent" valign="top"><html:text size="31" maxlength="50" property="department" tabindex="10"/></td>
	</tr>	 

    <tr>
     <td class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="admin.user.generalProperties.Email"/></td>
      <logic:messagesPresent property="emailAddress">
        <td class="ErrorField">
         <html:text size="31" property="emailAddress" tabindex="5"/><br>
         <span class="ErrorFieldContent">- <html:errors property="emailAddress"/></span>
        </td>
       </logic:messagesPresent>
       <logic:messagesNotPresent property="emailAddress">
        <td class="BlockContent"><html:text size="31" property="emailAddress" tabindex="5"/></td>
       </logic:messagesNotPresent>
    
    <td class="BlockLabel"><fmt:message key="admin.user.generalProperties.smsAddress"/></td>
    <td class="BlockContent"><html:text size="31" maxlength="50" property="smsAddress" tabindex="10"/></td>
    </tr>  
   <tr valign="top">
    <td class="BlockLabel"><fmt:message key="admin.user.generalProperties.Format"/></td>
    <td class="BlockContent">
       <html:radio property="htmlEmail" value="true"/>
       <fmt:message key="admin.user.generalProperties.format.HTML"/><br/>
       <html:radio property="htmlEmail" value="false"/>
       <fmt:message key="admin.user.generalProperties.format.TEXT"/>
    </td>
  <c:choose>
  <c:when test="${mode eq MODE_REGISTER}"> 
    <td class="BlockContent colspan="2">&nbsp;</td>
  </c:when>
  <c:otherwise>
    <td class="BlockLabel"><fmt:message key="admin.user.generalProperties.EnableLogin"/></td>
    <td class="BlockContent">
     <c:choose>
      <c:when test="${empty param.enableLogin}">
	<input type="radio" name="enableLogin" value="yes" checked="checked" tabindex="6"/>
      </c:when>
      <c:otherwise>
       <html:radio property="enableLogin" value="yes" tabindex="6"/>
      </c:otherwise>
     </c:choose>
     <fmt:message key="admin.user.generalProperties.enableLogin.Yes"/><br/>
     <html:radio property="enableLogin" value="no" tabindex="7"/>
     <fmt:message key="admin.user.generalProperties.enableLogin.No"/>
    </td>
  </c:otherwise>
  </c:choose>
   </tr>  

</table>

<c:if test="${mode eq MODE_EDIT}">         
 <html:hidden name="User" property="id" />
 <html:hidden property="u" value="${param.u}" />
</c:if>

<c:if test="${mode eq MODE_REGISTER}">         
 <html:hidden property="id" value="${User.id}" />
</c:if>
