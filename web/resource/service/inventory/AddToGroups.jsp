<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<%-- end vit: delete this block --%>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="editService"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<form>

<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
    <tr>
      <td width="50%" valign="top">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.inventory.edit.GroupsTab"/>
  <tiles:put name="useFromSideBar" value="true"/>
</tiles:insert>
      </td>
	  <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
      <td>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.inventory.edit.AddToGroupsTab"/>
  <tiles:put name="useToSideBar" value="true"/>
</tiles:insert>
	  </td>
    </tr>
	<tr>
		
		<!--  SELECT COLUMN  -->
		<td width="50%" valign="top">
		
		<!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
		<div id='<c:out value="${widgetInstanceName}"/>FromDiv'>
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="ListHeaderInactive"><input type="checkbox" name="fromToggleAll" onClick="ToggleAll(this, widgetProperties);"></td>
				<td class="ListHeaderInactiveSorted">
				<html:img page="/images/spacer.gif" width="200" height="1" border="0"/><br>
				<fmt:message key="common.header.Group"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
				<td class="ListHeaderInactive" width="100%">
				<html:img page="/images/spacer.gif" width="150" height="1" border="0"/><br>
				<fmt:message key="common.header.Description"/></td>
				<td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="availableListMember"></td>
				<td class="ListCell"><b>Some Group</b></td>
				<td class="ListCell">Lorem ipsum dolor</td>
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
			<!-- / ROW  -->
		</table>
		</div>
		<!--  /  -->
		
		<!--  TABLED LIST TOOLBAR -->
		<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
			<tr>
				<td><html:img page="/images/spacer.gif" width="1" height="16" border="0"/></td>
				<!--  PAGINATION CONTROLS  -->
				<td width="100%" align="right" valign="bottom">
				<table cellpadding="0" cellspacing="0" border="0">
					<tbody id="navTBody">
						<tr id="navRow">
							<td><html:img page="/images/tbb_pageleft_gray.gif" width="19" height="16" border="0"/></td>
							<td><html:img page="/images/spacer.gif" width="3" height="1" border="0"/></td>
							<td><html:img page="/images/tbb_left.gif" width="4" height="16" border="0"/><html:img page="/images/tbb_page_on.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_right.gif" width="4" height="16" border="0"/></td>
							<td><html:img page="/images/spacer.gif" width="3" height="1" border="0"/></td>
							<td><html:img page="/images/tbb_pageright.gif" width="19" height="16" border="0"/></td>
						</tr>
					</tbody>
				</table>
				</td>
				<!--  /  -->
				<td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
			</tr>
		</table>
		<!--  /  -->
		
		<!-- / SELECT COLUMN  -->
		</td>
		
		
		<!--  ADD/REMOVE COLUMN  -->
	    <td id='<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd'>
		  <div id="AddButtonDiv" align="left"><html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/></div>
	      <br>&nbsp;<br>
	      <div id="RemoveButtonDiv" align="right"><html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/></div>
	    </td>
	    <!-- / ADD/REMOVE COLUMN  -->
		
		
		<!--  ADD COLUMN  -->
		<td width="50%" valign="top">

		<!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
		<div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListHeaderInactive"><input type="checkbox" name="toToggleAll" onClick="ToggleAll(this, widgetProperties);"</td>
				<td class="ListHeaderInactiveSorted">
				<html:img page="/images/spacer.gif" width="200" height="1" border="0"/><br>
				<fmt:message key="common.header.Group"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
				<td class="ListHeaderInactive" width="100%">
				<html:img page="/images/spacer.gif" width="150" height="1" border="0"/><br>
				<fmt:message key="common.header.Description"/></td>
			</tr>
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCellCheckbox"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties);" class="pendingListMember"></td>
				<td class="ListCell"><b>BU-8 Senior Admin</b></td>
				<td class="ListCell">Adma Qualset (aqualset)</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
			
			<!--  ROW  -->
			<tr class="ListRow">
				<td class="ListCellLine"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td class="ListCell"><html:img page="/images/spacer.gif" width="1" height="20" border="0"/></td>
				<td class="ListCell">&nbsp;</td>
				<td class="ListCell">&nbsp;</td>
			</tr>
			<!-- / ROW  -->
		</table>
		</div>
		<!--  /  -->
		
		<!--  TABLED LIST TOOLBAR -->
		<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
			<tr>
				<td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
				<td><html:img page="/images/tbb_new.gif" width="42" height="16" border="0"/></td>
				
				<!--  PAGINATION CONTROLS  -->
				<td width="100%" align="right" valign="bottom">
				<!--<table cellpadding="0" cellspacing="0" border="0">
					<tbody id="navTBody">
						<tr id="navRow">
							<td><html:img page="/images/tbb_pageleft_gray.gif" width="19" height="16" border="0"/></td>
							<td><html:img page="/images/spacer.gif" width="3" height="1" border="0"/></td>
							<td><html:img page="/images/tbb_left.gif" width="4" height="16" border="0"/><html:img page="/images/tbb_page_on.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_page_off.gif" width="13" height="16" border="0"/><html:img page="/images/tbb_right.gif" width="4" height="16" border="0"/></td>
							<td><html:img page="/images/spacer.gif" width="3" height="1" border="0"/></td>
							<td><html:img page="/images/tbb_pageright.gif" width="19" height="16" border="0"/></td>
						</tr>
					</tbody>
				</table>-->
				</td>
				<!--  /  -->
				
			</tr>
		</table>
		<!--  /  -->

		</td>
		
	</tr>
</table>
<!-- / SELECT & ADD -->

<tiles:insert page="/common/components/FormButtons.jsp"/>
</form>
