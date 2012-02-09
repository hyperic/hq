<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
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

<tiles:importAttribute name="canBePrivate" ignore="true"/>

<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_APP" var="CONST_ADHOC_APP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="CONST_TYPE_GROUP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="CONST_TYPE_APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_PS" var="CONST_COMPAT_PS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_SVC" var="CONST_COMPAT_SVC" />
    
<jsu:script>
	var compatibleArr = new Array();
	var compatibleCount = 0;
	// need to set the total size of the compatbility types
	<c:choose>
	<c:when test="GroupForm.platformTypeCount > 0 || GroupForm.serverTypeCount > 0 || GroupForm.serviceTypeCount > 0">
	    compatibleArr.length = <c:out value="${GroupForm.clusterCount}"/> + 2;
	</c:when>
	<c:otherwise>
	    compatibleArr.length = <c:out value="${GroupForm.clusterCount}"/> + 1;
	</c:otherwise>
	</c:choose>
	
	// build the compatible types
	compatibleArr[0] = new Option ("<fmt:message key="resource.group.inventory.New.props.SelectResourceType"/>", "-1");
	
	<c:if test="${GroupForm.platformTypeCount > 0}">
	compatibleArr[1] = new Option ("<fmt:message key="resource.group.inventory.New.props.PlatformType"/>", "-1");
	
	compatibleCount = 1;
	
	<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.platformTypes}">
	  compatibleArr[<c:out value="${resourceCount.count}"/> + 1] = new Option ('<c:out value="${resType.label}"/>',
	                '<c:out value="${resType.value}"/>');
	  <c:if test="${resourceCount.last}">
	      compatibleCount= compatibleCount + <c:out value="${resourceCount.count}"/>;
	  </c:if>
	</c:forEach>
	
	</c:if>
	
	<c:if test="${GroupForm.serverTypeCount > 0}">
	
	<c:if test="${GroupForm.platformTypeCount > 0 }">
	    compatibleArr[compatibleCount + 1] = new Option ("", "-1");
	</c:if>
	
	compatibleArr[compatibleCount + 2] = new Option ("<fmt:message key="resource.group.inventory.New.props.ServerType"/>", "-1");
	
	<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.serverTypes}">
	  compatibleArr[<c:out value="${resourceCount.count}"/> + compatibleCount + 2] = 
	                                            new Option ('<c:out value="${resType.label}"/>',
	                                                        '<c:out value="${resType.value}"/>');
	  <c:if test="${resourceCount.last}">
	      compatibleCount=compatibleCount+<c:out value="${resourceCount.count}"/>;
	  </c:if>
	</c:forEach>
	compatibleCount = compatibleCount + 2;
	
	</c:if>
	
	<c:if test="${GroupForm.serviceTypeCount > 0}">
	
	<c:if test="${GroupForm.platformTypeCount > 0 || GroupForm.serverTypeCount > 0}">
	    compatibleArr[compatibleCount + 1] = new Option ("", "-1");
	</c:if>
	
	compatibleArr[compatibleCount + 2] = new Option ("<fmt:message key="resource.group.inventory.New.props.ServiceType"/>", "-1");
	
	<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.serviceTypes}">
	  compatibleArr[<c:out value="${resourceCount.count}"/> + compatibleCount + 2] = 
	                                            new Option ('<c:out value="${resType.label}"/>',
	                                                        '<c:out value="${resType.value}"/>');
	</c:forEach>
	</c:if>
	
	var clusterArr = new Array();
	
	// build the mixed types
	clusterArr.length=4;
	
	clusterArr[0] = new Option ("<fmt:message key="resource.group.inventory.New.props.SelectResourceType"/>", "-1");
	clusterArr[1] = new Option ('<fmt:message key="resource.group.inventory.New.props.GroupOfGroups"/>',
	                            '<c:out value="${CONST_TYPE_GROUP}"/>:-1');
	clusterArr[2] = new Option ('<fmt:message key="resource.group.inventory.New.props.GroupOfMixed"/>',
	                            '<c:out value="${CONST_ADHOC_PSS}"/>:-1');
	clusterArr[3] = new Option ('<fmt:message key="resource.group.inventory.New.props.GroupOfApplications"/>',
	                            '<c:out value="${CONST_TYPE_APPLICATION}"/>:-1');
	
	var masterArr = new Array ("", compatibleArr, clusterArr);
	
	function changeDropDown (masterSelName, selName, selectVal){
	  var masterSel = document.getElementsByName(masterSelName)[0];
	  var typeIndex = masterSel.selectedIndex;
	  
	  var sel = document.getElementsByName(selName)[0];
	  sel.options.length = 0;
	  
	  if (typeIndex == 0 ) {
	    sel.style.display = "none";
	  }
	  
	  else
	    sel.style.display = "block";
	  
	  if (typeIndex == 1 || typeIndex == 2) {
	    sel.options.length = masterArr[typeIndex].length;
	    
	    for(i=0; i<masterArr[typeIndex].length; i++) {
	  		sel.options[i] = masterArr[typeIndex][i];
	        if (selectVal != null && sel.options[i].value == selectVal)
	            sel.options[i].selected=true;
	  	}
	  }
	}
</jsu:script>
<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.inventory.New.GroupType.Title"/>
</tiles:insert>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" class="BottomLine">
<c:if test="${canBePrivate}">
    <tr>
        <td class="BlockLabel" width="20%"><fmt:message key="resource.group.inventory.New.Label.Private"/></td>
        <td class="BlockContent" colspan="3"><html:checkbox property="privateGroup"/></td>
    </tr>
</c:if>
	<tr>
<c:choose>
<c:when test="${not empty GroupForm.typeName}">
      <td class="BlockLabel" width="20%" nowrap><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.group.inventory.New.Label.Contains"/></td>
      <td class="BlockContent">
        <c:out value="${GroupForm.typeName}"/>
        <html:hidden property="typeAndResourceTypeId"/>
        <html:hidden property="groupType"/>
      </td>
</c:when>
<c:otherwise>
		<td class="BlockLabel" width="20%" nowrap><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.group.inventory.New.Label.Contains"/></td>
<logic:messagesPresent property="groupType">
    <td class="ErrorField">
	  <html:select property="groupType" onchange="changeDropDown('groupType', 'typeAndResourceTypeId');">
		<option value="-1"><fmt:message key="resource.common.inventory.props.SelectOption"/></option>
        <html:optionsCollection property="groupTypes" />
        <br>
      </html:select>
</logic:messagesPresent>
<logic:messagesNotPresent property="groupType">
      <td class="BlockContent">
          <html:select property="groupType" onchange="changeDropDown('groupType', 'typeAndResourceTypeId');" >
          <!--html:select property="groupType"-->
            <option value="-1"><fmt:message key="resource.common.inventory.props.SelectOption"/></option>
            <html:optionsCollection property="groupTypes" />
          </html:select>
      </td>
</logic:messagesNotPresent>
      
<logic:messagesNotPresent property="typeAndResourceTypeId">
      <td width="80%" class="BlockContent">      
          <html:select property="typeAndResourceTypeId"/>
          <jsu:script>
          	document.getElementsByName("typeAndResourceTypeId")[0].style.display = "none";
          </jsu:script>
      </td>
</logic:messagesNotPresent>
<logic:messagesPresent property="typeAndResourceTypeId">
      <td width="80%" class="ErrorField">      
          <html:select property="typeAndResourceTypeId"/>
          <jsu:script>
            document.getElementsByName("typeAndResourceTypeId")[0].style.display = "none";
          </jsu:script>
      </td>          
</logic:messagesPresent>
      <script  type="text/javascript">
        changeDropDown('groupType', 'typeAndResourceTypeId','<c:out value="${GroupForm.typeAndResourceTypeId}"/>');
      </script>
	</tr>
<logic:messagesPresent property="groupType">
	<tr>
      <td class="BlockLabel" nowrap>&nbsp;</td>
      <td width="20%" class="ErrorField">
          <span class="ErrorFieldContent">- <html:errors property="groupType"/></span>
      </td>
      <td width="60%" class="BlockContent">&nbsp;</td>
    </tr>
</logic:messagesPresent>      
<logic:messagesPresent property="typeAndResourceTypeId">
	<tr>
      <td class="BlockLabel" nowrap>&nbsp;</td>
      <td width="20%" class="BlockContent">&nbsp;</td>
      <td width="60%" class="ErrorField">
          &nbsp;
          <span class="ErrorFieldContent">- <html:errors property="typeAndResourceTypeId"/></span>
      </td>
    </tr>
</logic:messagesPresent>      
</c:otherwise>
</c:choose>
    </tr>
</table>
<!--  /  -->
