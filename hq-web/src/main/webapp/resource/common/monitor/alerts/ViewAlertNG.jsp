<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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


<%-- Don't insert the sub-tiles if there is no alert and no alertDef. --%>
<c:if test="${not empty alert and not empty alertDef}">

<s:form action="fixAlertAlertPortal">
<input type=hidden name="a" value="<c:out value="${alert.id}"/>"/>
<input type=hidden name="mode" id="mode" value=""/>

<tiles:insertDefinition name=".page.title.events">
  <tiles:putAttribute name="titleKey" value="alert.current.detail.PageTitle"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".events.alert.view.nav" flush="true"/>

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<tiles:insertDefinition name=".events.alert.view.properties"/>

&nbsp;<br>
<tiles:insertDefinition name=".events.config.view.conditions">
  <tiles:putAttribute name="showValues" value="true"/>
</tiles:insertDefinition>

<c:forEach var="action" items="${actionList}">
  &nbsp;<br>
  <tiles:insertAttribute value="${action}"/>
</c:forEach>

<c:if test="${canTakeAction}">
	<tiles:insertDefinition name=".header.tab">
	  	<tiles:putAttribute name="tabKey" value="resource.common.alert.action.fix.header"/>
	</tiles:insertDefinition>                                                                                                                           
	<table cellpadding="10" cellspacing="0" border="0" width="100%" id="fixedSection">
		<tr>
			<c:choose>
				<c:when test="${not alert.fixed}">
					<c:if test="${not empty fixedNote}">
			  			<td class="BlockContent" align="right" valign="top" width="20%">
			    			<div class="BoldText"><fmt:message key="resource.common.alert.previousFix"/></div>
			    		</td>
			    		<td class="BlockContent" colspan="2" width="80%">    		
			       			<c:out value="${fixedNote}"/>
			       		</td>
			       	</tr>
			       	<tr>
			    	</c:if>
		    		<td class="BlockLabel" align="right" valign="top" width="20%"><fmt:message key="resource.common.alert.fixedNote"/></td>
		    		<td class="BlockContent" colspan="2" width="80%">
		    			<s:textarea name="fixedNote" id="fixedNote" value="%{#attr.fixedNote}" cols="70" rows="5" />
		  			</td>
				</tr>
				<tr>
		  			<td class="BlockContent" width="20%" align="right">&nbsp;</td>
		  			<td class="BlockContent" width="5%" style="padding-top: 6px; padding-bottom: 6px;">
				</c:when>
				<c:when test="${not empty fixedNote}">
			  		<td class="BlockContent" width="20%" align="right">&nbsp;</td>
			  		<td class="BlockContent">
			  			<div style="padding: 4px; 0"><c:out value="${fixedNote}"/></div>
				</c:when>
				<c:otherwise>
					<td class="BlockContent" width="20%" align="right">&nbsp;</td>	
			  		<td class="BlockContent">
			  			<div style="padding: 4px 0;"><fmt:message key="resource.common.alert.beenFixed"/></div>
				</c:otherwise>
			</c:choose>
			<tiles:insertTemplate  template="/common/components/ActionButtonNG.jsp">
	  			<tiles:putAttribute name="labelKey" value="resource.common.alert.action.fixed.label"/>
	  			<tiles:putAttribute name="buttonClick">hqDojo.byId('mode').setAttribute('value', '<fmt:message key="resource.common.alert.action.fixed.label"/>'); document.forms[0].submit();</tiles:putAttribute>
	  			<tiles:putAttribute name="icon"><img src='<s:url value="/images/icon_fixed.gif"/>' alt="Click to mark as Fixed" align="middle"/></tiles:putAttribute>
	  			<c:choose>
	  	 			<c:when test="${not alert.fixed}">
	     				<tiles:putAttribute name="disabled" value="false"/>
	     			</c:when>
	     			<c:otherwise>
	        			<tiles:putAttribute name="hidden" value="true"/>
	     			</c:otherwise>
	  			</c:choose>
			</tiles:insertTemplate>
	    	<c:if test="${not alert.fixed}">
	      		<td class="BlockContent">
	        		<fmt:message key="resource.common.alert.clickToFix"/>
	      		</td>
	    	</c:if>
	  	</td>
	</tr>
	</table>
</c:if>

<tiles:insertDefinition name=".events.alert.view.nav" flush="true"/>

<tiles:insertDefinition name=".page.footer"/>

</s:form>

</c:if>

