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
</tiles:insert>



<script type="text/javascript">
/*
function requestEscalationSummary() {
    var url = '<html:rewrite page="/escalation/ListActiveEscalations.do"/>';

	new Ajax.Request(url, {method: 'get', onSuccess:showEscalationResponse, onFailure :reportError});
}
onloads.push(requestEscalationSummary);
*/
</script>

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
        
        <!-- table rows are inserted here dynamically -->
    </tbody>
	</table>
	<table width="100%" cellpadding="0" cellspacing="0" border="0" id="noEscResources" style="display:none;" class="portletLRBorder">
        <tbody>
        <tr class="ListRow">
            <td class="ListCell">
                <fmt:message key="dash.home.alerts.no.resource.to.display"/>
            </td>
        </tr>
        </tbody>
  </table>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">

          <tr>
              <td colspan="4" id="modifiedEscalationTime" class="modifiedDate">Updated: </td>
          </tr>

    </table>

     <div id="logInfo"></div>
    <div style="display:none;">
        <span id="noWaitText"><fmt:message key="alert.config.escalation.end"/></span>
        <span id="fiveText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="5"/></fmt:message></span>
        <span id="tenText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="10"/></fmt:message></span>
        <span id="twentyText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="20"/></fmt:message></span>
        <span id="thirtyText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="30"/></fmt:message></span>
        <span id="fortyfiveText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="45"/></fmt:message></span>
        <span id="sixtyText"><fmt:message key="alert.config.escalation.wait"><fmt:param value="60"/></fmt:message></span>
    </div>
</div>
