<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<c:set var="first" value="true" />

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >  
  
  <td valign="top" width='100%' >      
    <c:forEach var="portlet" items="${columnsList}" >        
    <table width="100%" border="0" cellspacing="0" cellpadding="0">          
      <c:choose>
        <c:when test="${first eq true}">
        <tr> 
          <td colspan="4">
            <tiles:insert  beanProperty="url" beanName="portlet" flush="true"/>
          </td>
          <c:set var="first" value="false" />
        </tr>
        </c:when >            
        <c:otherwise>
        <tr>  
          <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
          <td><html:img page="/images/spacer.gif" width="75" height="1" alt="" border="0"/></td>
          <td valign="top" width="100%">           
            <tiles:insert  beanProperty="url" beanName="portlet" flush="true"/>
            <c:if test="${not portal.dialog}">
            &nbsp;<br>
            </c:if>
          </td>      
        </tr>  
        </c:otherwise>
        
      </c:choose>
      
    </table>             
    </c:forEach>    
    <small><br></small><html:img page="/images/spacer.gif" width="95%" height="1" border="0"/>
  </td> 

</c:forEach>
<!-- /Content Block -->
