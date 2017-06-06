<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
<jsu:importScript path="/js/functions.js" />

<link rel=stylesheet href="<s:url value="/css/win.css"/>" type="text/css">
<%-- end vit: delete this block --%>

<!-- Content Block Title: Properties -->

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.NotifyOR.Label.Email"/></td>
    
    <td width="80%" class="BlockContent">
      <s:textarea cols="80" rows="3" name="emailAddresses" id="emailAddresses" value="%{#attr.emailAddresses}" />
    </td>
    
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="80%" class="BlockContentSmallText"><fmt:message key="alert.config.NotifyOR.TinyText"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
</table>
<tiles:insertDefinition name=".form.buttons">
	<tiles:putAttribute name="cancelAction"  value="cancelAlertOtherAction" />
	<tiles:putAttribute name="resetAction"  value="resetAlertOtherAction" />
</tiles:insertDefinition>
<s:hidden theme="simple" name="ad" id="ad" value="%{#attr.ad}"/>
  <c:choose>
    <c:when test="${not empty param.aetid}">
<s:hidden theme="simple" name="aetid" id="aetid" value="%{#attr.aetid}"/>
    </c:when>
    <c:otherwise>
<s:hidden theme="simple" name="eid" id="eid" value="%{#attr.eid}"/>
    </c:otherwise>
  </c:choose>

