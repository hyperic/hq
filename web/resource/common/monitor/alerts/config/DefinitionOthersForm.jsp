<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<%-- end vit: delete this block --%>

<!-- Content Block Title: Properties -->

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.NotifyOR.Label.Email"/></td>
    <logic:messagesPresent property="emailAddresses">
    <td width="80%" class="ErrorField">
      <html:textarea cols="80" rows="3" property="emailAddresses"/><br>
      <span class="ErrorFieldContent"><html:errors property="emailAddresses"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="emailAddresses">
    <td width="80%" class="BlockContent">
      <html:textarea cols="80" rows="3" property="emailAddresses"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="80%" class="BlockContentSmallText"><fmt:message key="alert.config.NotifyOR.TinyText"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<tiles:insert definition=".form.buttons"/>
<html:hidden property="ad"/>
  <c:choose>
    <c:when test="${not empty param.aetid}">
<html:hidden property="aetid"/>
    </c:when>
    <c:otherwise>
<html:hidden property="eid"/>
    </c:otherwise>
  </c:choose>

