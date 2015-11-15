<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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
    <img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
    <b><fmt:message key="alert.config.props.CB.IfCondition"/></b>
  </td>
  <c:if test="${fieldErrors.containsKey('condition[0].metricId')}"><td width="80%" class="ErrorField"></c:if>
  <c:if test="${!fieldErrors.containsKey('condition[0].metricId')}"><td width="80%" class="BlockContent"></c:if>
    <s:radio  theme="simple" name="getCondition(0).trigger" list="#{'onMetric':''}" value="%{#attr.defForm.getCondition(0).trigger}"/>
	
    <fmt:message key="alert.config.props.CB.Content.Metric"/>
    <c:set var="seldd"><fmt:message key="alert.dropdown.SelectOption"/></c:set>
    <select name="getCondition(0).metricId" onchange="javascript:selectMetric('getCondition(0).metricId', 'getCondition(0).metricName');">
    <option value="-1" ><s:property value="%{getText('alert.dropdown.SelectOption')}"/></option>
    <c:choose>
    <c:when test="${Resource.entityId.type != 5}"> <%-- group --%>
      <c:choose>
        <c:when test="${not empty ResourceType}">
          <c:forEach var="metric"  items="${defForm.metrics}">
		    
			<option   value="${metric.id}" <c:if test="${metric.id == defForm.getCondition(0).metricId }">
								<c:out value="selected='selected'"/>
								</c:if> > <c:out value="${metric.name}"/></option>
			</c:forEach>
        </c:when>
        <c:otherwise>
		  <c:forEach var="metric"  items="${defForm.metrics}">
			<option   value="${metric.id}" <c:if test="${metric.id == defForm.getCondition(0).metricId || metric.id == param.metricId }">
						<c:out value="selected='selected'"/>
						</c:if> > <c:out value="${metric.template.name}"/></option>     
		  </c:forEach>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
	<c:forEach var="metric"  items="${defForm.metrics}">
		<option   value="${metric.id}"> <c:out value="${metric.name}"/></option>     
	</c:forEach>
    
    </c:otherwise>
    </c:choose>
    </select>
	
    <c:if test="${fieldErrors.containsKey('condition[0].metricId')}">
		<span class="ErrorFieldContent">- &nbsp;<c:out value="${fieldErrors.get('condition[0].metricId').get(0)}" /></span>
    </c:if>
    <c:choose>
    <c:when test="${not empty param.metricName}">
      <input type="hidden""  name="getCondition(0).metricName" value="${param.metricName}"/>
    </c:when>
    <c:otherwise>
		<input type="hidden""  name="getCondition(0).metricName" value="${defForm.getCondition(0).metricName}"/>
    </c:otherwise>
    </c:choose>
  </td>
</tr>
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">
    
    <table width="100%" border="0" cellspacing="0" cellpadding="2">
      <tr> 
        <td nowrap="true"><div style="width: 60px; position: relative;"/><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0" align="right"/></td>
        <c:if test="${fieldErrors.containsKey('condition[0].absoluteValue')}"><td width="100%" class="ErrorField"></c:if>
        <c:if test="${!fieldErrors.containsKey('condition[0].absoluteValue')}"><td width="100%"></c:if>
          <s:radio  theme="simple" name="getCondition(0).thresholdType" list="#{'absolute':''}" value="%{#attr.defForm.getCondition(0).thresholdType}"/>
		         
          <fmt:message key="alert.config.props.CB.Content.Is"/>
		  <select name="getCondition(0).absoluteComparator">
			  <c:forEach var="comparator"  items="${defForm.comparators}">
				<c:if test="${comparator == defForm.getCondition(0).absoluteComparator}">
					<option value="${comparator}" selected><s:property value="%{getText('alert.config.props.CB.Content.Comparator.' + #attr.comparator)}"/></option>
				</c:if>
				<c:if test="${comparator != defForm.getCondition(0).absoluteComparator}">
					<option value="${comparator}" ><s:property value="%{getText('alert.config.props.CB.Content.Comparator.' + #attr.comparator)}"/></option>
				</c:if>
			  </c:forEach>
		  </select>
          
		  
          <s:textfield theme="simple" name="getCondition(0).absoluteValue" value="%{#attr.defForm.getCondition(0).absoluteValue}" size="8" maxlength="15"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.AbsoluteValue"/>
          <c:if test="${fieldErrors.containsKey('condition[0].absoluteValue')}">
          <br><span class="ErrorFieldContent">- &nbsp;<c:out value="${fieldErrors.get('condition[0].absoluteValue').get(0)}" /></span>
          </c:if>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td width="100%">
		  <s:radio  theme="simple" name="getCondition(0).thresholdType" list="#{'changed':''}" value="%{#attr.defForm.getCondition(0).thresholdType}"/>
          
          <fmt:message key="alert.config.props.CB.Content.Changes"/>
        </td>
      </tr>
    </table>
    
  </td>
</tr>

<c:if test="${custPropsAvail}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <c:if test="${fieldErrors.containsKey('condition[0].customProperty')}">
	<c:set var="customPropertyErrs" value="true"/>
  </c:if>
  <c:choose>
  <c:when test="${customPropertyErrs}">
  <td class="ErrorField" nowrap>
  </c:when>
  <c:otherwise>
  <td class="BlockContent" nowrap>
  </c:otherwise>
  </c:choose>
	<s:radio  theme="simple" name="getCondition(0).trigger" list="#{'onCustProp':''}" value="%{#attr.defForm.getCondition(0).trigger}"/>
    
    <fmt:message key="alert.config.props.CB.Content.CustomProperty"/>
	
	 
	<s:select theme="simple" value="%{#attr.defForm.getCondition(0).customProperty}" headerKey="-1" headerValue="%{getText('alert.dropdown.SelectOption')}"  name="getCondition(0).customProperty" list="%{#attr.defForm.customProperties}" listValue="value"   />
    
    <fmt:message key="alert.config.props.CB.Content.Changes"/>
  </td>
</tr>
</c:if>

<c:if test="${controlEnabled}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <c:if test="${fieldErrors.containsKey('condition[0].controlAction')}">
   <c:set var="controlActionErrs" value="true"/>
  </c:if>
  <c:if test="${fieldErrors.containsKey('condition[0].controlActionStatus')}">
	<c:set var="controlActionStatusErrs" value="true"/>
  </c:if>
  <c:choose>
  <c:when test="${controlActionErrs or controlActionStatusErrs}">
  <td class="ErrorField">
  </c:when>
  <c:otherwise>
  <td class="BlockContent">
  </c:otherwise>
  </c:choose>
	<s:radio  theme="simple" name="getCondition(0).trigger" list="#{'onEvent':''}" value="%{#attr.defForm.getCondition(0).trigger}"/> 
    <fmt:message key="alert.config.props.CB.Content.ControlAction"/>&nbsp;
	<s:select theme="simple"  headerKey="-1" headerValue="%{getText('alert.dropdown.SelectOption')}" value="%{#attr.defForm.getCondition(0).controlAction}" name="getCondition(0).controlAction" list="%{#attr.defForm.controlActions}"   />
    &nbsp;<fmt:message key="alert.config.props.CB.Content.Comparator.="/>&nbsp;
    <s:select theme="simple"  headerKey="-1" headerValue="%{getText('alert.dropdown.SelectOption')}" value="%{#attr.defForm.getCondition(0).controlActionStatus}" name="getCondition(0).controlActionStatus" list="controlActionStatuses"   />
    <c:if test="${controlActionErrs}">
    <br><span class="ErrorFieldContent"><c:out value="${fieldErrors.get('condition[0].controlAction').get(0)}" /></span>
    </c:if>
    <c:if test="${controlActionStatusErrs}">
    <br><span class="ErrorFieldContent"> <c:out value="${fieldErrors.get('condition[0].controlActionStatus').get(0)}" /></span>
    </c:if>
  </td>
</tr>
</c:if>

<c:if test="${logTrackEnabled}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent" nowrap>
	<s:radio  theme="simple" name="getCondition(0).trigger" list="#{'onLog':''}" value="%{#attr.defForm.getCondition(0).trigger}"/> 
    <fmt:message key="alert.config.props.CB.Content.Log"/>
    <select name="getCondition(0).logLevel">
      <option value="-1">Any</option>
      <option value="3"  <c:if test="${3 == defForm.getCondition(0).logLevel }">
							<c:out value="selected='selected'"/>
						 </c:if> > <s:property value="%{getText('resource.common.monitor.label.events.Error')}"/></option>
      <option value="4"  <c:if test="${4 == defForm.getCondition(0).logLevel }">
							<c:out value="selected='selected'"/>
						 </c:if>> <s:property value="%{getText('resource.common.monitor.label.events.Warn' )}"/></option>
      <option value="6"  <c:if test="${6 == defForm.getCondition(0).logLevel }">
							<c:out value="selected='selected'"/>
						 </c:if>> <s:property value="%{getText('resource.common.monitor.label.events.Info' )}"/></option>
      <option value="7"  <c:if test="${7 == defForm.getCondition(0).logLevel }">
							<c:out value="selected='selected'"/>
						 </c:if>> <s:property value="%{getText('resource.common.monitor.label.events.Debug')}"/></option>
    </select>
    <fmt:message key="alert.config.props.CB.Content.Match"/>
    <s:textfield theme="simple" name="getCondition(0).logMatch" value="%{#attr.defForm.getCondition(0).logMatch}" size="10" maxlength="150"/>
  </td>
</tr>
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent" nowrap>
	<s:radio  theme="simple" name="getCondition(0).trigger" list="#{'onCfgChg':''}" value="%{#attr.defForm.getCondition(0).trigger}"/> 
    
    <fmt:message key="alert.config.props.CB.Content.FileMatch"/>
    <s:textfield theme="simple" name="getCondition(0).fileMatch" value="%{#attr.defForm.getCondition(0).fileMatch}" size="10" maxlength="150"/>
  </td>
</tr>
</c:if>


