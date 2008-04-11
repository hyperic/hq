<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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


<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.categories"/></td>
  </tr>
  <tr> 
    <td class="ListCell" width="50%" nowrap="true"> 
      <html:multibox property="filter" value="0"/>
      <fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></td>
    <td class="ListCell" width="50%" nowrap="true"> 
      <html:multibox property="filter" value="1"/>
      <fmt:message key="resource.common.monitor.visibility.UtilizationTH"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="2"/>
      <fmt:message key="resource.common.monitor.visibility.UsageTH"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="3"/>
      <fmt:message key="resource.common.monitor.visibility.PerformanceTH"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.valueTypes"/></td>
  </tr>
  <tr> 
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="4"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.dynamic"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="5"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsup"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="6"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsdown"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="7"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.static"/></td>
  </tr>
</table>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td class="ListCell" align="right"> 
      <fmt:message key="resource.hub.search.KeywordSearchLabel"/>
      <html:text property="keyword" size="20"/>
    </td>
    <td class="ListCell" align="left">
      <html:hidden property="showAll" name="MetricsDisplayForm"/>
      <html:image property="filterSubmit" page="/images/4.0/icons/accept.png" border="0"/>
    </td>
  </tr>
</table>
