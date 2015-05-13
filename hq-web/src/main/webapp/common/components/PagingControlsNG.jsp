<%@ page language="java"%>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>


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


<tiles:importAttribute name="listItems" ignore="true" />
<tiles:importAttribute name="listSize" ignore="true" />
<tiles:importAttribute name="pageSizeMenuDisabled" ignore="true" />
<tiles:importAttribute name="pageSizeParam" ignore="true" />
<tiles:importAttribute name="pageSizeAction" ignore="true" />
<tiles:importAttribute name="pageNumParam" ignore="true" />
<tiles:importAttribute name="pageNumAction" ignore="true" />
<tiles:importAttribute name="defaultSortColumn" ignore="true" />

<c:if test="${empty pageSizeParam}">
	<c:set var="pageSizeParam" value="ps" />
</c:if>
<c:if test="${empty defaultSortColumn}">
  	<c:set var="defaultSortColumn" value="1" />
</c:if>
<c:if test="${empty pageNumParam}">
  	<c:set var="pageNumParam" value="pn" />
</c:if>
<c:set var="pageNumber" value="${param[pageNumParam]}" />

<td width="100%">
	<table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
  		<tr>
    		<c:choose>
      			<c:when test="${empty pageSizeMenuDisabled}">
        			<td width="100%" align="right" nowrap>
        				<b style="padding-right: 10px;">
        					<fmt:message key="ListToolbar.Total" />&nbsp;<span id="pagingTotal"><c:out value="${listSize}" default="0" /></span>
        				</b>
        			</td>
        			<td align="right" nowrap>
        				<b style="padding-right: 10px;">
        					<fmt:message key="ListToolbar.ItemsPerPageLabel" />
        				</b>
        			</td>
        			<td>
					<!-- TODO 
						1. take care of click onchange="goToSelectLocation(this, 'pageSizeParam',  'pageSizeAction')   
						2. make sure listSize received properly
					-->
					
						<s:select   name="pagingSelect"  value="15"     
						list="#{'15':getText('ListToolbar.ItemsPerPage.15'),'30':getText('ListToolbar.ItemsPerPage.30'),'50':getText('ListToolbar.ItemsPerPage.50'),'100':getText('ListToolbar.ItemsPerPage.100'),'250':getText('ListToolbar.ItemsPerPage.250'),'500':getText('ListToolbar.ItemsPerPage.500') }"/>
						
        			</td>
      			</c:when>
      			<c:otherwise>
        			<td width="100%" align="right">&nbsp;</td>
      			</c:otherwise>
    		</c:choose>
    		<td>
      			<hq:paginate action="${pageNumAction}" items="${listItems}"
        					 defaultSortColumn="${defaultSortColumn}" listTotalSize="${listSize}"
        					 pageValue="${pageNumParam}" pageNumber="${pageNumber}"
        					 pageSizeValue="${pageSizeParam}" />
    		</td>
  		</tr>
	</table>
</td>