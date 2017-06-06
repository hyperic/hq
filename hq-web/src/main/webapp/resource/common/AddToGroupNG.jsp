<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.me
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


<tiles:importAttribute name="resource" ignore="true" />
<c:if test="${not empty resource.entityId}">
	<c:set var="theEntityId" value="${resource.entityId}"/>
</c:if>
<c:if test="${empty theEntityId}">
	<c:if test="${not empty eid}">
		<c:set var="theEntityId" value="${eid}"/>
	</c:if>
</c:if>

<div id="add_to_existing_group_dialog" style="display:none;">
    <form name="AddToExistingGroupForm" action="<s:url action="newResourceInventoryGroupVisibility.action"/>" method="post" onsubmit="return false;">
		<input type="hidden" name="eid" value='<c:out value="${theEntityId}"/>' />
	<div id="AddToExistingGroupStatus" style="display:none"></div>
	<div id="AddToExistingGroupDiv" style="width:500px; height:300px;">
		<div id="AddToExistingGroupDataDiv">
			<fieldset>
				<legend><fmt:message key="resource.group.AddToGroup.Title"/></legend>
				<div style="height:240px; overflow-x:hidden; overflow-y:auto;">
					<table width="100%" cellpadding="0" cellspacing="0" border="0">
						<thead>
							<tr class="tableRowHeader">
								<th class="ListHeaderCheckbox" style="width:20px"><input type="checkbox" id="AddToExistingGroup_CheckAllBox" onclick="MyGroupManager.dialogs.AddToExistingGroup.toggleAll(this);" /></td>
								<th class="tableRowInactive" style="width:52%">Group</td>
								<th class="tableRowInactive">Description</td>
							</tr>
						</thead>
						<tfoot id="AddToExistingGroupTableFooter">
							<tr>
								<td colspan="3" style="font-style:italic; text-align:center; padding-top:10px;">Loading...</td>
							</tr>
						</tfoot>
						<tbody id="AddToExistingGroupTableBody"></tbody>
					</table>
				</div>
			</fieldset>
		</div>
		<div id="AddToExistingGroupButtonDiv" style="padding-top:5px">
			<table cellspacing="0" cellpadding="0">
				<tr>
					<td valign="middle" nowrap="true">
						<input type="button" id="AddToExistingGroupButton" value="ADD TO EXISTING GROUP" class="CompactButton" onclick="javascript:MyGroupManager.addResourcesToGroups(this.form);" />
						&nbsp;&nbsp;
  						<input type="button" id="AddToNewGroupButton" value="ADD TO NEW GROUP" class="CompactButton" onclick="javascript:MyGroupManager.addNewGroup(this.form.eid.value);" />
					</td>
				</tr>
			</table>
		</div>
	</div>
	</form>
</div>
<jsu:script>
	hqDojo.require("dijit.dijit");
    hqDojo.require("dijit.Dialog");

    var MyGroupManager = null;
    var AddToGroupMenuLink = hqDojo.byId("AddToGroupMenuLink");

    if (AddToGroupMenuLink) {
    	AddToGroupMenuLink.onclick = function() { 
        	MyGroupManager.processAction(document.AddToExistingGroupForm); 
        	return false; 
        };
    }
</jsu:script>
<jsu:script onLoad="true">

   	MyGroupManager = new hyperic.group_manager({
		associationsUrl: "<s:url value="/app/resource/associations"/>",
		postUrl:"<s:url value="/app/resource/association"/>"
   	});
</jsu:script>