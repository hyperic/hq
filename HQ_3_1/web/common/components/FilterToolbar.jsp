<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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

<!--  FILTER TOOLBAR  -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
  	<td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>		
    <td class="FilterLabelText" nowrap align="right"><fmt:message key="Filter.ViewLabel"/></td>
    <td class="FilterLabelText" width="100%">
      <html:select property="f" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, '${filterParam}',  '${filterAction}');">
        <html:option value="-1" key="${defaultKey}"/>
        <html:optionsCollection property="${optionsProperty}" value="${valueProperty}" label="${labelProperty}"/>
      </html:select>
    </td>
  </tr>
</table>
<!--  /  -->
