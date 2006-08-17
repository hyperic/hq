<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
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


<tiles:importAttribute name="form" ignore="true"/>
<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="eid" ignore="true"/>
<tiles:importAttribute name="view" ignore="true"/>

<c:if test="${not empty mode}">
  <input type="hidden" name="mode" value="<c:out value="${mode}"/>"/>
</c:if>
<c:if test="${not empty eid}">
  <input type="hidden" name="eid" value="<c:out value="${eid}"/>"/>
</c:if>
<c:if test="${not empty view}">
  <input type="hidden" name="view" value="<c:out value="${view}"/>"/>
</c:if>

<c:choose>
  <c:when test="${not empty form}">
    <%-- used only for forms that are not MetricsDisplayForm --%>
    <tiles:importAttribute name="formName"/>
    <c:set var="readOnly" value="${form.readOnly}"/>
    <c:set var="rangeBegin" value="${form.rb}"/>
    <c:set var="rangeEnd" value="${form.re}"/>
    <c:set var="showBaseline" value="false"/>
  </c:when>
  <c:otherwise>
    <c:set var="formName" value="MetricsDisplayForm"/>
    <c:set var="readOnly" value="${MetricsDisplayForm.readOnly}"/>
    <c:set var="rangeBegin" value="${MetricsDisplayForm.rb}"/>
    <c:set var="rangeEnd" value="${MetricsDisplayForm.re}"/>
    <c:set var="highlighted" value="${MetricsDisplayForm.h}"/>
    <c:set var="showBaseline" value="${MetricsDisplayForm.showBaseline}"/>
  </c:otherwise>
</c:choose>

<hq:dateFormatter var="rb" value="${rangeBegin}"/>
<hq:dateFormatter var="re" value="${rangeEnd}"/>

<!-- Table Content -->
<table width="100%" cellspacing="0" border="0" class="MonitorToolbar">
<c:if test="${showBaseline}">
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.HighlightMetricsLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td>
            <html:select styleClass="FilterFormText" property="hv">
              <html:option value=""/>
              <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.LowValue"/>
              <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.AverageValue"/>
              <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.PeakValue"/>
              <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.LastValue"/>
            </html:select>
          </td>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.is"/></td>
          <td nowrap>
            <html:select styleClass="FilterFormText" property="hp">
              <html:option value=""/>
              <html:option value="5" key="resource.common.monitor.visibility.metricsToolbar.5%"/>
              <html:option value="10" key="resource.common.monitor.visibility.metricsToolbar.10%"/>
              <html:option value="20" key="resource.common.monitor.visibility.metricsToolbar.20%"/>
              <html:option value="30" key="resource.common.monitor.visibility.metricsToolbar.30%"/>
              <html:option value="40" key="resource.common.monitor.visibility.metricsToolbar.40%"/>
              <html:option value="50" key="resource.common.monitor.visibility.metricsToolbar.50%"/>
              <html:option value="60" key="resource.common.monitor.visibility.metricsToolbar.60%"/>
              <html:option value="70" key="resource.common.monitor.visibility.metricsToolbar.70%"/>
              <html:option value="80" key="resource.common.monitor.visibility.metricsToolbar.80%"/>
              <html:option value="90" key="resource.common.monitor.visibility.metricsToolbar.90%"/>
              <html:option value="100" key="resource.common.monitor.visibility.metricsToolbar.100%"/>
            </html:select> 
            <html:select styleClass="FilterFormText" property="ht">
              <html:option value=""/>
              <hq:optionMessageList property="highlightThresholdMenu" baseKey="resource.common.monitor.visibility.metricsToolbar" filter="true"/>
            </html:select>
          </td>
          <td><html:image property="highlight" page="/images/dash-button_go-arrow.gif" border="0"/></td>
<c:choose>
  <c:when test="${highlighted}">
          <td width="100%"><html:link href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'clear')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.ClearHighlightingBtn"/></html:link></td>
  </c:when>
  <c:otherwise>
          <td width="100%">&nbsp;</td>
  </c:otherwise>
</c:choose>
        </tr>
      </table>
    </td>
  </tr>
</c:if>
<c:choose>
  <c:when test="${readOnly}">
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.MetricDisplayRangeLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td width="100%"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.DateRange"><fmt:param value="${rb}"/><fmt:param value="${re}"/></fmt:message> <html:link href="javascript:showAdvanced()"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn"/></html:link></td>
          <td width="100%"><html:img page="/images/spacer.gif" width="1" height="21" alt="" border="0"/></td>
        </tr>
      </table>
      <html:hidden property="rn"/>
      <html:hidden property="ru"/>
    </td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.MetricDisplayRangeLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.Last"/></td>
          <td nowrap>
            <html:select styleClass="FilterFormText" property="rn" styleId="simpleRn">
              <html:optionsCollection property="rnMenu"/>
            </html:select> 
            <html:select styleClass="FilterFormText" property="ru" styleId="simpleRu">
<!--
              <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.CollectionPoints"/>
-->
              <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.Minutes"/>
              <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.Hours"/>
              <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.Days"/>
            </html:select>
          </td>
          <td><html:image property="range" page="/images/dash-button_go-arrow.gif" border="0"/></td>
          <td width="100%">
            <html:link href="javascript:showAdvanced()"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.AdvancedSettingsBtn"/></html:link>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  </c:otherwise>
</c:choose>
</table>
  <div id="advancedDisplay" class="dialog" style="width:600px;filter: alpha(opacity=0);opacity: 0;">
  <div>
    <c:set var="startMonth" value="${form.startMonth}"/>
    <c:set var="startDay" value="${form.startDay}"/>
    <c:set var="startYear" value="${form.startYear}"/>
    <c:set var="endMonth" value="${form.endMonth}"/>
    <c:set var="endDay" value="${form.endDay}"/>
    <c:set var="endYear" value="${form.endYear}"/>

<script src="<html:rewrite page="/js/"/>schedule.js" type="text/javascript"></script>
<script src="<html:rewrite page="/js/"/>monitorSchedule.js" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
 var jsPath = "<html:rewrite page="/js/"/>";
 var cssPath = "<html:rewrite page="/css/"/>";
 
 var isMonitorSchedule = true;
</script>

  <table width="100%">
    <tr>
      <td>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="20%" class="SmokeyLabel"><fmt:message key="resource.common.monitor.visibility.DefineRangeLabel"/></td>
<logic:messagesPresent property="rn">
    <td width="80%" class="ErrorField">
</logic:messagesPresent>
<logic:messagesNotPresent property="rn">
    <td width="80%" class="SmokeyContent">
</logic:messagesNotPresent>
      <html:radio property="a" value="1"/>
      <fmt:message key="monitoring.baseline.BlockContent.Last"/>&nbsp;&nbsp;
      <html:text property="rn" size="2" maxlength="3" onfocus="toggleRadio('a', 0);"/> 
      <html:select property="ru" onchange="toggleRadio('a', 0);">
<!--
        <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.CollectionPoints"/>
-->
        <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.Minutes"/>
        <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.Hours"/>
        <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.Days"/>
      </html:select>
<logic:messagesPresent property="rn">
  <span class="ErrorFieldContent">- <html:errors property="rn"/></span>
</logic:messagesPresent>
    </td>
  </tr>

  <tr>
    <td class="SmokeyLabel">&nbsp;</td>
<logic:messagesPresent property="endDate">
    <td width="80%" class="ErrorField">
</logic:messagesPresent>
<logic:messagesNotPresent property="endDate">
    <td width="80%" class="SmokeyContent">
</logic:messagesNotPresent>
      <html:radio property="a" value="2"/>
      <fmt:message key="monitoring.baseline.BlockContent.WithinRange"/>
      <logic:messagesPresent>
      <span class="ErrorField"><html:errors/></span>
      </logic:messagesPresent>
      <br>

      <table width="100%" border="0" cellspacing="0" cellpadding="2">
        <tr> 
          <td><html:img page="/images/spacer.gif" width="20" height="20" border="0"/></td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.From"/></td>
          <td width="100%">
            <html:select property="startMonth" styleId="startMonth" onchange="toggleRadio('a', 1); changeMonitorDropDown('startMonth', 'startDay', 'startYear');"> 
              <html:option value="0">01 (Jan)</html:option>
              <html:option value="1">02 (Feb)</html:option>
              <html:option value="2">03 (Mar)</html:option>
              <html:option value="3">04 (Apr)</html:option>
              <html:option value="4">05 (May)</html:option>
              <html:option value="5">06 (Jun)</html:option>
              <html:option value="6">07 (Jul)</html:option>
              <html:option value="7">08 (Aug)</html:option>
              <html:option value="8">09 (Sep)</html:option>
              <html:option value="9">10 (Oct)</html:option>
              <html:option value="10">11 (Nov)</html:option>
              <html:option value="11">12 (Dec)</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="startDay" styleId="startDay" onchange="toggleRadio('a', 1);"> 
              <html:option value="1">01</html:option>
              <html:option value="2">02</html:option>
              <html:option value="3">03</html:option>
              <html:option value="4">04</html:option>
              <html:option value="5">05</html:option>
              <html:option value="6">06</html:option>
              <html:option value="7">07</html:option>
              <html:option value="8">08</html:option>
              <html:option value="9">09</html:option>
              <html:option value="10">10</html:option>
              <html:option value="11">11</html:option>
              <html:option value="12">12</html:option>
              <html:option value="13">13</html:option>
              <html:option value="14">14</html:option>
              <html:option value="15">15</html:option>
              <html:option value="16">16</html:option>
              <html:option value="17">17</html:option>
              <html:option value="18">18</html:option>
              <html:option value="19">19</html:option>
              <html:option value="20">20</html:option>
              <html:option value="21">21</html:option>
              <html:option value="22">22</html:option>
              <html:option value="23">23</html:option>
              <html:option value="24">24</html:option>
              <html:option value="25">25</html:option>
              <html:option value="26">26</html:option>
              <html:option value="27">27</html:option>
              <html:option value="28">28</html:option>
              <html:option value="29">29</html:option>
              <html:option value="30">30</html:option>
              <html:option value="31">31</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="startYear" styleId="startYear" onchange="toggleRadio('a', 1); changeMonitorDropDown('startMonth', 'startDay', 'startYear');"> 
              <html:options property="yearOptions"/>
            </html:select>&nbsp;<html:link href="#" onclick="toggleRadio('a', 1); calMonitor('startMonth', 'startDay', 'startYear'); return false;"><html:img page="/images/schedule_iconCal.gif" width="19" height="17" alt="" border="0"/></html:link>
            &nbsp;@&nbsp;
            <html:text property="startHour" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;:&nbsp;<html:text property="startMin" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;
            <html:select property="startAmPm" onchange="toggleRadio('a', 1);"> 
              <html:option value="am">AM</html:option>
              <html:option value="pm">PM</html:option>
            </html:select>&nbsp;
        </tr>
        <tr> 
          <td>&nbsp;</td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.To"/>&nbsp;</td>
          <td width="100%">
            <html:select property="endMonth" styleId="endMonth" onchange="toggleRadio('a', 1); changeMonitorDropDown('endMonth', 'endDay', 'endYear');"> 
              <html:option value="0">01 (Jan)</html:option>
              <html:option value="1">02 (Feb)</html:option>
              <html:option value="2">03 (Mar)</html:option>
              <html:option value="3">04 (Apr)</html:option>
              <html:option value="4">05 (May)</html:option>
              <html:option value="5">06 (Jun)</html:option>
              <html:option value="6">07 (Jul)</html:option>
              <html:option value="7">08 (Aug)</html:option>
              <html:option value="8">09 (Sep)</html:option>
              <html:option value="9">10 (Oct)</html:option>
              <html:option value="10">11 (Nov)</html:option>
              <html:option value="11">12 (Dec)</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="endDay" styleId="endDay" onchange="toggleRadio('a', 1);"> 
              <html:option value="1">01</html:option>
              <html:option value="2">02</html:option>
              <html:option value="3">03</html:option>
              <html:option value="4">04</html:option>
              <html:option value="5">05</html:option>
              <html:option value="6">06</html:option>
              <html:option value="7">07</html:option>
              <html:option value="8">08</html:option>
              <html:option value="9">09</html:option>
              <html:option value="10">10</html:option>
              <html:option value="11">11</html:option>
              <html:option value="12">12</html:option>
              <html:option value="13">13</html:option>
              <html:option value="14">14</html:option>
              <html:option value="15">15</html:option>
              <html:option value="16">16</html:option>
              <html:option value="17">17</html:option>
              <html:option value="18">18</html:option>
              <html:option value="19">19</html:option>
              <html:option value="20">20</html:option>
              <html:option value="21">21</html:option>
              <html:option value="22">22</html:option>
              <html:option value="23">23</html:option>
              <html:option value="24">24</html:option>
              <html:option value="25">25</html:option>
              <html:option value="26">26</html:option>
              <html:option value="27">27</html:option>
              <html:option value="28">28</html:option>
              <html:option value="29">29</html:option>
              <html:option value="30">30</html:option>
              <html:option value="31">31</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="endYear" styleId="endYear" onchange="toggleRadio('a', 1); changeMonitorDropDown('endMonth', 'endDay', 'endYear');">
              <html:options property="yearOptions"/>
            </html:select>&nbsp;<html:link href="#" onclick="toggleRadio('a', 1); calMonitor('endMonth', 'endDay', 'endYear'); return false;"><html:img page="/images/schedule_iconCal.gif" width="19" height="17" alt="" border="0"/></html:link>
            &nbsp;@&nbsp;
            <html:text property="endHour" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;:&nbsp;<html:text property="endMin" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;
            <html:select property="endAmPm" onchange="toggleRadio('a', 1);">
              <html:option value="am">AM</html:option>
              <html:option value="pm">PM</html:option>
            </html:select>&nbsp;
        </tr>
<logic:messagesPresent property="endDate">
        <tr> 
          <td colspan="2">&nbsp;</td>
          <td>
            <span class="ErrorFieldContent">- <html:errors property="endDate"/></span>
          </td>
        </tr>
</logic:messagesPresent>
      </table>

    </td>
  </tr>
  <c:if test="${showRedraw}">
  <tr>
      <td class="SmokeyLabel">&nbsp;</td>
      <td class="SmokeyContent"><html:image property="redraw" page="/images/fb_redraw.gif" border="0" onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');" onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')"/></td>
  </tr>
  </c:if>
    <tr>
    <td class="SmokeyLabel">&nbsp;</td>
    <td class="SmokeyContent"><span class="CaptionText"><fmt:message key="resource.common.monitor.visibility.TheseSettings"/></span>
    <html:image property="advanced" page="/images/fb_redraw.gif" border="0" onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');" onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')"/></td>
  <script language="javascript">
      if (/MSIE/.test(navigator.userAgent)) {
        document.write('<tr><td colspan="2" height="18">&nbsp;</td></tr>');
      }
  </script>
</table>
      </td>
      <td align="right" valign="top">
        <a href="javascript:cancelAdvanced()"><html:img page="/images/dash-icon_delete.gif" border="0"/></a>
      </td>
    </tr>
  </table>
  </div>
</div>
<!--  /  -->
<script type="text/javascript">
  function hideAdvanced() {
    var advancedDiv = $('advancedDisplay');
    new Rico.Effect.Position( 'advancedDisplay',
                               null, // move across y axis
                               advancedDiv.offsetTop - advancedDiv.offsetHeight,
                               0,
                               1, // 1 steps
                               {}
                             );
    Rico.Corner.round(advancedDiv , {corners:"tl,br",compact:true});
    new Effect.Fade(advancedDiv, {duration: 0});
  }

  function showAdvanced() {
    new Effect.Appear('advancedDisplay', {to: 0.85});
    if ($('simpleRn'))
        $('simpleRn').disabled = true;
    if ($('simpleRu'))
        $('simpleRu').disabled = true;
    $('advancedDisplay').style.visibility = "visible";
  }

  function cancelAdvanced() {
    if ($('simpleRn'))
        $('simpleRn').disabled = false;
    if ($('simpleRu'))
        $('simpleRu').disabled = false;
    new Effect.Fade('advancedDisplay');
  }

  onloads.push( hideAdvanced );

</script>
