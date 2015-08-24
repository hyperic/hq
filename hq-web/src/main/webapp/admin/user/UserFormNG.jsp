<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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

<c:if test="${empty userId}">
  <c:set var="userId" value="${param.u}" scope="request"/>
</c:if>
<c:if test="${empty userId}">
	<c:set var="userId" value="${User.id}" scope="request"/>
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

<tiles:insertDefinition name=".portlet.error"/>
<s:hidden theme="simple" name="mode" value="%{mode}"/>

<!-- CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
 	<tr class="BlockContent">  
  		<td width="20%" class="BlockLabel">
   			<img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
   			<fmt:message key="common.label.Name"/>
  		</td>
  		<td width="30%" rowspan="2" class="BlockContent">
   			<table cellpadding="0" cellspacing="0" border="0">
    			<tr>
     				<td>
      					<fmt:message key="admin.user.generalProperties.First"/>
     				</td>
     				<td style="padding-left: 5px;">
      					<fmt:message key="admin.user.generalProperties.Last"/>
     				</td>
    			</tr>
    			<tr>
     				<td><s:textfield size="%{textBoxSize}" maxlength="50" name="firstName" value="%{firstName}" tabindex="1" errorPosition="bottom"/></td>
     				<td style="padding-left: 5px;"><s:textfield size="%{textBoxSize}" maxlength="50" name="lastName" value="%{lastName}" tabindex="2" errorPosition="bottom"/></td>
					
      			</tr>
      		</table>
    	</td>
    	<td width="20%" class="BlockLabel">
    		<img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
    		<fmt:message key="admin.user.generalProperties.Username"/>
    	</td>
   		<c:choose>
   			<c:when test="${mode eq MODE_EDIT || mode eq MODE_REGISTER}">
    			<td width="30%" class="BlockContent">
    				<c:out value="${User.name}"/><br>
     				<c:if test="${mode eq MODE_EDIT}">
    					<s:hidden theme="simple" name="name" value="%{#attr.User.name}"/>
					</c:if> 
    			</td>
   			</c:when>
   			<c:otherwise>   
				<td width="30%" class="BlockContent">
					<s:textfield size="31" maxlength="40" name="name" value="%{name}" tabindex="8" errorPosition="bottom"/>
				</td>
			</c:otherwise>
   		</c:choose>
    </tr>
    <tr>
     	<td width="20%" class="BlockLabel">&nbsp;</td>
     	<td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Phone"/></td>
     	
     	<td width="30%" class="BlockContent"><s:textfield size="31" maxlength="50" name="phoneNumber" value="%{phoneNumber}" tabindex="9" errorPosition="bottom"/></td>
	</tr>
    <tr>
  		<c:choose>
   			<c:when test="${mode eq MODE_NEW}">
     			<td colspan="2">
      				<c:set var="tmpu" value="${param.u}" />
      				<tiles:insertTemplate template="/admin/user/UserPasswordFormNG.jsp">
       					<tiles:putAttribute name="userId" value="${tmpu}"/>  
      				</tiles:insertTemplate>
     			</td>
   			</c:when>
   			<c:when test="${mode eq MODE_EDIT and User.hasPrincipal}">
     			<td class="BlockLabel">
     				<img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
     				<fmt:message key="common.label.Password"/>
     			</td>
      			<td class="BlockContent">
      				<span class="CaptionText">
						<fmt:message key="admin.user.generalProperties.ReturnTo"/>
						<s:a action="viewUser">
							<s:param name="mode" value="%{#attr.MODE_VIEW}"/>
							<s:param name="u" value="%{#attr.userId}"/>
	 						<fmt:message key="admin.user.generalProperties.ViewUser"/>
						</s:a>
						<fmt:message key="admin.user.generalProperties.ToAccess"/>
					</span>
      			</td>
    		</c:when>
    		<c:otherwise>
      			<td class="BlockContent" colspan="2">&nbsp;</td>
    		</c:otherwise>
  		</c:choose>
      	<td class="BlockLabel" valign="top">
      		<fmt:message key="admin.user.generalProperties.Department"/>
      	</td>
      	<td class="BlockContent" valign="top">
      		<s:textfield size="31" maxlength="50" name="department" value="%{department}" tabindex="10" errorPosition="bottom"/>
      	</td>
	</tr>	 
    <tr>
     	<td class="BlockLabel">
     		<img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
     		<fmt:message key="admin.user.generalProperties.Email"/>
     	</td>
	
		<td class="BlockContent">
			<s:textfield size="31" name="emailAddress" value="%{emailAddress}" tabindex="5" errorPosition="bottom"/>
		</td>
	
    	<td class="BlockLabel"><fmt:message key="admin.user.generalProperties.smsAddress"/></td>
    	<td class="BlockContent"><s:textfield size="31" maxlength="50" name="smsAddress" value="%{smsAddress}" tabindex="10" errorPosition="bottom"/></td>
    </tr>  
   	<tr valign="top">
    	<td class="BlockLabel"><fmt:message key="admin.user.generalProperties.Format"/></td>
    	<td class="BlockContent">   		
			<s:radio  list="#{'true':getText('admin.user.generalProperties.format.HTML') + '<br/>', 'false':getText('admin.user.generalProperties.format.TEXT')}" name="htmlEmail" value="%{htmlEmail}"/>
    	</td>
  		<c:choose>
  			<c:when test="${mode eq MODE_REGISTER}"> 
    			<td class="BlockContent" colspan="2">&nbsp;</td>
  			</c:when>
  			<c:otherwise>
    			<td class="BlockLabel"><fmt:message key="admin.user.generalProperties.EnableLogin"/></td>
    			<td class="BlockContent">
					<s:radio  list="#{'yes':getText('admin.user.generalProperties.enableLogin.Yes') + '<br/>', 'no':getText('admin.user.generalProperties.enableLogin.No')}" name="enableLogin" value="%{enableLogin}"/>
    			</td>
  			</c:otherwise>
  		</c:choose>
   </tr>  
</table>

<c:if test="${mode eq MODE_EDIT}">         
 	<s:hidden theme="simple" name="id" id="id" value="%{#attr.User.id}"/>
 	<s:hidden theme="simple" name="u" id="u" value="%{#attr.userId}" />
</c:if>

<c:if test="${mode eq MODE_REGISTER}">         
 	<s:hidden theme="simple" name="id" value="%{User.id}" />
</c:if>