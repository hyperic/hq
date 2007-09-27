<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="isNewScan" ignore="true"/>

<c:url var="scanMethodAction" value="${selfAction}&rid=${param.rid}&type=${param.type}"/>
<c:set var="formName" value="PlatformAutoDiscoveryForm"/>

<script src="<html:rewrite page="/js/"/>checkAll.js" type="text/javascript"></script>

<!--  GENERAL PROPERTIES TITLE -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.ScanTab"/>
</tiles:insert>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
        <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
        <fmt:message key="resource.autodiscovery.scan.ScanForServerTypes"/>
    </td>
    <td width="30%" rowspan="3" colspan="3" class="BlockBg">
      <table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
        <tr>
					<td><input type="checkbox" class="autodiscParent" onclick="ToggleAll(this);"></td>
					<td colspan="5"><b><fmt:message key="resource.autodiscovery.scan.SelectAll"/></b></td>
			  </tr>
        <tr>
<logic:iterate id="serverType" indexId="status1" name="org.apache.struts.taglib.html.BEAN" 
            property="serverTypes" scope="request">
  <td><html:multibox property="selectedServerTypeIds" value="${serverType.value}" styleClass="autodisc" onclick="ToggleSelection(this);"/> 
</td> 
  <td width="33%"><c:out value="${serverType.label}"/></td> 
  <c:choose>
    <c:when test="${(status1+1) % 3 == 0}">
        </tr>
        <tr>
    </c:when>
  </c:choose>
</logic:iterate>
        </tr>
			</table> 
		</td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel">&nbsp;</td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel">&nbsp;</td>
        <html:hidden property="scanMethod"/>
	</tr>
    
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
    <!-- start of config options -->

    <c:if test="${PlatformAutoDiscoveryForm.configOptionsCount == 0}">
        <tr valign="top">
            <td colspan="1" class="BlockContent">
            </td>
            <td colspan="3" class="BlockContent">
                <fmt:message key="resource.autodiscovery.scan.NoScanConfig"/>
            </td>
        </tr>
    </c:if>
<logic:iterate id="configOption" indexId="ctr" collection="${PlatformAutoDiscoveryForm.configOptions}" >
	<tr valign="top">
		<td class="BlockLabel" colspan="2">
        <c:if test="${configOption.isBoolean == false }">
            <c:if test="${monitorConfigOption.optional == false}">
                <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
            </c:if>
            <c:out value="${configOption.description}"/><fmt:message key="common.label.Colon"/>
        </c:if>            
        </td>

<c:choose>
    <c:when test="${configOption.isBoolean == true }">
        <td class="BlockContent" colspan="2">
			<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
				<tr>
					<td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
					<td><html:img page="/images/schedule_return.gif" width="17" height="21" border="0"/></td>
					<td>
                    <c:choose >
                        <c:when test="${configOption.value == true}">
                            <input type="checkbox" checked name="<c:out value='${configOption.option}'/>" 
                                value="true"/> 
                        </c:when>
                        <c:otherwise>
                            <input type="checkbox" name="<c:out value='${configOption.option}'/>" 
                                value="true"/> 
                        </c:otherwise>
                    </c:choose>            
                    </td>
					<td width="100%"><c:out value='${configOption.description}'/></td>
			    </tr>
			</table>
        </td>
    </c:when>
    <c:when test="${configOption.isEnumeration == true }">
<logic:messagesNotPresent property="${configOption.option}">
        <td class="BlockContent" colspan="2">
          <html:select property="${configOption.option}" value="${configOption.value}">
            <html:optionsCollection name="configOption" property="enumValues"/>
          </html:select>
        </td>
</logic:messagesNotPresent>        
<logic:messagesPresent property="${configOption.option}">
        <td class="ErrorField" colspan="2">
          <html:select property="${configOption.option}" value="${configOption.value}">
            <html:optionsCollection name="configOption" property="enumValues"/>
          </html:select>
          <br>
          <span class="ErrorFieldContent">- <html:errors property="${configOption.option}"/></span>       
        </td>
</logic:messagesPresent>        
    </c:when>
    <c:when test="${configOption.isDir == true }">
<logic:messagesNotPresent property="${configOption.option}">
        <td class="BlockContent" colspan="2">
          <textarea cols="105" rows="3" 
                name="<c:out value='${configOption.option}'/>" ><c:out value='${configOption.value}'/></textarea>
        </td>
</logic:messagesNotPresent>        
<logic:messagesPresent property="${configOption.option}">
        <td class="ErrorField" colspan="2">
          <textarea cols="105" rows="3" 
                name="<c:out value='${configOption.option}'/>" ><c:out value='${configOption.value}'/></textarea>
          <br>
          <span class="ErrorFieldContent">- <html:errors property="${configOption.option}"/></span>       
        </td>
</logic:messagesPresent>        
    </c:when>
    <c:otherwise>
<logic:messagesNotPresent property="${configOption.option}">
 	  <td class="BlockContent" colspan="2">
        <input type="text" 
            name="<c:out value='${configOption.option}'/>" 
            value="<c:out value='${configOption.value}'/>">
      </td>
</logic:messagesNotPresent>      
<logic:messagesPresent property="${configOption.option}">
 	  <td class="ErrorField" colspan="2">
        <input type="text" 
            name="<c:out value='${configOption.option}'/>" 
            value="<c:out value='${configOption.value}'/>">
        <br>
      <span class="ErrorFieldContent">- <html:errors property="${configOption.option}"/></span>       
      </td>
</logic:messagesPresent>
    </c:otherwise>
</c:choose>
        
	</tr>
</logic:iterate>    

</table>
<!--  /  -->
