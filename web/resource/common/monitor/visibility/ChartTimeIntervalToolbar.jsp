<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<tiles:importAttribute name="form"/>
<tiles:importAttribute name="formName"/>
<tiles:importAttribute name="rangeNow"/>
<tiles:importAttribute name="begin"/>
<tiles:importAttribute name="end"/>
<tiles:importAttribute name="prevProperty" ignore="true"/>
<tiles:importAttribute name="nextProperty" ignore="true"/>

<c:if test="${empty prevProperty}">
<c:set var="prevProperty" value="prevRange"/>
</c:if>
<c:if test="${empty nextProperty}">
<c:set var="nextProperty" value="nextRange"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockBottomLine" colspan="3"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockContent" width="100%" align="right">
      <html:image property="${prevProperty}" page="/images/tbb_pageleft.gif" border="0"/>
    </td>
    <td class="BlockContent" nowrap>
      <hq:dateFormatter value="${begin}"/>
      &nbsp;<fmt:message key="resource.common.monitor.visibility.chart.to"/>&nbsp;
      <hq:dateFormatter value="${end}"/>
    </td>
    <td class="BlockContent">
      <c:choose>
      <c:when test="${rangeNow}">
      <html:img page="/images/tbb_pageright_gray.gif" border="0"/>
      </c:when>
      <c:otherwise>
      <html:image property="${nextProperty}" page="/images/tbb_pageright.gif" border="0"/>
      </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockContent" align="right">
    <html:link href="javascript:showAdvanced()"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn"/></html:link>
    </td>
    <td class="BlockContent">
      <div id="advancedDisplay" class="dialog" style="width:600px;filter: alpha(opacity=0);opacity: 0;">
        <tiles:insert definition=".resource.common.monitor.visibility.embeddedMetricDisplayRange">
          <tiles:put name="form" beanName="form"/>
          <tiles:put name="formName" beanName="formName"/>
        </tiles:insert>
      </td>
    </div>
    </td>
  </tr>
</table>

<script language="javascript">
  function hideAdvanced() {
    var advancedDiv = $('advancedDisplay');
   /* new Rico.Effect.Position( 'advancedDisplay',
                               advancedDisplay.offsetLeft - advancedDisplay.offsetWidth,
                               advancedDisplay.offsetTop - advancedDisplay.offsetHeight,
                               0,
                               1, // 1 steps
                               {}
                             );*/
    new Effect.Shrink(advancedDiv)
    //new Effect.Fade(advancedDiv, {duration: 0});
  }

  onloads.push( hideAdvanced );
</script>
