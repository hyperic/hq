<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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

<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>
 
<jsu:script>
	function checkGroup() {
	  	if (document.ResourceHubForm.ff.selectedIndex == 4) {
	    	document.ResourceHubForm.g.value = 2;
	  	} else {
	    	document.ResourceHubForm.g.value = 1;
	  	}
	}
</jsu:script>


<div class="effectsPortlet">
	<!-- Content Block Title -->
	<tiles:insertDefinition name=".header.tab">
  		<tiles:putAttribute name="tabKey" value="dash.home.SearchResources"/>
  		<tiles:putAttribute name="adminUrl" value="${adminUrl}"/>
		<tiles:putAttribute name="portletName" value="${portletName}"/>
	</tiles:insertDefinition>
	
	<!-- fixme: there's no "minimize" functionality on this block, only "close" -->
	<s:set var="elements" value="#attr.resourcesHub.entrySet()"/>
	<s:form name="ResourceHubForm"  action="resourceHub" onsubmit="checkGroup()">
		<s:hidden theme="simple"  name="g" value="1"/>
		
		<!-- Content Block Contents -->
		<table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBotBorder" >
  			<tr>
    			<td class="BlockContent" colspan="3"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  			</tr>
  			<tr valign="top">
    			<td class="BlockContent" nowrap>
      				<input type="text" size="12" maxlength="40" value='<fmt:message key="common.header.ResourceName"/>' onfocus="this.value='';" name="keywords">      
    			</td>
    			<td class="BlockContent" nowrap>
									
							<select class="FilterFormText" name="ff" >
								<c:forEach var="itemKey" items="${elements}" > 
								<c:set var="curKey" value="${itemKey.key}" />
								<c:if test="${itemKey.key == '6'}">
									<c:set var="curKey" value="5" />
								</c:if>
								<option value="<c:out value="${curKey}"/>"><fmt:message key="${itemKey.value}" /></option>
								</c:forEach>           
							</select>
    			</td>
    			<td width="100%" class="BlockContent" valign="center"><input type="image" src='<s:url value="/images/4.0/icons/accept.png" />' name="image" border="0" property="ok" /></td>
  			</tr>                                                         
  			<tr>
    			<td class="BlockContent" colspan="3"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  			</tr>
		</table>
	</s:form>
</div>
