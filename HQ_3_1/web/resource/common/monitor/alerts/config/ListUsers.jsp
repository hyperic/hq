<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<tiles:importAttribute name="selfUrl"/>

<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
<c:choose>
<c:when test="${null == notifyList || empty listSize}">
<!-- error occured -->
</c:when>
<c:otherwise>

<hq:pageSize var="pageSize"/>
<display:table cellspacing="0" cellpadding="0" width="100%" action="${selfUrl}" pageSize="${pageSize}" items="${notifyList}">
  <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
    <display:checkboxdecorator name="users" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column width="15%" property="firstName" sort="true" sortAttr="2" defaultSort="false" title="alert.config.props.NB.FirstName"/>
  <display:column width="15%" property="lastName" sort="true" sortAttr="1" defaultSort="false" title="alert.config.props.NB.LastName"/>
  <display:column width="20%" property="name" sort="true" sortAttr="0" defaultSort="true" title="alert.config.props.NB.Username" href="/admin/user/UserAdmin.do?mode=view" paramId="u" paramProperty="id"/>
  <display:column width="29%" property="emailAddress" title="alert.config.props.NB.Email" autolink="true"/>
  <display:column width="20%" property="department" title="alert.config.props.NB.Dept"/>
</display:table>

</c:otherwise>
</c:choose>
