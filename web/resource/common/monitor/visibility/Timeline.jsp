<%@ page language="java" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:importAttribute name="hideLogs" ignore="true"/>

<script type="text/javascript">
  var semiIndex = imagePath.indexOf(";");
  if (semiIndex != -1)
    imagePath = imagePath.substring(0, semiIndex);

  <c:forEach var="timeTick" items="${timeIntervals}">
    overlay.times.push('<hq:dateFormatter value="${timeTick.time}"/>');
  </c:forEach>
</script>

<div id="overlay" class="overlay"></div>

<table cellpadding="0" cellspacing="0" border="0" width="100%">
  <tr>
    <td width="10">
      <div id="timetop"></div>
      <html:img page="/images/timeline_ll.gif" height="10"/> 
    </td>
    <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
      <c:set var="count" value="${status.count}"/>
    <td width="9">
      <div id="timePopup_<c:out value="${count - 1}"/>" onmouseover="overlay.delayTimePopup(<c:out value="${count - 1}"/>)" onmousedown="overlay.moveOverlay(this)" onmouseout="overlay.curTime = null">
      <html:img page="/images/timeline_off.gif" height="10" width="9" onmouseover="imageSwap(this, imagePath + 'timeline', '_on')" onmouseout="imageSwap(this, imagePath +  'timeline', '_off');" onmousedown="imageSwap(this, imagePath +  'timeline', '_down')"/> 
      </div>
    </td>
    </c:forEach>
    <td width="100%">
      <html:img page="/images/timeline_lr.gif" height="10"/> 
    </td>
  </tr>
  <tr>
    <td></td>
    <td colspan="<c:out value="${count / 2}"/>" valign="top">
      <hq:dateFormatter value="${timeIntervals[0].time}"/>
      <div id="timePopup" class="timepopup" onmousedown="overlay.hideTimePopup()"></div>
      <div style="height: 16px;"></b>
    </td>
    <td colspan="<c:out value="${count / 2}"/>" align="right" valign="top">
      <hq:dateFormatter value="${timeIntervals[count - 1].time}"/>
    </td>
    <td></td>
  </tr>
</table>

