<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<!-- 
    refer to server/src/org/apache/taglibs/display/TablePropertyTag.java
    for possible attributes to use, for now we'll start with the obvious
    ones
-->
<!-- list of data row objects/beans -->
<tiles:importAttribute name="items" />
<tiles:importAttribute name="tableComp" ignore="true" />
<tiles:importAttribute name="orderValue" ignore="true" />
<tiles:importAttribute name="principal" ignore="true" />
<!-- general table properties -->
<tiles:importAttribute name="width" ignore="true" />
<tiles:importAttribute name="border" ignore="true" />
<tiles:importAttribute name="cellspacing" ignore="true" />
<tiles:importAttribute name="cellpadding" ignore="true" />
<tiles:importAttribute name="styleClass" ignore="true" />

<!-- set defaults if they're not defined -->
<c:if test="${empty width}">
    <c:set var="width" value="100%" />
</c:if>
<c:if test="${empty border}">
    <c:set var="border" value="0" />
</c:if>
<c:if test="${empty cellspacing}">
    <c:set var="cellspacing" value="0" />
</c:if>
<c:if test="${empty cellpadding}">
    <c:set var="cellpadding" value="0" />
</c:if>
<c:if test="${empty styleClass}">
    <c:set var="styleClass" value="table" />
</c:if>
<!-- / -->
<table class="<c:out value="${styleClass}" />" width="<c:out value="${width}" />" border="<c:out value="${border}" />" cellspacing="<c:out value="${cellspacing}" />" cellpadding="<c:out value="${cellpadding}" />">
<tr class="tableRowHeader">
<c:choose>
<c:when test="${empty principal}">
 <c:choose>
 <c:when test="${empty tableComp}">
 <tiles:insert definition=".table.rows">
 </tiles:insert>
 </c:when>
 <c:otherwise>
 <tiles:insert attribute="tableComp" />
 </c:otherwise>
 </c:choose>
</c:when>
<c:otherwise>
 <c:choose>
 <c:when test="${empty tableComp}">
 <tiles:insert definition=".table.rows">
  <tiles:put name="principalBean" beanName="principal"/>
 </tiles:insert>
 </c:when>
 <c:otherwise>
 <tiles:insert attribute="tableComp">
  <tiles:put name="principalBean" beanName="principal"/>
 </tiles:insert>
 </c:otherwise>
 </c:choose>
</c:otherwise>
</c:choose>
</tr>
<c:set var="iteration" value="0" />
<c:forEach var="row" items="${items}">
  <c:choose>
    <c:when test="${ iteration % 2 == 1 }">
      <c:set var="rowStyle" value="tableRowEven" />
    </c:when>
    <c:otherwise>
      <c:set var="rowStyle" value="tableRowOdd" />
    </c:otherwise>
  </c:choose>
<tr class="<c:out value="${rowStyle}" />">
<c:choose>
<c:when test="${empty tableComp}">
<tiles:insert definition=".table.rows">
  <tiles:put name="rowBean" beanName="row"/>
</tiles:insert>
</c:when>
<c:otherwise>
<tiles:insert attribute="tableComp">
  <tiles:put name="rowBean" beanName="row"/>
</tiles:insert>
</c:otherwise>
</c:choose>

</tr>
</c:forEach>
</table>

