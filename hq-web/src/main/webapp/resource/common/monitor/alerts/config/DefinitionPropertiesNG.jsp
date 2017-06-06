<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="tableBottomLine">
  <tr>
    <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <%-- name --%>
    <td width="20%" class="BlockLabel">
      <fmt:message key="common.label.Name"/>
    </td>
    
    
    <td width="30%" class="BlockContent">
      <s:textfield size="30" maxlength="255" name="name"  value="%{#attr.defForm.name}" errorPosition="bottom"/>
    </td>
    

    <%-- priority --%>
    <td width="20%" class="BlockLabel">
      <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9"
      border="0"/><fmt:message key="alert.config.props.PB.Priority"/>
    </td>
    
    <td width="30%" class="BlockContent">
      <s:select theme="simple" name="priority" value="%{#attr.defForm.priority}" list="%{#attr.defForm.prioritiesMap}" listValue="value" listKey="key" filter="true" />
    </td>
    
  </tr>
  
  <tr valign="top">
    <%-- description --%>
    <td width="20%" class="BlockLabel">
      <fmt:message key="common.label.Description"/>
    </td>
   
    <td width="30%" class="BlockContent">
      <s:textarea cols="40" rows="3" name="description" value="%{#attr.defForm.description}" errorPosition="bottom" />
    </td>
   

    <%-- active --%>
    <td width="20%" class="BlockLabel">
      <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9"
      border="0"/><fmt:message key="alert.config.props.PB.Active"/>
    </td>
    
    <td width="30%" class="BlockContent">
	  <c:if test="${defForm.active}">	
		<input type="radio" name="active" value= "true" checked="checked"/>
	  </c:if>
	  <c:if test="${not defForm.active}">	
		<input type="radio" name="active"  value="true"/>
	  </c:if>	
      <fmt:message key="alert.config.props.PB.ActiveYes"/><br>
      <c:choose>
      	<c:when test="${empty alertDef or alertDef.enabled}">
		  <c:if test="${defForm.active}">	
			<input type="radio" name="active" value= "false" />
		  </c:if>
		  <c:if test="${not defForm.active}">	
			<input type="radio" name="active"  value="false" checked="checked"/>
		  </c:if>	
	      
      	</c:when>
      	<c:otherwise>
      	  <fmt:message var="activeButDisabledWarningMsg" key="alert.config.props.PB.ActiveButDisabledWarning" />
	      <c:if test="${defForm.active}">	
			<input type="radio" name="active" value= "false" onchange="if (this.checked) { alert('%{#attr.defForm.activeButDisabledWarningMsg}'); }" />
		  </c:if>
		  <c:if test="${not defForm.active}">	
			<input type="radio" name="active"  value="false" checked="checked" onchange="if (this.checked) { alert('%{#attr.defForm.activeButDisabledWarningMsg}'); }"/>
		  </c:if>	

      	</c:otherwise>
      </c:choose>
      <fmt:message key="alert.config.props.PB.ActiveNo"/>
    </td>
    
  </tr>
  <tr>
    <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
</table>
&nbsp;<br>
