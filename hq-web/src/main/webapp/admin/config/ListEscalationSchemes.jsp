<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
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
<table width="100%" cellpadding="0" cellspacing="10">
<tr>
<td width="30%" valign="top" id="escalationsList">
<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

    <table width="100%" cellpadding="0" cellspacing="0" class="TableBottomLine">
    <thead>
    <tr>
          <td colspan="2">
                <table cellpadding="0" cellspacing="0" border="0" width="100%">
                    <tr>
                        <td class="BlockTitle"><fmt:message key="common.header.EscalationName"/></td>
                        <c:if test="${not empty param.escId && useroperations['createEscalation']}">
                        	<td class="BlockTitle" id="createButton" style="text-align: right;">
                        		<html:link action="/admin/config/Config.do">
                        			<html:param name="mode" value="escalate" />
                        			<html:img src="/images/tbb_new.gif" border="0"/>
                        		</html:link>
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
</td>

<!-- Do the delete button here so that we don't have to try to duplicate it in javascript -->
<span id="deleteBtn" style="display: none;">&nbsp;
<c:if test="${useroperations['removeEscalation']}">
<html:img page="/images/tbb_delete.gif" border="0" onmouseout="imageSwap(this, imagePath + 'tbb_delete', '');" onmousedown="imageSwap(this, imagePath + 'tbb_delete', '_gray')"/>
</c:if>
</span>
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

  		var editEscUrl = "<html:rewrite action="/admin/config/Config"><html:param name="mode" value="escalate"/><html:param name="escId" value="{escId}"/></html:rewrite>";
  		var removeEscUrl = "<html:rewrite action="/admin/config/RemoveEscalation"><html:param name="esc" value="{escId}"/></html:rewrite>";
  
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
      			td2.innerHTML = '<html:img page="/images/icon_right_arrow.gif" border="0" width="10" height="10" style="padding-right:5px;"/>' + '<b>' + schemes[i].name.escapeHTML() + '</b>';
      			td2.setAttribute((document.all ? 'className' : 'class'), "selectedHighlight");
      			td2.setAttribute("align", "left");
    		} else {
      			td2.innerHTML = '<html:img page="/images/icon_right_arrow.gif" border="0" width="10" height="10" style="display:none;padding-right:5px;"/>' + '<a href="' + unescape(editEscUrl).replace("{escId}", schemes[i].id) + '" onclick="changeHighlight(this);">' + schemes[i].name.escapeHTML() + '</a>';
	  			td2.setAttribute((document.all ? 'className' : 'class'), 'ListCell');
    		}
    
    		tr.appendChild(td2);

    		td3 = document.createElement("td");
    		
    		td3.setAttribute('align', 'right');

    		if (schemes.length > 1) {
      			td3.innerHTML = '<a href="' + unescape(removeEscUrl).replace("{escId}", schemes[i].id) + '">' + hqDojo.byId('deleteBtn').innerHTML + '</a>';
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
    	new Ajax.Request('<html:rewrite action="/escalation/ListAllEscalationName"/>', {onSuccess:showEscRows});
    	document.EscalationSchemeForm.action = '<html:rewrite action="/admin/config/Config"/>';
    	document.EscalationSchemeForm.mode.value = 'escalate';
  	}

  	var reloadScheme = true;
</jsu:script>
<jsu:script onLoad="true">
	initEscalationSchemes();
</jsu:script>
<br/>

<td valign="top">
<c:choose>
  <c:when test="${not empty param.escId}">
    <tiles:insert page="/admin/config/ViewEscalation.jsp"/>
  </c:when>
  <c:otherwise>
    <tiles:insert page="/admin/config/NewEscalation.jsp"/>
  </c:otherwise>
</c:choose>
</td>
</tr>
</table>

<br/>
<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
