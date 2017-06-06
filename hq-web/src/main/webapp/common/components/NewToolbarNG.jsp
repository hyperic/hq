<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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


<%-- removed for later consideration 
<tiles:importAttribute name="newButtonKey" ignore="true"/>
--%>
<tiles:importAttribute name="useFromSideBar" ignore="true"/>
<tiles:importAttribute name="useToSideBar" ignore="true"/>
<tiles:importAttribute name="listItems"/>
<tiles:importAttribute name="listSize"/>
<tiles:importAttribute name="pageSizeParam" ignore="true"/>
<tiles:importAttribute name="pageSizeAction"/>
<tiles:importAttribute name="pageNumParam" ignore="true"/>
<tiles:importAttribute name="pageNumAction"/>

      <!--  NEW TOOLBAR -->
      <table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
        <tr>

<c:if test="${not empty useToSideBar}">
          <td class="ListCellLineEmpty"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0" /></td>
</c:if>

<c:choose>
  <c:when test="${not empty newButtonKey}">
          <td><input type="image" src='<s:url value="/images/tbb_new.gif" />' border="0" property="create" titleKey="${newButtonKey}" /></td>
  </c:when>
  <c:otherwise>
          <td>&nbsp;</td>
  </c:otherwise>
</c:choose>

<tiles:insertDefinition name=".controls.paging">
  <tiles:putAttribute name="listItems" value="${listItems}"/>
  <tiles:putAttribute name="listSize" value="${listSize}"/>
  <tiles:putAttribute name="pageSizeMenuDisabled" value="true"/>
  <tiles:putAttribute name="pageSizeParam" value="${pageSizeParam}"/>
  <tiles:putAttribute name="pageSizeAction" value="${pageSizeAction}"/>
  <tiles:putAttribute name="pageNumParam" value="${pageNumParam}"/>
  <tiles:putAttribute name="pageNumAction" value="${pageNumAction}"/>  
</tiles:insertDefinition>

<c:if test="${not empty useFromSideBar}">
          <td class="ListCellLineEmpty"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0" /></td>
</c:if>
        </tr>
      </table>
      <!--  /  -->
