<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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

<tiles:insertTemplate template="/admin/config/AdminHomeNav.jsp"/>
<jsu:script>
    function changeHighlight(elem) {
           elem.previousSibling.style.display = "";
           elem.parentNode.style.backgroundColor = "#dbe3f5";
           elem.parentNode.nextSibling.style.backgroundColor = "#dbe3f5";

        }

    function hideCreateButton() {
        hqDojo.byId('createButton').style.display = "none";
    }
</jsu:script>

<table width="100%" cellpadding="0" cellspacing="10" border="0">
<tr>
<td width="30%" valign="top" align="left" id="escalationsList">
<tiles:insertDefinition   name=".portlet.error"/>
<tiles:insertDefinition   name=".portlet.confirm"/>

    <table width="100%" cellpadding="0" cellspacing="0" class="TableBottomLine" border="0">
    <thead>
    <tr>
          <td colspan="2">
                <table cellpadding="0" cellspacing="0" border="0" width="100%">
                    <tr>
                        <td class="BlockTitle"><fmt:message key="common.header.EscalationName"/></td>
                        <c:if test="${not empty param.escId && useroperations['createEscalation']}">
                        	<td class="BlockTitle" id="createButton" style="text-align: right;">
                        		<s:a action="escalateConfig">
                        			<img src="<s:url value='/images/tbb_new.gif' />" border="0" alt="" id="toolMenuArrow" />
                        		</s:a>
                        	</td>
                        </c:if>
                </tr>
            </table>
        </td>
    </tr>
    </thead>
    <tbody id="escalations">
    </tbody>
    </table>


<!-- Do the delete button here so that we don't have to try to duplicate it in javascript -->
<span id="deleteBtn" style="display: none;">&nbsp;
<c:if test="${useroperations['removeEscalation']}">
<img src="<s:url value='/images/tbb_delete.gif' />"  border="0" onmouseout="imageSwap(this, imagePath + 'tbb_delete', '');" onmousedown="imageSwap(this, imagePath + 'tbb_delete', '_gray')" />
</c:if>
</span>
</td>
<jsu:script>
	function showEscRows(originalRequest) {
  		var escJson = eval( '( { "escalations": ' + originalRequest.responseText + ' })' );
  		var schemes = escJson.escalations;
  		var escalations = hqDojo.byId('escalations');
  
  		if (escalations.childNodes.length > 0) {
    		while(escalations.lastChild) {
      			escalations.removeChild(escalations.lastChild);
    		}
  		}

  		if (schemes.length == 0) {
    		var tr = document.createElement("tr");
    
    		tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

    		var td1 = document.createElement("td");
    		
    		td1.setAttribute((document.all ? 'className' : 'class'), "ListCell");
    		td1.setAttribute('colspan', '2');
    		td1.innerHTML = '<fmt:message key="admin.config.message.noEscalations"/>';
    		tr.appendChild(td1);

    		escalations.appendChild(tr);
  		}
		
		<s:set var="tempPlaceHolder" value="'placeHolder'" />
  		var editEscUrl = "<s:url value="escalateConfig.action"><s:param name="escId" value="#attr.tempPlaceHolder"/></s:url>";
  		var removeEscUrl = "<s:url value="RemoveEscalationAction.action"><s:param name="esc" value="#attr.tempPlaceHolder"/></s:url>";
		var toBeReplacedEscId = "placeHolder";
  
  		for (var i = 0; i < schemes.length; i++) {
    		var tr = document.createElement("tr");
    
    		if ((i % 2) == 0) {
      			tr.setAttribute((document.all ? 'className' : 'class'), "tableRowEven");
    		} else {
      			tr.setAttribute((document.all ? 'className' : 'class'), "tableRowOdd");
    		}

    		var td2 = document.createElement("td");
    		
    		td2.setAttribute('title', '<fmt:message key="admin.config.message.ClickEscNameEdit"/>')
    
    		if (schemes[i].id == '<c:out value="${param.escId}"/>') {
      			td2.innerHTML = '<img src="<s:url value="/images/icon_right_arrow.gif" />" border="0" width="10" height="10" style="padding-right:5px;" />' + '<b>' + schemes[i].name.escapeHTML() + '</b>';
      			td2.setAttribute((document.all ? 'className' : 'class'), "selectedHighlight");
      			td2.setAttribute("align", "left");
    		} else {
				var tempEditURL= unescape(editEscUrl).replace(toBeReplacedEscId, schemes[i].id);
      			td2.innerHTML = '<img src="<s:url value="/images/icon_right_arrow.gif" />" border="0" width="10" height="10" style="display:none;padding-right:5px;" />' + '<a href="' + unescape(tempEditURL) + '" onclick="changeHighlight(this);">' + schemes[i].name.escapeHTML() + '</a>';
	  			td2.setAttribute((document.all ? 'className' : 'class'), 'ListCell');
    		}
    
    		tr.appendChild(td2);

    		td3 = document.createElement("td");
    		
    		td3.setAttribute('align', 'right');

    		if (schemes.length > 1) {
				var tempRemoveURL= unescape(removeEscUrl).replace(toBeReplacedEscId, schemes[i].id);
      			td3.innerHTML = '<a href="' +  unescape(tempRemoveURL) + '">' + hqDojo.byId('deleteBtn').innerHTML + '</a>';
    		} else {
        		td3.innerHTML="&nbsp;";
    		}

    		if (schemes[i].id == '<c:out value="${param.escId}"/>') {
      			td3.setAttribute((document.all ? 'className' : 'class'), "selectedHighlight");
    		} else {
      			td3.setAttribute((document.all ? 'className' : 'class'), "ListCell");
    		}

    		tr.appendChild(td3);
    		escalations.appendChild(tr);
  		}
	}

  	function initEscalationSchemes() {
    	new Ajax.Request('<s:url value="ListAllEscalationName.action" />', {onSuccess:showEscRows});
    	document.EscalationSchemeForm.action = "<s:url value='escalateConfig.action'/>";
    	document.EscalationSchemeForm.mode.value = 'escalate';
  	}

  	var reloadScheme = true;
</jsu:script>
<jsu:script onLoad="true">
	initEscalationSchemes();
</jsu:script>

<td valign="top" align="left" width="70%">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<c:choose>
  <c:when test="${not empty param.escId}">
    <tiles:insertTemplate template="/admin/config/ViewEscalationNG.jsp"/>
  </c:when>
  <c:otherwise>
    <tiles:insertTemplate template="/admin/config/NewEscalationNG.jsp"/>
  </c:otherwise>
</c:choose>
</table>
</td>
</tr>
</table>

<br/>
<tiles:insertTemplate template="/admin/config/AdminHomeNav.jsp"/>