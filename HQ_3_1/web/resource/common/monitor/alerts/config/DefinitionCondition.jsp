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


<tiles:importAttribute name="formName"/>
 <!--<input type="hidden" name="remove.x" id="remove.x"/>-->
<tr>
  <td width="20%" class="BlockLabel">
    <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
    <b><fmt:message key="alert.config.props.CB.IfCondition"/></b>
  </td>
  <logic:messagesPresent property="condition[0].metricId"><td width="80%" class="ErrorField"></logic:messagesPresent>
  <logic:messagesNotPresent property="condition[0].metricId"><td width="80%" class="BlockContent"></logic:messagesNotPresent>
    <html:radio property="condition[0].trigger" value="onMetric"/>
    <fmt:message key="alert.config.props.CB.Content.Metric"/>
    <c:set var="seldd"><fmt:message key="alert.dropdown.SelectOption"/></c:set>
    <html:select property="condition[0].metricId" onchange="javascript:selectMetric('condition[0].metricId', 'condition[0].metricName');">
    <html:option value="-1" key="alert.dropdown.SelectOption"/>
    <c:choose>
    <c:when test="${Resource.entityId.type != 5}"> <%-- group --%>
      <c:choose>
        <c:when test="${not empty ResourceType}">
          <html:optionsCollection property="metrics" label="name" value="id"/>
        </c:when>
        <c:otherwise>
          <html:optionsCollection property="metrics" label="template.name" value="id"/>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
    <html:optionsCollection property="metrics" label="name" value="id"/>
    </c:otherwise>
    </c:choose>
    </html:select>
    <logic:messagesPresent property="condition[0].metricId">
    <span class="ErrorFieldContent">- <html:errors property="condition[0].metricId"/></span>
    </logic:messagesPresent>
    <c:choose>
    <c:when test="${not empty param.metricName}">
      <html:hidden property="condition[0].metricName" value="${param.metricName}"/>
    </c:when>
    <c:otherwise>
      <html:hidden property="condition[0].metricName"/>
    </c:otherwise>
    </c:choose>
  </td>
</tr>
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">
    
    <table width="100%" border="0" cellspacing="0" cellpadding="2">
      <tr> 
        <td nowrap="true"><div style="width: 60px; position: relative;"/><html:img page="/images/schedule_return.gif" width="17" height="21" border="0" align="right"/></td>
        <logic:messagesPresent property="condition[0].absoluteValue"><td width="100%" class="ErrorField"></logic:messagesPresent>
        <logic:messagesNotPresent property="condition[0].absoluteValue"><td width="100%"></logic:messagesNotPresent>
          <html:radio property="condition[0].thresholdType" value="absolute"/>
        
          <fmt:message key="alert.config.props.CB.Content.Is"/>
          <html:select property="condition[0].absoluteComparator">
            <hq:optionMessageList property="comparators" baseKey="alert.config.props.CB.Content.Comparator" filter="true"/>
          </html:select>
          <html:text property="condition[0].absoluteValue" size="8" maxlength="15"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.AbsoluteValue"/>
          <logic:messagesPresent property="condition[0].absoluteValue">
          <br><span class="ErrorFieldContent">- <html:errors property="condition[0].absoluteValue"/></span>
          </logic:messagesPresent>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td width="100%">
          <html:radio property="condition[0].thresholdType" value="changed"/>
          <fmt:message key="alert.config.props.CB.Content.Changes"/>
        </td>
      </tr>
    </table>
    
  </td>
</tr>

<c:if test="${custPropsAvail}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <logic:messagesPresent property="condition[0].customProperty">
  <c:set var="customPropertyErrs" value="true"/>
  </logic:messagesPresent>
  <c:choose>
  <c:when test="${customPropertyErrs}">
  <td class="ErrorField" nowrap>
  </c:when>
  <c:otherwise>
  <td class="BlockContent" nowrap>
  </c:otherwise>
  </c:choose>
    <html:radio property="condition[0].trigger" value="onCustProp"/>
    <fmt:message key="alert.config.props.CB.Content.CustomProperty"/>
    <html:select property="condition[0].customProperty">
      <html:option value="" key="alert.dropdown.SelectOption"/>
      <html:optionsCollection property="customProperties"/>
    </html:select>
    <fmt:message key="alert.config.props.CB.Content.Changes"/>
  </td>
</tr>
</c:if>

<c:if test="${controlEnabled}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <logic:messagesPresent property="condition[0].controlAction">
  <c:set var="controlActionErrs" value="true"/>
  </logic:messagesPresent>
  <logic:messagesPresent property="condition[0].controlActionStatus">
  <c:set var="controlActionStatusErrs" value="true"/>
  </logic:messagesPresent>
  <c:choose>
  <c:when test="${controlActionErrs or controlActionStatusErrs}">
  <td class="ErrorField">
  </c:when>
  <c:otherwise>
  <td class="BlockContent">
  </c:otherwise>
  </c:choose>
    <html:radio property="condition[0].trigger" value="onEvent"/>
    <fmt:message key="alert.config.props.CB.Content.ControlAction"/>&nbsp;
    <html:select property="condition[0].controlAction">
    <html:option value="" key="alert.dropdown.SelectOption"/>
    <html:options property="controlActions"/>
    </html:select>
    &nbsp;<fmt:message key="alert.config.props.CB.Content.Comparator.="/>&nbsp;
    <html:select property="condition[0].controlActionStatus">
    <html:option value="" key="alert.dropdown.SelectOption"/>
    <html:options property="controlActionStatuses"/>
    </html:select>
    <c:if test="${controlActionErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[0].controlAction"/></span>
    </c:if>
    <c:if test="${controlActionStatusErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[0].controlActionStatus"/></span>
    </c:if>
  </td>
</tr>
</c:if>

<c:if test="${logTrackEnabled}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent" nowrap>
    <html:radio property="condition[0].trigger" value="onLog"/>
    <fmt:message key="alert.config.props.CB.Content.Log"/>
    <html:select property="condition[0].logLevel">
      <html:option value="-1" key="any"/>
      <html:option value="3" key="resource.common.monitor.label.events.Error"/>
      <html:option value="4" key="resource.common.monitor.label.events.Warn"/>
      <html:option value="6" key="resource.common.monitor.label.events.Info"/>
      <html:option value="7" key="resource.common.monitor.label.events.Debug"/>
    </html:select>
    <fmt:message key="alert.config.props.CB.Content.Match"/>
    <html:text property="condition[0].logMatch" size="10" maxlength="25"/>
  </td>
</tr>
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent" nowrap>
    <html:radio property="condition[0].trigger" value="onCfgChg"/>
    <fmt:message key="alert.config.props.CB.Content.FileMatch"/>
    <html:text property="condition[0].fileMatch" size="10" maxlength="25"/>
  </td>
</tr>
</c:if>


