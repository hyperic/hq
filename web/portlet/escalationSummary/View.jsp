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

<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>

<%--<c:set var="rssUrl" value="/rss/ViewResourceHealth.rss"/>--%>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.EscalationSummary"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="rssBase" beanName="rssUrl" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

<html:link page="/Resource.do?eid=" linkName="viewResUrl" styleId="viewEscUrl" style="visibility:hidden;"></html:link>

<script type="text/javascript">
function requestEscalationSummary() {
	var escUrl = "<html:rewrite page="/dashboard/ViewEscalationSummary.do"/>"
	new Ajax.Request(escUrl, {method: 'get', onSuccess:showEscalationResponse, onFailure :reportError});
}
onloads.push(requestEscalationSummary);
</script>
    <div id="logInfo">
  </div>
 <table width="100%" border="0" cellspacing="0" cellpadding="0" id="escalationTable" class="portletLRBorder">
 	<tbody>
        <tr class="tableRowHeader">
            <th width="40%" class="tableRowInactive">
                Problem Resource
            </th>
            <th width="30%"  class="tableRowInactive" noWrap>
                Alert Name
            </th>
            <th width="10%" class="tableRowInactive" align="center">

                Time Elapsed
            </th>
            <th width="10%" align="center" class="tableRowInactive" noWrap>
                Next Action Starts
            </th>
            <th width="10%" align="center" class="tableRowInactive" noWrap>
                Ack'd
            </th>

        </tr>
        <!--<tr class="tableRowOdd">
            <td class="resourceTypeName">JBoss</td>
            <td class="resourceTypeName"><img src="/web/images/comment.gif" onmouseover="menuLayers.show('fakelog_menu2', event)" onmouseout="menuLayers.hide()" border="0" align="top"> Availability</td>
            <td class="throughput" align="center">22:00</td>

            <td class="throughput" align="center">05:00</td>
            <td class="throughput" align="center">Yes</td>

        </tr>-->
        <!-- table rows are inserted here dynamically -->
    </tbody>
	</table>
	<table width="100%" cellpadding="0" cellspacing="0" border="0" id="noEscResources" style="display:none;" class="portletLRBorder">
        <tbody>
        <tr class="ListRow">
            <td class="ListCell">
                <c:url var="path" value="/"/>
                <fmt:message key="dash.home.add.resources.to.display">
                  <fmt:param value="${path}"/>
                </fmt:message>
            </td>
        </tr>
        </tbody>
  </table>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">

          <tr>
              <td colspan="4" id="modifiedEscalationTime" class="modifiedDate">Updated: </td>
          </tr>

    </table>
</div>
