<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="isNewScan" ignore="true"/>
<tiles:importAttribute name="formName" />

<c:url var="scanMethodAction" value="${selfAction}">
	<c:param name="rid" value="${param.rid}"/>
	<c:param name="type" value="${param.type}"/>
</c:url>
<c:set var="formName" value="${formName}"/>
<jsu:importScript path="/js/checkAll.js" />
<!--  GENERAL PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.autodiscovery.ScanTab"/>
</tiles:insertDefinition>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
        <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
        <fmt:message key="resource.autodiscovery.scan.ScanForServerTypes"/>
    </td>
    <td width="30%" rowspan="3" colspan="3" class="BlockBg">
      <table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
        <tr>
					<td><input type="checkbox" class="autodiscParent" onclick="ToggleAll(this);"></td>
					<td colspan="5"><b><fmt:message key="resource.autodiscovery.scan.SelectAll"/></b></td>
			  </tr>
        <tr>

		
<c:set var="status1" value="0" scope="page" />
<c:forEach var="serverType" items="${newForm.serverTypes}">


  <td><input type="checkbox" name="selectedServerTypeIds" value="${serverType.value}" class="autodisc" onclick="ToggleSelection(this);" />
</td> 
  <td width="33%"><c:out value="${serverType.label}"/></td> 
  <c:choose>
    <c:when test="${(status1+1) % 3 == 0}">
        </tr>
        <tr>
    </c:when>
  </c:choose>
  <c:set var="status1" value="${status1 + 1}" scope="page"/>
</c:forEach >
        </tr>
			</table> 
		</td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel">&nbsp;</td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel">&nbsp;</td>
        <s:hidden name="scanMethod" value="%{#newForm.scanMethod}"/>
	</tr>
    
	<tr>
      <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
    <!-- start of config options -->

    <c:if test="${newForm.configOptionsCount == 0}">
        <tr valign="top">
            <td colspan="1" class="BlockContent">
            </td>
            <td colspan="3" class="BlockContent">
                <fmt:message key="resource.autodiscovery.scan.NoScanConfig"/>
            </td>
        </tr>
    </c:if>
	
<c:set var="ctr" value="0" scope="page" />
<c:forEach var="configOption" items="${newForm.configOptions}">
	
	<tr valign="top">
		<td class="BlockLabel" colspan="2">
        <c:if test="${configOption.isBoolean == false }">
            <c:if test="${monitorConfigOption.optional == false}">
                <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
            </c:if>
            <c:out value="${configOption.description}"/><fmt:message key="common.label.Colon"/>
        </c:if>            
        </td>

<c:choose>
    <c:when test="${configOption.isBoolean == true }">
        <td class="BlockContent" colspan="2">
			<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
				<tr>
					<td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
					<td><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0"/></td>
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

        <td class="BlockContent" colspan="2">
		  <s:select name="%{#attr..configOption.option}" list="%{#attr.configOption.enumValues}" listKey="key" listValue="value" />
        </td>
            
    </c:when>
    <c:when test="${configOption.isDir == true }">

        <td class="BlockContent" colspan="2">
          <textarea cols="105" rows="3" 
                name="<c:out value='${configOption.option}'/>" ><c:out value='${configOption.value}'/></textarea>
        </td>
            
    </c:when>
    <c:otherwise>

 	  <td class="BlockContent" colspan="2">
        <input type="text" 
            name="<c:out value='${configOption.option}'/>" 
            value="<c:out value='${configOption.value}'/>">
      </td>
     
    </c:otherwise>
</c:choose>
        
	</tr>
</c:forEach>    
   

</table>
<!--  /  -->
