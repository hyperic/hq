<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq"%>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display"%>
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

<jsu:importScript path="/js/listWidget.js"/>
<c:set var="widgetInstanceName" value="listAlerts" />
<jsu:script>
	var pageData = new Array();

	initializeWidgetProperties('${widgetInstanceName}');
	
	widgetProperties = getWidgetProperties('${widgetInstanceName}');  

	function setActiveInactive() {
    	document.RemoveConfigForm.setActiveInactive.value='y';
    	document.RemoveConfigForm.submit();
	}
</jsu:script>

<hq:pageSize var="pageSize" />

<c:url var="pnAction" value="/alerts/Config.do">
	<c:param name="mode" value="list" />
	<c:param name="aetid" value="${param.aetid}" />
	<c:if test="${not empty param.ps}">
		<c:param name="ps" value="${param.ps}" />
	</c:if>
	<c:if test="${not empty param.so}">
		<c:param name="so" value="${param.so}" />
	</c:if>
	<c:if test="${not empty param.sc}">
		<c:param name="sc" value="${param.sc}" />
	</c:if>
</c:url>
<c:url var="psAction" value="/alerts/Config.do">
	<c:param name="mode" value="list" />
	<c:param name="aetid" value="${param.aetid}" />
	<c:if test="${not empty param.ps}">
		<c:param name="pn" value="${param.pn}" />
	</c:if>
	<c:if test="${not empty param.so}">
		<c:param name="so" value="${param.so}" />
	</c:if>
	<c:if test="${not empty param.sc}">
		<c:param name="sc" value="${param.sc}" />
	</c:if>
</c:url>
<c:url var="sortAction" value="/alerts/Config.do">
	<c:param name="mode" value="list" />
	<c:param name="aetid" value="${param.aetid}" />
	<c:if test="${not empty param.pn}">
		<c:param name="pn" value="${param.pn}" />
	</c:if>
	<c:if test="${not empty param.ps}">
		<c:param name="ps" value="${param.ps}" />
	</c:if>
</c:url>
<c:url var="newAction" value="/alerts/Config.do">
	<c:param name="mode" value="new" />
	<c:if test="${not empty param.aetid}">
		<c:param name="aetid" value="${param.aetid}" />
	</c:if>
</c:url>
<c:url var="monitorAction" value="/admin/config/Config.do">
	<c:param name="mode" value="monitor" />
</c:url>

<c:set var="tmpTitle" value=".page.title.resource.${section}" />

<tiles:insert beanName="tmpTitle">
	<tiles:put name="titleName">
		<a href="${monitorAction}">
			<fmt:message key="admin.home.ResourceTemplates" />
		</a> &gt; ${ResourceType.name} ${section}s
  	</tiles:put>
</tiles:insert>

<html:form action="/admin/alerts/RemoveConfig">
	<html:hidden property="aetid" />
	<c:if test="${not empty param.so}">
		<html:hidden property="so" value="${param.so}" />
	</c:if>
	<c:if test="${not empty param.sc}">
		<html:hidden property="sc" value="${param.sc}" />
	</c:if>

	<table width="100%" cellpadding="0" cellspacing="0" border="0">
		<tr>
			<td class="ErrorBlock">
				<html:img page="/images/tt_error.gif" height="11" width="10" border="0" alt="" />
			</td>
			<td class="ErrorBlock" width="100%">
				<fmt:message key="admin.resource.alerts.Warning">
					<fmt:param value="${ResourceType.name}" />
				</fmt:message>
			</td>
		</tr>
	</table>

	<display:table cellspacing="0" cellpadding="0" width="100%" action="${sortAction}" items="${Definitions}">
		<display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
                   isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    		<display:checkboxdecorator name="definitions"
				onclick="ToggleSelection(this,widgetProperties)"
				styleClass="listMember" />
		</display:column>
		<display:column width="20%" property="name" sort="true" sortAttr="1"
			defaultSort="true"
			title="alerts.config.DefinitionList.ListHeader.AlertDefinition"
			href="/alerts/Config.do?mode=viewDefinition&aetid=${param.aetid}"
			paramId="ad" paramProperty="id" />
		<display:column width="20%" property="description"
			title="common.header.Description" />
		<display:column width="20%" property="ctime" sort="true" sortAttr="2"
			title="alerts.config.DefinitionList.ListHeader.DateCreated">
			<display:datedecorator />
		</display:column>
		<display:column width="20%" property="enabled"
			title="alerts.config.DefinitionList.ListHeader.Active">
			<display:booleandecorator flagKey="yesno" />
		</display:column>
	</display:table>

	<tiles:insert definition=".toolbar.list">
		<tiles:put name="listNewUrl" beanName="newAction" />
		<tiles:put name="listItems" beanName="Definitions" />
		<tiles:put name="listSize" beanName="Definitions" beanProperty="totalSize" />
		<tiles:put name="pageNumAction" beanName="pnAction" />
		<tiles:put name="pageSizeAction" beanName="psAction" />
		<tiles:put name="defaultSortColumn" value="1" />
		<tiles:put name="widgetInstanceName" beanName="widgetInstanceName" />
		<tiles:put name="goButtonLink" value="javascript:setActiveInactive()" />
	</tiles:insert>

	<html:hidden property="setActiveInactive" />
</html:form>
<tiles:insert definition=".page.footer" />