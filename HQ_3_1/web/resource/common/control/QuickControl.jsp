<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
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

<c:set var="entityId" value="${Resource.entityId}"/>

<tiles:importAttribute name="action" />

<c:set var="tmpTitle"> <fmt:message key="resource.group.QuickControl.Caption"/></c:set>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.QuickControl.Tab"/>
  <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>

<!--  GENERAL PROPERTIES CONTENTS -->

<html:form action="${action}">
<html:hidden property="type" value="${entityId.type}"/>
<html:hidden property="rid" value="${entityId.id}"/>
<html:hidden property="resourceType" value="${entityId.type}"/>
<html:hidden property="resourceId" value="${entityId.id}"/>
<html:hidden property="mode" value="${param.mode}"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="3" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
    <tr valign="top">
<c:choose>
 <c:when test="${QuickControlForm.numControlActions == 0}">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Action"/></td>
    <td width="50%" class="ErrorField"><fmt:message key="resource.service.control.controllist.NoActions"/></td>
    <td width="30%" class="BlockContent" colspan="1">&nbsp;</td>
  </tr>
 </c:when>
 <c:otherwise>
  <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Action"/></td>
      <logic:messagesPresent property="resourceAction">
      <td width="5%" class="ErrorField">
       <html:select property="resourceAction">
        <html:option value="" key="resource.application.applicationProperties.Select"/>
        <html:optionsCollection property="controlActions" />
       </html:select>
       <span class="ErrorFieldContent">- <html:errors property="resourceAction"/></span>
      </td>
      </logic:messagesPresent>
      <logic:messagesNotPresent property="resourceAction">
      <td width="5%" class="BlockContent">
       <html:select property="resourceAction">
        <html:option value="" key="resource.application.applicationProperties.Select"/>
        <html:optionsCollection property="controlActions" />
       </html:select>
      </td>
     </logic:messagesNotPresent>
      <td width="75%" class="BlockContent">
       <html:image property="ok" page="/images/dash-button_go-arrow.gif" border="0"/>
      </td>
  </tr>
  <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Arguments"/></td>
      <td width="80%" class="BlockContent" colspan="2">
        <html:text size="30" maxlength="250" property="arguments" tabindex="1"/>
      </td>
  </tr>
 </c:otherwise>
</c:choose>
    <tr valign="top">
      <td width="20%" class="BlockLabel">&nbsp;</td>
      <td width="80%" class="BlockContentSmallText" colspan="2">
       <fmt:message key="resource.group.QuickControl.Content.Caption"/>
      </td>
    </tr>
    <tr>
     <td colspan="3" class="BlockContent">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
     </td>
    </tr>
    <tr>
     <td colspan="3" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
</html:form>
<!--  /  -->
