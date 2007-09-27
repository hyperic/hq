<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<tiles:importAttribute name="resource"/>

<font class="PageTitleSmallText">
<c:choose>
  <c:when test="${isFavorite}">
    <html:link page="/resource/common/QuickFavorites.do?eid=${resource.entityId.appdefKey}&mode=remove"><fmt:message key="resource.common.quickFavorites.remove"/><html:img width="11" height="9" border="0" page="/images/title_arrow.gif"/>
    </html:link>
  </c:when> 
  <c:otherwise> 
    <html:link page="/resource/common/QuickFavorites.do?eid=${resource.entityId.appdefKey}&mode=add"><fmt:message key="resource.common.quickFavorites.add"/><html:img width="11" height="9" border="0" page="/images/title_arrow.gif"/>
    </html:link>
  </c:otherwise> 
</c:choose>
</font>
