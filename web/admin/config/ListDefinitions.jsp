<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="display" prefix="display" %>
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


<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listAlerts"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  

function setActiveInactive() {
    document.RemoveConfigForm.setActiveInactive.value='y';
    document.RemoveConfigForm.submit();
}
</script>

<hq:pageSize var="pageSize"/>
<c:url var="pnAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="psAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>
<c:set var="newAction" value="/alerts/Config.do?mode=new&aetid=${param.aetid}"/>

<c:set var="tmpTitle" value=".page.title.resource.${section}"/>
<tiles:insert beanName="tmpTitle">
  <tiles:put name="titleName"><html:link page="/admin/config/Config.do?mode=monitor"><fmt:message key="admin.home.ResourceTemplates"/></html:link> >
        <c:out value="${ResourceType.name}"/> <c:out value="${section}"/>s</tiles:put>
</tiles:insert>

<!-- FORM -->
<html:form action="/admin/alerts/RemoveConfig">
<html:hidden property="aetid"/>
<c:if test="${not empty param.so}">
  <html:hidden property="so" value="${param.so}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <html:hidden property="sc" value="${param.sc}"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><html:img page="/images/tt_error.gif" height="11" width="10" border="0" alt=""/></td>
    <td class="ErrorBlock" width="100%">
      <fmt:message key="admin.resource.alerts.Warning">
        <fmt:param value="${ResourceType.name}"/>
      </fmt:message>
    </td>
  </tr>
</table>

<display:table cellspacing="0" cellpadding="0" width="100%" action="${sortAction}"
               items="${Definitions}" >
  
  <display:column width="1%" property="id" 
                   title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
                   isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="definitions" onclick="ToggleSelection(this,widgetProperties)" styleClass="listMember"/>
  </display:column>
  <display:column width="20%" property="name" sort="true" sortAttr="1"
                  defaultSort="true" title="alerts.config.DefinitionList.ListHeader.AlertDefinition" href="/alerts/Config.do?mode=viewDefinition&aetid=${param.aetid}" paramId="ad" paramProperty="id"/>
    
  <display:column width="20%" property="description"
                  title="common.header.Description" >
</display:column>

  <display:column width="20%" property="ctime" sort="true" sortAttr="2"
                  title="alerts.config.DefinitionList.ListHeader.DateCreated" >
<display:datedecorator/>
</display:column>
                  
  <display:column width="20%" property="enabled"
                  title="alerts.config.DefinitionList.ListHeader.Active">
    <display:booleandecorator flagKey="yesno"/>
</display:column>

</display:table>

  <tiles:insert definition=".toolbar.list">
    <tiles:put name="listNewUrl" beanName="newAction"/> 
    <tiles:put name="listItems" beanName="Definitions"/>
    <tiles:put name="listSize" beanName="Definitions" beanProperty="totalSize"/>
    <tiles:put name="pageNumAction" beanName="pnAction"/>
    <tiles:put name="pageSizeAction" beanName="psAction"/>
    <tiles:put name="defaultSortColumn" value="1"/>
    <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
    <tiles:put name="goButtonLink" value="javascript:setActiveInactive()"/>
  </tiles:insert>

<html:hidden property="setActiveInactive"/>
</html:form>
<!-- /  -->
 <tiles:insert definition=".page.footer"/>

