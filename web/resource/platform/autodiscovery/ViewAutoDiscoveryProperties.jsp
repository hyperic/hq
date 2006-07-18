<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
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


<!-- we shouldn't be in this tile if we don't have a ScanState -->
<tiles:importAttribute name="scanstate" />

<!--  AUTO-DISCOVERY PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.AutoDiscoveryPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<!--  AUTO-DISCOVERY PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.autoDiscoveryProperties.ElapsedTime"/></td>
		<td width="30%" class="BlockContent"><c:out value="${scanstate.elapsedTimeStr}"/></td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.autoDiscoveryProperties.DateStarted"/></td>
		<td width="30%" class="BlockContent"><c:out value="${scanstate.startTimeStr}"/></td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel">&nbsp;</td>
		<td width="30%" class="BlockContent">&nbsp;</td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.autoDiscoveryProperties.DateCompleted"/></td>
        <c:choose>
            <c:when test="${scanstate.startTime > 0 && !scanstate.isDone }"> <!-- there's a scan -->
                <td width="30%" class="BlockContent"></td>
            </c:when>
            <c:otherwise>
                <td width="30%" class="BlockContent"><c:out value="${scanstate.endTimeStr}"/></td>
            </c:otherwise>
        </c:choose>            
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->

