<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ page import="org.hyperic.hq.appdef.shared.AIQueueConstants" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
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


<div class="effectsPortlet">
<!-- Content Block Contents -->
<tiles:importAttribute name="resources"/>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.AutoDiscovery"/>  
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
</tiles:insert>
<jsu:script>
	// If the user has checked a server but not its platform, check the platform
	function setImportCheckboxes (cbform) {
	  var pcChecked = false;
	  for (var i=0; i<cbform.elements.length; i++) {
	    if (cbform.elements[i].name == 'platformsToProcess') {
	      pcChecked = cbform.elements[i].checked;
	      continue;
	    }
	
	    if (cbform.elements[i].name == 'serversToProcess' && 
	        cbform.elements[i].checked) {
	        cbform.elements[i].checked = pcChecked;
	    }
	  }
	}
	function setRemoveCheckboxes (cbform) {
	  var pc;
	  for (var i=0; i<cbform.elements.length; i++) {
	    if (cbform.elements[i].name == 'platformsToProcess') {
	      pc = cbform.elements[i];
	      continue;
	    }
	    /*
	    if (cbform.elements[i].name == 'serversToProcess' && 
	        pc.checked && !cbform.elements[i].checked) {
			return confirm("Warning: removing a platform will automatically remove all of its servers.\n\nClick OK to proceed.");
	    }
	    */
	  }
	  return true;
	}
	function addInventory() {
	    var formValue = '<hq:constant classname="org.hyperic.hq.appdef.shared.AIQueueConstants" symbol="Q_DECISION_APPROVE"/>';
	    AIQueueForm.queueAction.value = formValue;
	}
	
	function skipResources() {
	    var formValue = '<hq:constant classname="org.hyperic.hq.appdef.shared.AIQueueConstants" symbol="Q_DECISION_IGNORE"/>';
	    if(!setRemoveCheckboxes(AIQueueForm)) return false;
	    AIQueueForm.queueAction.value = formValue; 
	}
</jsu:script>
<tiles:insert definition=".portlet.error"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
    <c:choose>
      <c:when test="${empty resources}">
        <tr class="ListRow">
          <c:choose>
            <c:when test="${hasNoAgents}">
              <td class="ListCellHeader"><fmt:message key="dash.home.HQ.is.empty"/></td>
            </c:when>
            <c:otherwise>
              <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
            </c:otherwise>
          </c:choose>
        </tr>
      </c:when>
      <c:otherwise>
        <html:form action="/dashboard/ProcessAutoDiscovery">
        <html:hidden property="queueAction"/>
        <tr>
          <td class="ListHeaderInactive">&nbsp;</td>
          <td colspan="2" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.ResourceName"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
          <td class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Status"/></td>
          <td class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Changes"/></td>
        </tr>
        <c:forEach items="${resources}" var="resource">
          <tr class="ListRow">
            <td class="ListCell" nowrap="nowrap"><html:radio property="platformsToProcess" value="${resource.id}"/></td>
            <td colspan="2" class="ListCell">
              	<html:link action="/resource/platform/AutoDiscovery">
              		<html:param name="mode" value="results"/>
              		<html:param name="aiPid" value="${resource.id}"/>
                	${resource.name}
              	</html:link>
                <c:choose>
                  <c:when test="${empty resource.description}">
                  - <c:out value="${resource.platformTypeName}"/>
                  </c:when>
                  <c:otherwise>
                  - <c:out value="${resource.description}" />
                  </c:otherwise>
                </c:choose>
            </td>
            <td class="ListCell"><c:out value="${resource.queueStatusStr}"/></td>            
            <td class="ListCell"><hq:autoInventoryDiff resource="${resource}"/></td>
            <c:forEach items="${resource.AIServerValues}" var="server">
            <hq:skipIfAutoApproved aiserver="${server}">
              <c:if test="${server.queueStatus != 0 && !server.ignored}">
                <tr class="ListRow">
                  <td class="ListCell" nowrap="nowrap">&nbsp;&nbsp;&nbsp;<html:multibox property="serversToProcess" value="${server.id}"/></td>
                  <td class="ListCell">
                  	<hq:removePrefix property="serverNameNoPrefix" prefix="${resource.name}" value="${server.name}"/>
      				<hq:shortenText maxlength="32" value="${serverNameNoPrefix}" position="middle" styleClass="ListCellPopup5 "/>
				  </td>
                  <td class="ListCell">
                  	<hq:shortenPath value="${server.installPath}" preChars="20" postChars="25" styleClass="ListCellPopup5" />
                  </td>
                  <td class="ListCell"><c:out value="${server.queueStatusStr}"/></td>
                  <td class="ListCell"><hq:autoInventoryServerDiff resource="${server}"/></td>
                </tr>
              </c:if>
            </hq:skipIfAutoApproved>
            </c:forEach>
          </tr>    
        </c:forEach>
        <tr class="ToolbarContent">
          <td colspan="5" class="ListCell" nowrap>
            <table cellpadding="0" cellspacing="0" border="0">
             <tr>
                <td style="padding-right:10px;">
                <input type="hidden" name="temp" value="true" id="buttonActionHidden"/>
                <a class="buttonGreen" href="javascript:setImportCheckboxes(AIQueueForm); addInventory(); hyperic.form.mockLinkSubmit('buttonAction','Add to Inventory','buttonActionHidden')"><span style="white-space: nowrap"><fmt:message key="common.label.AddtoInventory"/></span></a>
                <a class="buttonGreen" href="javascript:skipResources(); hyperic.form.mockLinkSubmit('buttonAction','Skip Checked Resources','buttonActionHidden');"><span style="white-space: nowrap"><fmt:message key="common.label.SkipResources"/></span></a>
                </td>
            </tr>
           </table>
            </td>
        </tr>
        </html:form>
      </c:otherwise>
      
    </c:choose>  
</table>
<tiles:insert definition=".dashContent.seeAll"/>
<!-- / -->
</div>
