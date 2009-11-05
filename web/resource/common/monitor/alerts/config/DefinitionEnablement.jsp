<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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


<tiles:importAttribute name="enableList"/>
<tiles:importAttribute name="showDuration" ignore="true"/>

    <tr>
      <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>

    <logic:messagesPresent property="whenEnabled">
    <c:set var="whenEnabledErrs" value="true"/>
    <tr>
      <td colspan="2" class="ErrorField">
        <span class="ErrorFieldContent"><html:errors property="whenEnabled"/></span>
      </td>
    </tr>
    </logic:messagesPresent>
    
    <logic:messagesPresent property="meetTimeTP">
    <c:set var="meetTimeTPErrs" value="true"/>
    </logic:messagesPresent>
    <logic:messagesPresent property="howLongTP">
    <c:set var="howLongTPErrs" value="true"/>
    </logic:messagesPresent>
    <c:choose>
    <c:when test="${meetTimeTPErrs or howLongTPErrs}">
    <c:set var="tpClass" value="ErrorField"/>
    </c:when>
    <c:otherwise>
    <c:set var="tpClass" value="BlockContent"/>
    </c:otherwise>
    </c:choose>

    <logic:messagesPresent property="numTimesNT">
    <c:set var="numTimesNTErrs" value="true"/>
    </logic:messagesPresent>
    <logic:messagesPresent property="howLongNT">
    <c:set var="howLongNTErrs" value="true"/>
    </logic:messagesPresent>
    <c:choose>
    <c:when test="${numTimesNTErrs or howLongNTErrs}">
    <c:set var="ntClass" value="ErrorField"/>
    </c:when>
    <c:otherwise>
    <c:set var="ntClass" value="BlockContent"/>
    </c:otherwise>
    </c:choose>
    
<c:forEach var="enable" items="${enableList}">
  <tiles:insert beanName="enable">
    <tiles:put name="tpClass" beanName="tpClass"/>
    <tiles:put name="ntClass" beanName="ntClass"/>
    <tiles:put name="showDuration" beanName="showDuration"/>
  </tiles:insert>
</c:forEach>
