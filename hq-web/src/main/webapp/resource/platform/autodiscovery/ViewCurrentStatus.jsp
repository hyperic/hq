<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
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


<tiles:importAttribute name="scanstate" ignore="true"/>
<tiles:importAttribute name="selfAction" />

<c:url var="selfUrl" value="${selfAction}"/>

<c:if test="${scanstate.startTime > 0 && !scanstate.isDone }"> <!-- there's a scan -->
	<jsu:script>
	   	function changeUrl(newUrl) {
	    	window.location.href = newUrl;
	   	}
	
	   	setTimeout("changeUrl('<c:out value="${selfUrl}" escapeXml="false"/>')", 30000);   
	</jsu:script>
</c:if>
<!--  CURRENT STATUS TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.CurrentStatusTab"/>
</tiles:insert>
<!--  /  -->

<c:set var="entityId" value="${Resource.entityId}"/>
<c:url var="viewAutoInventoryResults" value="/resource/platform/AutoDiscovery.do">
	<c:param name="mode" value="viewResults"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${entityId.type}"/>
	<c:param name="aiPid" value="${AIPlatform.id}"/>
</c:url>

<html:form action="/resource/platform/autodiscovery/ScanControl"> 
<html:hidden property="rid"/>
<html:hidden property="type"/>

<!--  CURRENT STATUS CONTENTS -->
<c:choose>
<c:when test="${not empty scanstate}">
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.currentStatus.Status"/></td>
    <td width="30%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        
        <c:if test="${scanstate.startTime == 0}"> <!-- no scan has been started -->
          <tr valign="middle">
            <td>&nbsp;</td>
            <td nowrap class="BlockLabel"><fmt:message key="resource.autodiscovery.currentStatus.NotStarted"/></td>
            <td width="100%">&nbsp;</td>
          </tr>
        </c:if>
        
        <c:if test="${scanstate.startTime > 0 && !scanstate.isDone }"> <!-- there's a scan -->
          <tr valign="middle">
            <td nowrap><html:img page="/images/status_bar.gif" width="50" height="12" border="0"/></td>
            <td class="DisplayLabel" width="100%" nowrap>
              <fmt:message key="resource.autodiscovery.currentStatus.InProgress"/>, 
              <html:link href="${viewAutoInventoryResults}">
                <fmt:message key="resource.autodiscovery.currentStatus.ViewResults"/>
              </html:link>
            </td>
            <td>
              <html:image page="/images/fb_abort.gif" border="0" titleKey="FormButtons.ClickToOk" property="abort" onmouseover="imageSwap(this, imagePath + 'fb_abort', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_abort', '');" onmousedown="imageSwap(this, imagePath +  'fb_abort', '_down')"/>
            </td>
          </tr>
        </c:if>
        <c:if test="${scanstate.startTime > 0 && scanstate.isDone }"> <!-- there's a scan -->
          <tr valign="middle">
            <c:choose>
            <c:when test="${scanstate.isInterrupted}">
            <td nowrap><html:img page="/images/status_error.gif" width="50" height="12" border="0"/>&nbsp;</td>
            <td nowrap class="BlockLabel">
              <fmt:message key="resource.autodiscovery.currentStatus.ScanInterrupted"/> 
            </td>
            </c:when>
            <c:otherwise>
            <td nowrap><html:img page="/images/status_complete.gif" width="50" height="12" border="0"/>&nbsp;</td>
            <td nowrap class="BlockLabel">
              <fmt:message key="resource.autodiscovery.currentStatus.ScanCompleted"/>, 
              <html:link href="${viewAutoInventoryResults}">
                <fmt:message key="resource.autodiscovery.currentStatus.ViewResults"/>
              </html:link>
            </td>
            </c:otherwise>
            </c:choose>
            <td width="100%">&nbsp;</td>
          </tr>
        </c:if>
      </table>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.currentStatus.LastError"/></td>
    <td width="30%" class="BlockContent"><c:out value="${LastAIError.message}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.currentStatus.ElapsedTime"/></td>
    <td width="30%" class="BlockContent"><c:out value="${scanstate.elapsedTimeStr}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.autodiscovery.currentStatus.DateStarted"/></td>
    <td width="30%" class="BlockContent"><c:out value="${scanstate.startTimeStr}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:when>
<c:otherwise>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
   <td class="BlockContent" width="100%"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/>
     <i><fmt:message key="resource.platform.inventory.autoinventory.error.NoScanStatus"/></i>
   </td>
  </tr>
  <tr>
   <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:otherwise>
</c:choose>
<!--  /  -->
</html:form>
