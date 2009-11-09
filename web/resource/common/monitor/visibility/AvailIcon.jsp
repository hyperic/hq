<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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

<tiles:importAttribute name="availability" ignore="true"/>
  <c:choose>
    <c:when test="${empty availability}">
      <html:img page="/images/icon_available_error.gif" border="0"
                width="12" height="12"/>
    </c:when>
    <c:when test="${availability == 0}">
      <html:img page="/images/icon_available_red.gif" border="0"
                width="12" height="12"/>
    </c:when>
    <c:when test="${availability == 1}">
      <html:img page="/images/icon_available_green.gif" border="0"
                width="12" height="12"/>
    </c:when>
    <c:when test="${availability == -0.01}">
      <html:img page="/images/icon_available_orange.gif" border="0"
                width="12" height="12"/>
    </c:when>
    <c:when test="${availability < 1 && availability > 0}">
      <html:img page="/images/icon_available_yellow.gif" border="0"
                width="12" height="12"/>
    </c:when>
    <c:otherwise>
      <html:img page="/images/icon_available_error.gif" border="0"
                width="12" height="12"/>
    </c:otherwise>
  </c:choose>

