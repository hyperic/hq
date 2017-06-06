<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


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


<tiles:importAttribute name="defaultKey"/>
<tiles:importAttribute name="optionsProperty"/>
<tiles:importAttribute name="labelProperty" ignore="true"/>
<tiles:importAttribute name="valueProperty" ignore="true"/>
<tiles:importAttribute name="filterParam" ignore="true"/>
<tiles:importAttribute name="filterAction"/>


<c:if test="${empty labelProperty}">
  <c:set var="labelProperty" value="label"/>
</c:if>
<c:if test="${empty valueProperty}">
  <c:set var="valueProperty" value="value"/>
</c:if>
<c:if test="${empty filterParam}">
  <c:set var="filterParam" value="f"/>
</c:if>

<c:set var="selectValue" value=""/>
<c:if test="${not empty param.fs}">
	<c:if test="${not fn:contains(param.fs, '-1')}">
	   <c:set var="selectValue" value="${param.fs}"/>
	</c:if> 
</c:if>
<c:if test="${not empty param.resourceType}">
	<c:if test="${not fn:contains(param.resourceType, '-1')}">
	   <c:set var="selectValue" value="${param.resourceType}"/>
	</c:if> 
</c:if>

<!--  FILTER TOOLBAR  -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>		
    <td class="FilterLabelText" nowrap align="right" style="border-top: 1px solid #ABB1C7;"><fmt:message key="Filter.ViewLabel"/></td>
    <td class="FilterLabelText" width="100%" style="border-top: 1px solid #ABB1C7;">


	<s:select theme="simple" cssStyle="FilterFormText" name="ff" list="#attr.optionsProperty" 
			  onchange="goToSelectLocation(this, '%{#attr.filterParam}',  '%{#attr.filterAction}');"   size="1" 
			  listKey="id" listValue="name" headerKey="-1" headerValue="%{getText(#attr.defaultKey)}" value="%{#attr.selectValue}" />
	

    </td>
	
  </tr>
</table>
<!--  /  -->
