<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.server.Control.Properties.Title"/>
</tiles:insertDefinition>
<!--  /  -->

<!-- CONSTANT DEFINITIONS -->

<c:set var="formBean" value="${requestScope[\"org.apache.struts.taglib.html.BEAN\"]}"/>
<c:set var="instance" value="${requestScope[\"org.apache.struts.action.mapping.instance\"]}"/>
<c:set var="formName" value="${instance.name}"/>
<jsu:importScript path="/js/control_ControlActionProperties.js" />

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel">
   <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
   <fmt:message key="resource.server.Control.Properties.Label.ControlAction"/>
 </td>
<c:choose>
 <c:when test="${formBean.numControlActions == 0}">
 <td width="30%" class="BlockContent">
  <fmt:message key="resource.service.control.controllist.NoActions"/>
 </td>
 </c:when>
 <c:otherwise>
  			<s:if test="fieldErrors.containsKey('controlAction')">    
				<td width="30%" class="ErrorField">
			</s:if>
  			<s:else>    
				<td width="30%" class="BlockContent">
			</s:else>
	
<%--<s:set var="actions" value="#{
	'start':'Start',
	'stop':'Stop',
	'restart':'Restart'
}"/>--%> 
  <s:select name="controlAction" value="%{#attr.cForm.controlAction}" headerKey="" headerValue="%{getText('resource.application.applicationProperties.Select')}" list="%{#attr.availableActions}" errorPosition="bottom" />
<%--<select name='controlAction'>
								<option value="" ><fmt:message key="resource.application.applicationProperties.Select"/></option>
								<c:forEach var="action"  items="${cForm.controlActions}">
                                                                <option value="${action}" ><c:out value="${action}"/></option>
								</c:forEach>
                                  </select>--%>
  </td>
   </c:otherwise>
 </c:choose>
  <td width="20%" class="BlockLabel">
   <fmt:message key="common.label.Description"/>
  </td>
  
    <td width="30%" class="BlockContent">
     <s:textarea cols="35" rows="3" name="description" value="%{#attr.cForm.description}" errorPosition="bottom"/> 
    </td>
  
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
 </tr>
</table>
