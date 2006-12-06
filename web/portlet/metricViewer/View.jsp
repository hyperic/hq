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

<html:link page="/Resource.do?eid=" linkName="viewResUrl" styleId="viewResUrl" style="visibility:hidden;"></html:link>

<script type="text/javascript">
function requestMetricsResponse() {
var metricsUrl = "<html:rewrite page="/dashboard/ViewMetrics.do"/>"
	new Ajax.Request(metricsUrl, {method: 'get', onSuccess:showMetricsResponse, onFailure :reportError});
	}
onloads.push(requestMetricsResponse);
</script>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.MetricViewer"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

  <!-- JSON available at /dashboard/ViewMetrics.do -->
  
  <table class="table" width="100%" border="0" cellspacing="0" cellpadding="0" id="metricTable" >
  	<tbody>
	<tr class="tableRowHeader">
		<th width="90%" class="tableRowInactive" id="resourceNameType" nowrap>
		Resource
		</th>
		<th width="10%" align="center" nowrap class="tableRowInactive" id="resourceLoadType">
		Load Avg
		</th>
	</tr>
	
 <!-- table rows are inserted here dynamically -->
 	</tbody>
 </table>
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="noMetricValues" style="display:none;">
    <tr class="ListRow">
      <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
    </tr>
  </table>

</div>
