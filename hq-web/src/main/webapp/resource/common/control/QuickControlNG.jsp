<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.group.QuickControl.Tab"/>
  <tiles:putAttribute name="subTitle" value="${tmpTitle}"/>
</tiles:insertDefinition>

<!--  GENERAL PROPERTIES CONTENTS -->

<s:form action="%{#attr.action}">

<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}"/>
<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}"/>
<s:hidden theme="simple" name="resourceType" value="%{#attr.entityId.type}"/>
<s:hidden theme="simple" name="resourceId" value="%{#attr.entityId.id}"/>

<input type="hidden" name="mode" value="${param.mode}"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="3" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
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
      
      <td width="5%" class="BlockContent">
      
	   
	    <s:select theme="simple" list="%{#attr.qForm.controlActions}"  name="resourceAction" >
		</s:select>
      </td>
     
      <td width="75%" class="BlockContent">
       <input type="image" name="ok" src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/>
      </td>
  </tr>
  <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Arguments"/></td>
      <td width="80%" class="BlockContent" colspan="2">
       <s:textfield  size="30" maxlength="250" name="arguments" tabindex="1" errorPosition="bottom"/>
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
 
      <img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/>
     </td>
    </tr>
    <tr>
     <td colspan="3" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
</table>
</s:form>
<!--  /  -->
