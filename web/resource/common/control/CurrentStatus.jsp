<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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

<%-- 
    Tile that displays the status of a control action.

    @param tabKey resource key for displaying as the table's header
    @param isDetail flag to display "delete current" or "view details" links
    @param section the control that this tile should be displayed as:
           server, service, control

--%>

<tiles:importAttribute name="tabKey" ignore="true"/>
<tiles:importAttribute name="isDetail" ignore="true"/>
<tiles:importAttribute name="section"/>

<c:if test="${empty tabKey}">
 <c:set var="tabKey" value="resource.server.ControlStatus.Title"/>
</c:if>

<!--  CURRENT STATUS TITLE -->
<c:choose>
 <c:when test="${empty isDetail}">
  <tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" beanName="tabKey"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" beanName="tabKey"/>
   <tiles:put name="tabName" beanName="controlCurrentStatus" beanProperty="action"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>
<!--  /  -->

<%-- pick image: completed/inprogress/error --%>
<c:choose>
 <c:when test="${controlStatus eq 'In Progress'}">
  <c:set var="statusImage" value="/images/status_bar.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Progress"/>
  <c:if test="${section eq 'group'}">
   <c:url var="detailsLink" value="/resource/${section}/Control.do">
    <c:param name="mode" value="crntDetail"/>
    <c:param name="eid" value="${Resource.entityId}"/>
    <c:param name="bid" value="${requestScope.bid}"/> 
   </c:url>
   <c:set var="statusMsg"><c:out value="${statusMsg}"/>&nbsp;<html:link href="${detailsLink}"><fmt:message key="resource.group.ControlStatus.Link.Details"/></html:link></c:set>
  </c:if>
  <script language="JavaScript1.2">
<!--
    ControlStatusUpdater = Class.create();

    ControlStatusUpdater.prototype = {
      initialize: function(container, options) {},

      ajaxUpdate: function(ajaxResponse) {
        var statusNode = ajaxResponse.childNodes[0];

        var status = statusNode.getAttribute("ctrlStatus");

        if (status != 'In Progress') {
          window.location.reload()
        }
        else {
          dojo.byId('ctrlAction').innerHTML = statusNode.getAttribute("ctrlAction");
          dojo.byId('ctrlDesc').innerHTML = statusNode.getAttribute("ctrlDesc");
          dojo.byId('ctrlStart').innerHTML = statusNode.getAttribute("ctrlStart");
          dojo.byId('ctrlSched').innerHTML = statusNode.getAttribute("ctrlSched");
          dojo.byId('ctrlDuration').innerHTML = statusNode.getAttribute("ctrlDuration");
        }
      }
    }

    function initControlStatusUpdate() {
      controlStatusUpdater = new ControlStatusUpdater();
      ajaxEngine.registerRequest( 'getUpdatedControlStatus', '<html:rewrite page="/resource/common/control/UpdateStatus.do"/>');
      ajaxEngine.registerAjaxObject( 'controlStatusUpdater', controlStatusUpdater  );
      updateControlStatus();
    }

    function updateControlStatus() {
      setTimeout( "updateControlStatus()", 5*1000 );

      ajaxEngine.sendRequest( 'getUpdatedControlStatus',
                              "eid=<c:out value="${Resource.entityId}"/>"
                            <c:if test="${section eq 'group'}">
                              ,"bid=<c:out value="${requestScope.bid}"/>"
                            </c:if>
                            );
    }

    onloads.push( initControlStatusUpdate );
  -->
  </script>
 </c:when>
 <c:when test="${controlStatus eq 'Failed'}">
  <c:set var="statusImage" value="/images/status_error.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Error"/>
 </c:when>
 <c:when test="${controlStatus eq 'Completed'}">
  <c:set var="statusImage" value="/images/status_complete.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Completed"/>
 </c:when>
</c:choose>

<!--  CURRENT STATUS CONTENTS -->
<c:choose>
<c:when test="${controlStatus eq 'Failed' || controlStatus eq 'In Progress' || controlStatus eq 'Completed' }">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Action"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlAction"><c:out value="${controlCurrentStatus.action}"/></span></td>
		<td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlDesc"><c:out value="${controlCurrentStatus.description}"/></span></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Status"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlImg"><html:img page="${statusImage}" width="50" height="12" border="0"/></span>&nbsp;<span id="ctrlStatus"><c:out value="${statusMsg}" escapeXml="false"/></span></td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Started"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlStart"><hq:dateFormatter value="${controlCurrentStatus.startTime}"/></span></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.ErrorDescr"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlMessage"><c:out value="${controlCurrentStatus.message}"/></span></td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Sched"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlSched"><hq:dateFormatter value="${controlCurrentStatus.dateScheduled}"/></span></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Elapsed"/></td>
		<td width="30%" class="BlockContent"><span id="ctrlDuration"><hq:dateFormatter time="true" value="${controlCurrentStatus.duration}"/></span></td>
		<td width="20%" class="BlockLabel">&nbsp;</td>
		<td width="30%" class="BlockContent">&nbsp;</td>
	</tr>
        <c:if test="${controlStatus eq 'Completed'}">
         <tr valign="top">
          <td width="20%" class="BlockLabel">&nbsp;</td>
           <c:choose>
            <c:when test="${empty isDetail}">
             <html:form action="/resource/${section}/control/RemoveCurrentStatus">
              <td width="80%" class="BlockContent" colspan="3"><html:link href="javascript:document.RemoveControlForm.submit()"><fmt:message key="resource.server.ControlStatus.Link.Clear"/></html:link></td>
              <html:hidden property="rid" value="${Resource.id}"/>
              <html:hidden property="type" value="${Resource.entityId.type}"/>
              <html:hidden property="controlActions" value="${requestScope.bid}" />              
             </html:form>
            </c:when>
            <c:otherwise>
             <td width="80%" class="BlockContent" colspan="3">&nbsp;</td>
            </c:otherwise>
           </c:choose>
         </tr>
        </c:if>
	</tr>
        <tr>
         <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
</table>
</c:when> <%-- end inprogress/error/completed block --%>
<c:otherwise>
 <table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
   <td class="BlockContent" width="100%"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/><i><fmt:message key="resource.server.ControlStatus.Content.None"/></i></td>
  </tr>
  <tr>
   <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:otherwise>
</c:choose>
<!--  /  -->

