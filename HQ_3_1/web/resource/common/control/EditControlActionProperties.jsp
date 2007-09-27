<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
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

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.Control.Properties.Title"/>
</tiles:insert>
<!--  /  -->

<!-- CONSTANT DEFINITIONS -->

<c:set var="formBean" value="${requestScope[\"org.apache.struts.taglib.html.BEAN\"]}"/>
<c:set var="instance" value="${requestScope[\"org.apache.struts.action.mapping.instance\"]}"/>
<c:set var="formName" value="${instance.name}"/>

<script src="<html:rewrite page="/js/"/>control_ControlActionProperties.js" type="text/javascript"></script>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="resource.server.Control.Properties.Label.ControlAction"/>
 </td>
<c:choose>
 <c:when test="${formBean.numControlActions == 0}">
 <td width="30%" class="BlockContent">
  <fmt:message key="resource.service.control.controllist.NoActions"/>
 </td>
 </c:when>
 <c:otherwise>
  <logic:messagesPresent property="controlAction">
  <td width="30%" class="ErrorField">
   <html:select property="controlAction">
    <html:option value="" key="resource.application.applicationProperties.Select"/>
    <html:optionsCollection property="controlActions" />
   </html:select>
     <span class="ErrorFieldContent">- <html:errors property="controlAction"/></span>
  </td>
  </logic:messagesPresent>
  <logic:messagesNotPresent property="controlAction">
  <td width="30%" class="BlockContent">
   <html:select property="controlAction">
    <html:option value="" key="resource.application.applicationProperties.Select"/>
    <html:optionsCollection property="controlActions" />
   </html:select>
  </td>
  </logic:messagesNotPresent>
  </c:otherwise>
 </c:choose>
  <td width="20%" class="BlockLabel">
   <fmt:message key="common.label.Description"/>
  </td>
  <logic:messagesPresent property="description">
    <td width="30%" class="ErrorField">
     <html:textarea cols="35" rows="3" property="description" />
      <span class="ErrorFieldContent">- <html:errors property="description"/></span>
    </td>
  </logic:messagesPresent>
  <logic:messagesNotPresent property="description">
    <td width="30%" class="BlockContent">
     <html:textarea cols="35" rows="3" property="description" /> 
    </td>
  </logic:messagesNotPresent>
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
 </tr>
</table>
