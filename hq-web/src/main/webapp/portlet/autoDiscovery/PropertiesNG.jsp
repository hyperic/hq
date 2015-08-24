<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
 <tiles:importAttribute name="portletName" ignore="true"/>
<jsu:script>
  	var help = "<hq:help/>";
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" alt="" border="0" /></td>
    <td><img src='<s:url value="/images/spacer.gif" />' width="15" height="1" alt="" border="0" /></td>
    <td width="67%" class="PortletTitle" nowrap><fmt:message key="dash.home.AutoDiscovery.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif" />' width="202" height="32" alt="" border="0" /></td>
    <td width="1%"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" alt="" border="0" /></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
    <td colspan='2'><img src='<s:url value="/images/spacer.gif" />' width="1" height="10" alt="" border="0" /></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
      <s:form action="updateAutoDiscModifyPortlet.action" >
<div id="narrowlist_false">
      <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.DisplaySettings"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
</div>
      <tiles:insertDefinition name=".ng.dashContent.admin.generalSettings">
        <tiles:putAttribute name="portletName" value="${portletName}"/>
      </tiles:insertDefinition>

      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr>
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.AutoDiscRange"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
            <table width="100%" cellpadding="0" cellspacing="5" border="0">
             <tr>
              <td nowrap><fmt:message key="dash.settings.auto-disc.last"/></td>
                 <td>
				 <s:select theme="simple" cssStyle="FilterFormText" name="range" disabled="%{!#attr.modifyDashboard}" list="#{ '5':'5', '10':'10', '-1':getText('dash.settings.auto-disc.all') }" value="%{#attr.range}" />
                 </td>
              <td width="100%"><fmt:message key="dash.settings.auto-disc.completed"/></td>
             </tr>
           </table>
          </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0"/></td>
        </tr>
      </table>
      <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelAutoDiscModifyPortlet" />
			<tiles:putAttribute name="resetAction"  value="resetAutoDiscModifyPortlet" />
		  </c:if>
      </tiles:insertDefinition>
      </s:form>
    </td>
  </tr>
  <tr> 
    <td colspan="4"><img src='<s:url value="/images/spacer.gif" />' width="1" height="13" alt="" border="0" /></td>
  </tr>
</table>
