<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
 
<tiles:importAttribute name="userId" ignore="true"/>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="40%" class="BlockLabel">
   <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
   <fmt:message key="common.label.Password"/>
   </td>
   <td width="60%" class="BlockContent">
       <c:if test="${not empty userId}">  
      <tiles:importAttribute name="administrator"/>
      <s:hidden name="id" value="%{param.u}"/>
      <s:hidden name="u" value="%{param.u}"/>         
          <fmt:message key="admin.user.changePassword.EnterYourCurrent"/><br>
          <input type="password" size="31" maxlength="40" name="currentPassword" tabindex="3"><br>
			<span class="ErrorFieldContent">
              -<s:fielderror fieldName="currentPassword" /><br>
			</span>
      </c:if>
    <fmt:message key="admin.user.changePassword.EnterNew"/><br>
    <input type="password" size="31" maxlength="40" name="newPassword" tabindex="4"><br>
        <c:if test="${passwordMessagesPresent}">
        <div class="ErrorField">
         <span class="ErrorFieldContent">
     
       -<s:fielderror fieldName="newPassword" /><br>
		</span>
     </div>
      </c:if>
    <span class="CaptionText">
     <fmt:message key="admin.user.changePassword.NoSpaces"/><br>&nbsp;<br>
    </span>
    <fmt:message key="admin.user.changePassword.ConfirmNew"/><br>
    <input type="password" size="31" maxlength="40" name="confirmPassword" tabindex="5"><br>
        <c:if test="${passwordMessagesPresent}">
        <div class="ErrorField">
         <span class="ErrorFieldContent">
     
       -<s:fielderror fieldName="confirmPassword" /><br>
		</span>
     </div>
      </c:if>
   </td>
  </tr>

  <%-- we need to display the yellow box below if there is a  password
       message for current password is incorrect --%>
    <c:if test="${passwordMessagesPresent}">
   <tr valign="top"> 
    <td class="BlockLabel">&nbsp;</td>
    <td class="ErrorField">
     <span class="ErrorFieldContent">
      
       -<s:fielderror fieldName="currentPassword" /><br>
      
     </span>
    </td>
   </tr> 
  </c:if>

</table>
<!--  /  -->
