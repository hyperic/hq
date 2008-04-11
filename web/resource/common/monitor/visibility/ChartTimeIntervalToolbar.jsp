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

<table width="100%" cellpadding="0" cellspacing="0" border="0" id="advancedTbl">
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
    <td colspan="3" class="BlockContent" align="right">
    <html:link href="javascript:moveIt(dojo.byId('advancedDisplay'), null,'75px');showAdvanced();"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn"/></html:link>
    </td>
    </tr>
    <tr>
    <td class="BlockContent" colspan="3">
      <div id="advancedDisplay" class="dialog" style="filter: alpha(opacity=0);opacity: 0;">
        <tiles:insert definition=".resource.common.monitor.visibility.embeddedMetricDisplayRange">
          <tiles:put name="form" beanName="form"/>
          <tiles:put name="formName" beanName="formName"/>
        </tiles:insert>
    </div>
    </td>
  </tr>
</table>

<script type="text/javascript">
  function hideAdvanced() {
    var advancedDiv = dojo.byId('advancedDisplay');
    advancedDiv.style.display='none';
    new Effect.MoveBy(advancedDiv.parentNode, 0, 0 );
  }

function moveIt(obj, mvTop, mvLeft) {
	obj.style.position = "absolute";
	obj.style.top = mvTop;
	obj.style.left = mvLeft;
    obj.style.display = '';
}

  onloads.push( hideAdvanced );

    
</script>
