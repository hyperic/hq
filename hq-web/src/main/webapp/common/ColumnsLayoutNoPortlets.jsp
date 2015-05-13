<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

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
<tiles:importAttribute name="addFullName" ignore="true"/>
<table cellspacing="0" cellpadding="0" border="0" width="100%">
<tr>
	<td>
	
	<c:choose>
		<c:when test="${not empty addFullName}">
			<c:set var="fullName" value="${User.firstName} ${User.lastName}"/>
			<tiles:insertDefinition name=".page.title">
				<tiles:putAttribute name="titleKey"  value="${request.titleKey}" /> 
				<tiles:putAttribute name="titleName"  value="${fullName}" /> 
			</tiles:insertDefinition>
		</c:when>
		<c:otherwise>
			<tiles:insertDefinition name=".page.title">
				<tiles:putAttribute name="titleKey"  value="${request.titleKey}" /> 
			</tiles:insertDefinition>
		</c:otherwise>
	</c:choose>
			
	
		
	</td>
</tr>
<tr>
<!-- Content Block -->

  
  <td valign="top" width="100%">      

    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr>      
        <td valign="top"> 			
			<tiles:insertTemplate template="${request.content}" />			
        </td>        
      </tr>
    </table>             

  </td> 


</tr></table>

<!-- /Content Block -->
