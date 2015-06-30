<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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

<tiles:importAttribute name="showIQSettings" ignore="true"/>

<jsu:importScript path="/js/functions.js" />
<link rel=stylesheet href="<s:url value="/css/win.css"/>" type="text/css">

<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="DAYS_LABEL" var="CONST_DAYS" />
	
<tiles:insertDefinition name=".portlet.error"/>	
<s:if test="hasErrors()">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock" width="100%"><s:fielderror /></td>
    
  </tr>
</table>
</s:if>
<div class="BlockContent"><fmt:message key="admin.settings.RestartNote"/></div>

<!--  EMAIL CONFIG TITLE -->
<tiles:insertDefinition name=".header.tab">  
  	<tiles:putAttribute name="tabKey" value="admin.settings.EmailConfigTab"/>  
</tiles:insertDefinition>

<!--  EMAIL CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  	<tr>
	    <td width="30%" class="BlockLabel"><fmt:message key="admin.settings.BaseURL"/></td>
	    <td width="40%" class="BlockContent"><s:textfield  size="31" name="baseUrl" value="%{baseUrl}" errorPosition="bottom"/></td>
	    <td width="30%" class="BlockContent" colspan="2"></td>
  	</tr>
  	<tr>
	    <td class="BlockLabel"><fmt:message key="admin.settings.SenderEmailAddressLabel"/></td>
	    <td class="BlockContent"><s:textfield  size="31" name="senderEmail" value="%{senderEmail}" errorPosition="bottom"/></td>
	    <td class="BlockContent" colspan="2"></td>
  	</tr>
</table>

<!--  UPDATE CONFIG TITLE -->
<tiles:insertDefinition name=".header.tab">  
  	<tiles:putAttribute name="tabKey" value="admin.settings.UpdateConfigTab"/>  
</tiles:insertDefinition>
<!--  UPDATE CONFIG CONTENTS -->

	<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  	<tr>    <td width="30%" class="BlockLabel"></td>
	    <td width="40%" class="BlockContent">
			<s:radio key="admin.settings.AnnouncementType" list="#{'0':getText('All'), '1':getText('admin.settings.Major'),'2':getText('common.label.None')}" name="updateMode" value="%{updateMode}"></s:radio>
			
	    </td>
	    <td width="30%" class="BlockContent" colspan="2"></td>
  	</tr>
	<!--  DATA MANAGER CONFIG TITLE -->
  	<tr>
    	<td colspan="4" class="BlockHeader">
			<tiles:insertDefinition name=".header.tab">  
  				<tiles:putAttribute name="tabKey" value="admin.settings.DataMangerConfigTab"/>  
			</tiles:insertDefinition>
    	</td>
  	</tr>
	<!--  DATA MANAGER CONFIG CONTENTS -->
  	<tr>
    	<td class="BlockLabel"><fmt:message key="admin.settings.DataMaintInterval"/></td>
    	<td class="BlockContent">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td class="BlockContent">
						<s:textfield size="2" name="maintIntervalVal" errorPosition="bottom"/>
					</td>
					<td class="BlockContent" width="100%"><fmt:message key="admin.settings.Hours"/>
						<s:hidden theme="simple" name="maintInterval" value="%{#CONST_HOURS}"/>
					</td>
				</tr>
			</table>
		</td>
</td>
  	</tr>
  	<tr><td class="BlockLabel"><fmt:message key="admin.settings.PurgeOlderThanLabel"/></td>
    	<td class="BlockContent">
      		<table width="100%" cellpadding="0" cellspacing="0" border="0">
        		<tr>
				<td class="BlockContent">
            				<s:textfield size="2" name="deleteUnitsVal" errorPosition="bottom"/>
          				</td>
          				<td class="BlockContent" width="100%"><fmt:message key="admin.settings.Days"/>
            				<s:hidden theme="simple"  name="deleteUnits" value="%{#CONST_DAYS}"/>
          				</td>
						</tr>
						<tr>
          				<td class="BlockContent" colspan="2"></td>
        			</tr>
					</table>
					</td>
					<td class="BlockLabel"/>
    	<td class="BlockContent">
      		<table width="100%" cellpadding="0" cellspacing="0" border="0">
        		<tr>
          			<td class="BlockContent" colspan="2"></td>
        		</tr>
      		</table>
    	</td>
  	</tr>
  	<tr>
	<td class="BlockLabel"><fmt:message key="admin.settings.Reindex"/></td>
    	<td class="BlockLabel" align="left">
      		<div style="float:left;">
      			<table cellpadding="0" cellspacing="4" border="0">
        			<tr>
					<td align="left">
					
								<s:radio  list="#{true:getText('yesno.true'), false:getText('yesno.false')}" name="reindex" value="%{reindex}"></s:radio>

					
          				</td>
        			</tr>
      			</table>
				      		</div>
    	</td>
    	<td class="BlockLabel"></td>
    	<td class="BlockContent"></td>
  	</tr>
  	<tr>
    	<td class="BlockLabel"><fmt:message key="admin.settings.AlertPurge"/></td>
    	<td class="BlockContent">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
		<tr>
					
          				<td class="BlockContent">
            				<s:textfield size="2" name="alertPurgeVal" errorPosition="bottom"/>
          				</td>
          				<td class="BlockContent" width="100%">
          					<fmt:message key="admin.settings.Days"/>
            				<s:hidden theme="simple"  name="alertPurge" value="%{#CONST_DAYS}"/>
          				</td>
					
        		</tr>
				<tr>
          				<td class="BlockContent" colspan="2"></td>
        			</tr>
					</table>
    	</td>
    	<td class="BlockLabel"></td>
    	<td class="BlockContent">
      		<table width="100%" cellpadding="0" cellspacing="0" border="0">
        		<tr>
          			<td class="BlockContent" colspan="2"></td>
        		</tr>
      		</table>
			</td>
  	</tr>
  	<tr>
    	<td class="BlockLabel"><fmt:message key="admin.settings.EventLogPurge"/></td>
    	<td class="BlockContent">
      		<table width="100%" cellpadding="0" cellspacing="0" border="0">
        		<tr> 
				
					          
					
          				<td class="BlockContent">
            				<s:textfield size="2" name="elPurgeVal" errorPosition="bottom"/>
          				</td>
          				<td class="BlockContent" width="100%"><fmt:message key="admin.settings.Days"/>
            				<s:hidden theme="simple"  name="elPurge" value="%{#CONST_DAYS}"/>
							</td>
					       
        		</tr>

	
		
        			<tr>
          				<td class="BlockContent" colspan="2"></td>
        			</tr>
				
      		</table>
    	</td>
<td class="BlockLabel" colspan="2" width="30%"></td>
				</tr>
</table>

<tiles:insertDefinition name=".header.tab">  
  <tiles:putAttribute name="tabKey" value="admin.settings.vCenterTab"/>  
</tiles:insertDefinition>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.vCenterAddress"/></td>
    <td width="30%" class="BlockContent"><s:textfield size="31" name="vCenterURL" value="%{vCenterURL}"  errorPosition="bottom"/></td>
    <td width="30%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
   <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.vCenterUser"/></td>
    <td width="30%" class="BlockContent"><s:textfield size="31" name="vCenterUser" value="%{vCenterUser}" errorPosition="bottom"/></td>
    <td width="30%" class="BlockContent" colspan="2"></td>
   </tr>
  <tr>
  <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.vCenterPassword"/></td>
    <td width="30%" class="BlockContent"><s:password size="31" name="vCenterPassword" showPassword="true" value="%{vCenterPassword}" /></td>
    <td width="30%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"></td>
  </tr>
</table>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  	<tr>
    	<td class="BlockHeader" colspan="2">
			<tiles:insertDefinition name=".header.tab">  
  				<tiles:putAttribute name="tabKey" value="admin.settings.AlertConfigTab"/>  
			</tiles:insertDefinition>
    	</td>
  	</tr>
  	<tr>
		<td class="BlockContent" colspan="2"><fmt:message key="admin.settings.RestartNote.Alert"/></td>
  	</tr>
	<!--  GLOBAL ALERT CONFIG CONTENTS -->
  	<tr>
    	<td class="BlockLabel" width="30%"><fmt:message key="admin.settings.AlertsEnabled"/></td>
    	<td class="BlockContent" width="70%" style="padding-left: 6px;">
		<s:radio  list="#{true:getText('ON'), false:getText('OFF')}" name="alertsAllowed" value="%{alertsAllowed}"></s:radio>
    		
    	</td>
  	</tr>
  	<tr>
	   	<td class="BlockLabel" width="30%"><fmt:message key="admin.settings.AlertNotificationsEnabled"/></td>
    	<td class="BlockContent" width="70%" style="padding-left: 6px;">
		<s:radio  list="#{true:getText('ON'), false:getText('OFF')}" name="alertNotificationsAllowed" value="%{alertNotificationsAllowed}"></s:radio>
    		    	
		</td>
  	</tr>
</table>

