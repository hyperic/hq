<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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

<tiles:importAttribute name="portlet"/>

<html:link page="/ResourceHub.do?ff=" linkName="browseUrl" styleId="browseUrl" style="visibility:hidden;"></html:link>

<script type="text/javascript">
function requestAvailSummary<c:out value="${portlet.token}"/>() {
    var availResourcesUrl = "<html:rewrite page="/dashboard/ViewAvailSummary.do?token=${portlet.token}"/>"
	new Ajax.Request(availResourcesUrl, {method: 'get', onSuccess:showAvailSummary, onFailure :reportError});
}
onloads.push(requestAvailSummary<c:out value="${portlet.token}"/>);
</script>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.AvailSummary"/>
  <tiles:put name="subTitle" beanName="portlet" beanProperty="description"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />

  <c:if test="${not empty portlet.token}">
    <tiles:put name="adminToken" beanName="portlet" beanProperty="token"/>
    <c:set var="tableName" value="availTable${portlet.token}"/>
    <c:set var="noTableName" value="noAvailTable${portlet.token}"/>
  </c:if>
  <c:if test="${empty portlet.token}">
    <c:set var="tableName" value="availTable"/>
    <c:set var="noTableName" value="noAvailTable"/>
  </c:if>
  <tiles:put name="portletName"><c:out value="${portlet.fullUrl}"/></tiles:put>

</tiles:insert>
   <!-- Content Block  -->
    <table width="100%" border="0" cellspacing="0" cellpadding="0" id="<c:out value="${tableName}"/>" class="portletLRBorder">
      <tbody>
        <tr class="tableRowHeader">
          <th width="72%" class="tableRowInactive"><fmt:message key="dash.home.TableHeader.Type"/></th>
          <th align="center" colspan="2" class="tableRowInactive"><fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></th>
        </tr>
        <!-- table rows are inserted here dynamically -->
      </tbody>
    </table>
    <table width="100%" cellpadding="0" cellspacing="0" border="0" id="<c:out value="${noTableName}"/>" style="display:none;">
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
  </div>

