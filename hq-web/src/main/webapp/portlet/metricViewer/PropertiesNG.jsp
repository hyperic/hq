<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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

<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="resources"/>
<c:url var="selfAction" value="metricViewerPortletControl.action">
 	<c:if test="${not portletIdentityToken}">
 		<c:param name="token" value="${portletIdentityToken}"/>
 	</c:if>
</c:url>

<c:set var="listSize" value="${metricViewerList.getTotalSize()}" />
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();

	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');

	var help = '<hq:help/>';

	/***********************************************/
	/* Disable "Enter" key in Form script- By Nurul Fadilah(nurul@REMOVETHISvolmedia.com)
	/* This notice must stay intact for use
	/* Visit http://www.dynamicdrive.com/ for full source code
	/***********************************************/
	
	function handleEnter (field, event) {
		var keyCode = event.keyCode ? event.keyCode : event.which ? event.which : event.charCode;
		
		if (keyCode == 13) {
			var i;
			
			for (i = 0; i < field.form.elements.length; i++) {
				if (field == field.form.elements[i]) break;
			}
			
			return false;
		}
		
		return true;
	}

	function selectValidOption() {
    	var sels = document.getElementsByTagName('select');
    
    	for (var i=0; i < sels.length; i++) {
      		while (sels[i].options[sels[i].selectedIndex].value == "-1") {
        		sels[i].selectedIndex++;
      		}
    	}
	}

	function submitMetricViewerForm() {
    	// selectValidOption();
    	MetricViewerForm.submit();
	}
</jsu:script>
<jsu:script onLoad="true">
	// selectValidOption();
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle">
    <td rowspan="99"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0" /></td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0" /></td>
    <td width="35%" class="PageTitle" nowrap><fmt:message key="dash.home.MetricViewer.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0" /></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="202" height="32" alt="" border="0" /></td>
    <td width="1%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0" /></td>
  </tr>
  
  <tr>
    <td valign="top" align="left" rowspan="99"></td>
	<td ><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0" /></td>
  </tr>
  <tr valign="top">
    <td colspan='3'>
	  <s:form action="updateMetricViewerModifyPortlet" name="MetricViewerForm">

	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.DisplaySettings"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
	  
	  <tiles:insertDefinition name=".ng.dashContent.admin.generalSettings">
        <tiles:putAttribute name="portletName" value="${portletName}"/>
      </tiles:insertDefinition>
	  
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
		   <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /></td>
        </tr>
         <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="common.label.Description"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
			  <s:textfield theme="simple" name="title" maxlength="50" onkeypress="return handleEnter(this, event);" disabled="%{#!attr.modifyDashboard}" value="%{#attr.titleDescription}" /> 
          </td>
        </tr>
         <tr valign="top">
          <td class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.MetricViewerRange"/></td>
             <td class="BlockContent" colspan="3" valign="center">
                 <fmt:message key="dash.settings.metricViewer.top"/>
                 &nbsp;
                 <c:choose>
                     <c:when test="${not sessionScope.modifyDashboard}">
                         <c:out value="${MetricViewerForm.numberToShow}"/>
                     </c:when>
                     <c:otherwise>
					  <s:select theme="simple" name="numberToShow" disabled="%{#!attr.modifyDashboard}"  list="#{ '5':'5', '10':'10', '20':'20','30':'30' }" value="%{#attr.numberToShow}"  />
                     </c:otherwise>
                 </c:choose>
                 &nbsp;
                 <fmt:message key="dash.settings.metricViewer.resources"/>
             </td>
        </tr>
        <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.ResourceType"/></td>
            <td width="80%" class="BlockContent" colspan="3" valign="center">
                <c:choose>
                    <c:when test="${not sessionScope.modifyDashboard}">
                        <c:out value="${MetricViewerForm.resourceType}"/>
                    </c:when>
                    <c:otherwise>
						<select name="resourceType" onchange="submitMetricViewerForm()">
						  <option value="-1" style="color: #CCC" disabled="true"><fmt:message key="dash.settings.metricViewer.selectResourceType"/></option>
						  <option value="-1" style="color: #CCC" disabled="true"><fmt:message key="dash.settings.metricViewer.platformTypes"/></option>
							  <c:forEach var="type" items="${platformTypes}">
								<option value="${type.appdefTypeKey}" 
								<c:if test="${type.appdefTypeKey == resourceType }">
								<c:out value="selected='selected'"/>
								</c:if> >${type.name}</option>
							</c:forEach>
							<option value="-1" style="color: #CCC" disabled="true">&nbsp;</option>	
							<option value="-1" style="color: #CCC" disabled="true"><fmt:message key="dash.settings.metricViewer.serverTypes"/></option>			
							  <c:forEach var="type" items="${serverTypes}">
								<option value="${type.appdefTypeKey}" 
								<c:if test="${type.appdefTypeKey == resourceType }">
								<c:out value="selected='selected'"/>
								</c:if> >${type.name}</option>
							  </c:forEach>
							<option value="-1" style="color: #CCC" disabled="true">&nbsp;</option>	
								<option value="-1" style="color: #CCC" disabled="true"><fmt:message key="dash.settings.metricViewer.serviceTypes"/></option>			
							  <c:forEach var="type" items="${serviceTypes}">
								<option value="${type.appdefTypeKey}" 
								<c:if test="${type.appdefTypeKey == resourceType }">
								<c:out value="selected='selected'"/>
								</c:if> >${type.name}</option>
							  </c:forEach>
						</select> 
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.Metric"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
              <c:choose>
                  <c:when test="${not sessionScope.modifyDashboard}">
                      <c:out value="${MetricViewerForm.metric}"/>
                  </c:when>
                  <c:otherwise>
					  <select name="metric" onchange="submitMetricViewerForm()">
						  <option value="-1" style="color: #CCC" disabled="true"><fmt:message key="dash.settings.metricViewer.selectMetric"/></option>
							  <c:forEach var="metricItem" items="${metrics}">
							      <c:if test="${metricItem.defaultOn}">
									<option value="${metricItem.id}" 
										<c:if test="${metric == metricItem.id}">
											<c:out value="selected='selected'"/>
										</c:if>
									>${metricItem.name}</option>
									</c:if>
							</c:forEach>
						</select> 
                  </c:otherwise>
              </c:choose>
          </td>
        </tr>
        <tr valign="top">
            <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.SortOrder"/></td>
            <td width="80%" class="BlockContent" colspan="3" valign="center">
                <c:choose>
                    <c:when test="${not sessionScope.modifyDashboard}">
                        <c:choose>
                        <c:when test="${MetricViewerForm.descending}">
                            <fmt:message key="dash.settings.metricViewer.descending"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="dash.settings.metricViewer.ascending"/>
                        </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
					  <select name="descending" >
						  <option value="true"><fmt:message key="dash.settings.metricViewer.descending"/></option>
						  <option value="false"><fmt:message key="dash.settings.metricViewer.ascending"/></option>
						</select> 
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
		   <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /> </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /> </td>
        </tr>
      </table>
      &nbsp;<br>

	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.SelectedResources"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>	  
	  
      <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfAction}"
                     pageSize="${pageSize}" items="${metricViewerList}" var="resource"  >

        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
          <display:checkboxdecorator name="ids" value="${resource.entityId.type}:${resource.id}" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>

        <display:column width="50%" property="name" sort="true" sortAttr="7"
                  defaultSort="true" title="dash.settings.ListHeader.Resource" />

        <display:column width="50%" property="description"
                        title="common.header.Description" />

      </display:table>


      <c:url var="addToListUrl" value="metricViewerAddResourcesPortletControl.action" >
          <c:param name="mode" value="metricViewerAddResources"/>
          <c:if test="${not empty portletIdentityToken}">
            <c:param name="key" value=".ng.dashContent.metricviewer.resources${portletIdentityToken}"/>
            <c:param name="token" value="${portletIdentityToken}"/>
          </c:if>
          <c:if test="${empty portletIdentityToken}">
            <c:param name="key" value=".ng.dashContent.metricviewer.resources"/>
          </c:if>
          <c:param name="ff" value="${appdefType}"/>
          <c:param name="ft" value="${resourceType}"/>
      </c:url>

      <c:choose>
          <c:when test="${not sessionScope.modifyDashboard}">
           
          </c:when>
          <c:otherwise>
			  <tiles:insertDefinition name=".ng.toolbar.addToList">
                      <tiles:putAttribute name="addToListUrl" value="${addToListUrl}"  />
                      <tiles:putAttribute name="listItems" value="${metricViewerList}"/>
                      <tiles:putAttribute name="listSize" value="${listSize}" />
                      <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
                      <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
                      <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
                      <tiles:putAttribute name="defaultSortColumn" value="1"/>
               </tiles:insertDefinition>
          </c:otherwise>
      </c:choose>
	  <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelMetricViewerModifyPortlet" />
			<tiles:putAttribute name="resetAction"   value="resetMetricViewerModifyPortlet" />
		  </c:if>
      </tiles:insertDefinition>
      <s:hidden theme="simple" name="token"/>
      </s:form>

    </td>
  </tr>
  <tr>
    <td colspan="4"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0" /></td>
  </tr>
</table>

