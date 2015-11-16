<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>


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


<tiles:importAttribute name="TheControlForm" ignore="true"/>
<tiles:importAttribute name="noRecurrence" ignore="true"/>
<c:if test="${empty TheControlForm}">
 <c:set var="TheControlForm" value="${requestScope[\"org.apache.struts.taglib.html.BEAN\"]}"/>
</c:if>
<jsu:importScript path="/js/schedule.js" />
<jsu:script>
    var imagePath = "/images/";
    var jsPath = "/js/";
    var cssPath = "/css/";
    var isMonitorSchedule = false;
	
</jsu:script>

<!--  SCHEDULE TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.autodiscovery.ScheduleTab"/>
</tiles:insertDefinition>
<!--  /  -->

<!--  SCHEDULE CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 
    <tr valign="top">
		<td class="BlockContent">
		</td>
		<c:if test="${fieldErrors.containsKey('startHour') || fieldErrors.containsKey('startMin')|| fieldErrors.containsKey('numWeeks')|| fieldErrors.containsKey('recurrenceDay')|| fieldErrors.containsKey('numDays')|| fieldErrors.containsKey('numMonths')|| fieldErrors.containsKey('startMonth') || fieldErrors.containsKey('startYear')}"><td class="ErrorField"></c:if>
		<c:if test="${!fieldErrors.containsKey('startHour') && !fieldErrors.containsKey('startMin')&& !fieldErrors.containsKey('numWeeks')&& !fieldErrors.containsKey('recurrenceDay')&& !fieldErrors.containsKey('numDays')&& !fieldErrors.containsKey('numMonths')&& !fieldErrors.containsKey('startMonth') && !fieldErrors.containsKey('startYear')}"><td class="BlockContent"></c:if>
			<s:if test="fieldErrors.containsKey('startHour')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('startHour').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('startMin')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('startMin').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('numWeeks')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('numWeeks').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('recurrenceDay')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('recurrenceDay').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('numDays')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('numDays').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('numMonths')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('numMonths').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('startMonth')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('startMonth').get(0)}" /></span>
			</s:if>
			<s:if test="fieldErrors.containsKey('startYear')">
				<span class="ErrorFieldContent">- <c:out value="${fieldErrors.get('startYear').get(0)}" /></span>
			</s:if>
		</td>
	</tr>
    <tr valign="top">
        <td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="resource.autodiscovery.schedule.Start"/></td>
       <s:if test="%{#attr.immediately=='true'}">
			<td width="80%" class="BlockContent"><s:radio checked="%{immediately}" theme="simple" name="startTime" id="startTime" list="#{'1':getText('resource.autodiscovery.schedule.Immediately')}" value="%{startTime}" onclick="turnOnRecurrence(false)"/></td>
       </s:if>

		<s:else>
			<td width="80%" class="BlockContent"><s:radio  theme="simple" name="startTime" id="startTime" list="#{'1':getText('resource.autodiscovery.schedule.Immediately')}" value="%{startTime}" onclick="turnOnRecurrence(false)"/></td>
       </s:else>
		
    </tr>
<c:if test="${empty noRecurrence}">
    <tr>
        <td class="BlockLabel">&nbsp;</td>
        <td class="BlockContent">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <s:if test="%{#attr.immediately=='true'}">  
						<td><s:radio   theme="simple" name="startTime" list="#{'0':''}" value="%{startTime}" onclick="turnOnRecurrence(true)" />&nbsp;</td>
					</s:if>
					<s:else>
						<td><s:radio checked="true"   theme="simple" name="startTime" list="#{'0':''}" value="%{startTime}" onclick="turnOnRecurrence(true)" />&nbsp;</td>
					 </s:else>
					 <td nowrap>
                        <s:select name="startMonth" value="%{#attr.cForm.startMonth}" theme="simple"  
						list="#{ '0':'01 (Jan)', '1':'02 (Feb)', '2':'03 (Mar)', '3':'04 (Apr)', '4':'05 (May)', '5':'06 (Jun)', '6':'07 (Jul)', '7':'08 (Aug)', '8':'09 (Sep)', '9':'10 (Oct)', '10':'11 (Nov)', '11':'12 (Dec)' }" 
						id="startMonth" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1);">
						</s:select>
                        /
                        <s:select  name="startDay" value="%{#attr.cForm.startDay}" theme="simple"
							list="#{ '1':'01', '2':'02', '3':'03', '4':'04', '5':'05', '6':'06', '7':'07', '8':'08', '9':'09', '10':'10', '11':'11', '12':'12', '13':'13', '14':'14', '15':'15', '16':'16', '17':'17', '18':'18', '19':'19', '20':'20', '21':'21', '22':'22', '23':'23', '24':'24', '25':'25', '26':'26', '27':'27', '28':'28', '29':'29', '30':'30', '31':'31'}" 
						   id="startDay" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1);">
                                 </s:select>
                        /
                        <s:select name="startYear" value="%{#attr.cForm.startYear}"  theme="simple" id="startYear" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1);"
								list="#{#attr.startYearr:#attr.startYearr,
										#attr.startYearr+1:#attr.startYearr+1,
										#attr.startYearr+2:#attr.startYearr+2,
										#attr.startYearr+3:#attr.startYearr+3,
										#attr.startYearr+4:#attr.startYearr+4}"/>
                    </td>
                    <td><s:a href="#" onclick="cal('startMonth', 'startDay', 'startYear'); return false;"><img src='<s:url value="/images/schedule_iconCal.gif"/>' width="19" height="17" hspace="5" border="0"/></s:a></td>
                    <td nowrap>&nbsp;&nbsp;<b>@</b>&nbsp;&nbsp;</td>
                    <td width="100%">
                        <s:textfield theme="simple" name="startHour" value="%{#attr.cForm.startHour}" id="startHour"  size="2" maxlength="2"/> : <s:textfield name="startMin" value="%{#attr.cForm.startMin}" theme="simple" id="startMin" size="2" maxlength="2" />
							
						<s:select name="startAmPm"  theme="simple" 
                            list="#{'am':'AM','pm':'PM'}" value="%{#attr.cForm.startAmPm}">
                        </s:select>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td colspan="4" class="BlockContent"><span class="CaptionText"><fmt:message key="resource.autodiscovery.schedule.recur.Specify"/></span></td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td class="BlockLabel" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
    <tr>
        <td class="BlockContent" colspan="2">
            <div id="recur">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td class="BlockLabel" width="20%"><b><fmt:message key="resource.autodiscovery.schedule.Recur"/></b></td>
                    <td class="BlockContent" width="80%">
                        <s:select theme="simple" name="recurInterval" value="%{#attr.cForm.recurInterval}" id="recurInterval" onchange="getRecurrence();"
								list="#{'recurNever':getText('resource.autodiscovery.schedule.recur.Never'),'recurDaily':getText('resource.autodiscovery.schedule.recur.Daily'),'recurWeekly':getText('resource.autodiscovery.schedule.recur.Weekly'),'recurMonthly':getText('resource.autodiscovery.schedule.recur.Monthly')}" >
                        </s:select>
                    </td>
                </tr>
            </table>
            </div>
        </td>
    </tr>
    <tr>
      <td class="BlockContent">&nbsp;</td>
            <td class="BlockContent">
        <div id="recurNever">&nbsp;</div>
        <div id="recurDaily">
        <table width="100%" border="0" cellspacing="0" cellpadding="0">
          <tr> 
            <td class="BlockContent"><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0"/></td>
           <s:if test="%{#attr.every=='true'}">
			
				<td class="BlockContent"><s:radio checked="%{every}" theme="simple" name="recurrenceFrequencyDaily" list="#{'1':''}" value="%{#attr.cForm.everyDay}"/></td>
			</s:if>

			<s:else>
				<td class="BlockContent"><s:radio  theme="simple" name="recurrenceFrequencyDaily" list="#{'1':''}" value="%{#attr.cForm.everyDay}"/></td>
			</s:else> 
			
            <td class="BlockContent"><fmt:message key="resource.autodiscovery.schedule.recur.Every"/></td>
            <td class="BlockContent"><s:textfield theme="simple" name="numDays" value="%{#attr.cForm.numDays}" id="numDays" size="2" maxlength="2" onchange="toggleRadio('recurrenceFrequencyDaily', 0);"/></td>
            <td class="BlockContent" width="100%"><fmt:message key="resource.autodiscovery.schedule.daily.Days"/></td>
          </tr>
          <tr> 
            <td class="BlockContent">&nbsp;</td>
            <s:if test="%{#attr.every=='true'}">
			
				<td class="BlockContent"><s:radio theme="simple" name="recurrenceFrequencyDaily" value="%{#attr.cForm.everyWeekday}" list="#{'0':''}" /></td>
			</s:if>

			<s:else>
				<td class="BlockContent"><s:radio checked="true" theme="simple" name="recurrenceFrequencyDaily" value="%{#attr.cForm.everyWeekday}" list="#{'0':''}" /></td>
			</s:else> 
			
            <td class="BlockContent" colspan="3"><fmt:message key="resource.autodiscovery.schedule.daily.EveryWeekday"/></td>
          </tr>
        </table>
        </div>
        <div id="recurWeekly">
			<c:set var="days" value="#{'1':'b'}"/>
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
              <tr valign="top"> 
                <td class="BlockContent"><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0"/></td>
                <td class="BlockContent" nowrap><fmt:message key="resource.autodiscovery.schedule.recur.Every"/> 
                  <s:textfield theme="simple" name="numWeeks" value="%{#attr.cForm.numWeeks}" id="numWeeks" size="2" maxlength="2"/>
                  <fmt:message key="resource.autodiscovery.schedule.weekly.WeeksOn"/></td>
                <td class="BlockContent" width="100%">
             <table border="0" cellspacing="0" cellpadding="2">
                   <tr> 
						
                      <td><s:if test="%{#attr.Sunday=='Sunday'}">  
							<input type="checkbox"   name="recurrenceDay" value="1"  checked/>
						
					</s:if>
					<s:else>
					<input type="checkbox"   name="recurrenceDay" value="1"  />
					 </s:else>
					  <fmt:message key="admin.role.alert.Sunday"/></td>
                      <td><s:if test="%{#attr.Monday=='Monday'}">  
						<input type="checkbox"   name="recurrenceDay" value="2"  checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="2"/>
					 </s:else>
					  <fmt:message key="admin.role.alert.Monday"/></td>
					 
                      <td><s:if test="%{#attr.Tuesday=='Tuesday'}">  
						<input type="checkbox"   name="recurrenceDay" value="3"  checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="3"/>
					 </s:else>
					 <fmt:message key="admin.role.alert.Tuesday"/></td>
                      <td><s:if test="%{#attr.Wednesday=='Wednesday'}">  
						<input type="checkbox"   name="recurrenceDay" value="4"  checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="4"/>
					 </s:else>
					 <fmt:message key="admin.role.alert.Wednesday"/></td>
                    </tr>
                    <tr> 
                      <td><s:if test="%{#attr.Thursday=='Thursday'}">  
						<input type="checkbox"   name="recurrenceDay" value="5"  checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="5"/>
					 </s:else>
					  <fmt:message key="admin.role.alert.Thursday"/></td>
                      <td><s:if test="%{#attr.Friday=='Friday'}">  
						<input type="checkbox"   name="recurrenceDay" value="6"  checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="6"/>
					 </s:else>
					   <fmt:message key="admin.role.alert.Friday"/></td>
                      <td><s:if test="%{#attr.Saturday=='Saturday'}">  
						<input type="checkbox"   name="recurrenceDay" value="7" checked/>
					</s:if>
					<s:else>
						<input type="checkbox"   name="recurrenceDay" value="7"/>
					 </s:else>
					  
					  <fmt:message key="admin.role.alert.Saturday"/></td>
                      <td>&nbsp;</td>
                    </tr>
                  </table>
            </td>
              </tr>
            </table>
        </div>
        <div id="recurMonthly">
            <table width="100%" border="0" cellspacing="0" cellpadding="2">
          <tr> 
            <td><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0"/></td>
            <td nowrap><fmt:message key="resource.autodiscovery.schedule.recur.Every"/> 
              <s:textfield theme="simple" name="numMonths" value="%{#attr.cForm.numMonths}" id="numMonths" size="2" maxlength="2" />
              <fmt:message key="resource.autodiscovery.schedule.monthly.MonthsOn"/></td>
            
			 <s:if test="%{#attr.each=='true'}">
			
				<td><s:radio theme="simple" name="recurrenceFrequencyMonthly" list="#{'0':''}" value="%{#attr.cForm.onDay}" /></td>
			</s:if>

			<s:else>
				<td><s:radio checked="true" theme="simple" name="recurrenceFrequencyMonthly" list="#{'0':''}" value="%{#attr.cForm.onDay}" /></td>
			</s:else> 
			
            <td nowrap><fmt:message key="resource.autodiscovery.schedule.monthly.OnThe"/></td>
            <td><s:select theme="simple" name="recurrenceWeek" value="%{#attr.cForm.recurrenceWeek}" id="recurrenceWeek" onchange="toggleRadio('recurrenceFrequencyMonthly', 0);"
               list="#{'1':getText('resource.autodiscovery.schedule.monthly.first'),'2':getText('resource.autodiscovery.schedule.monthly.second'),'3':getText('resource.autodiscovery.schedule.monthly.third'),'4':getText('resource.autodiscovery.schedule.monthly.fourth')}" /> 
				
			  <s:select theme="simple" name="monthlyRecurrenceDay" value="%{#attr.cForm.monthlyRecurrenceDay}" id="recurrenceDay" onchange="toggleRadio('recurrenceFrequencyMonthly', 0);"
              list="#{'1':getText('admin.role.alert.Sunday'),'2':getText('admin.role.alert.Monday'),'3':getText('admin.role.alert.Tuesday'),'4':getText('admin.role.alert.Wednesday'),'5':getText('admin.role.alert.Thursday'),'6':getText('admin.role.alert.Friday'),'7':getText('admin.role.alert.Saturday')}" /> 			  
			  </td>
          </tr>
          <tr> 
            <td>&nbsp;</td>
            <td>&nbsp;</td>
           <s:if test="%{#attr.each=='true'}">
			
				<td> <s:radio checked="%{each}" theme="simple" name="recurrenceFrequencyMonthly" list="#{'1':''}" value="%{#attr.cForm.onEach}"/></td>
			</s:if>

			<s:else>
				<td> <s:radio theme="simple" name="recurrenceFrequencyMonthly" list="#{'1':''}" value="%{#attr.cForm.onEach}"/></td>
			</s:else>  
			
            <td><fmt:message key="resource.autodiscovery.schedule.monthly.Each"/></td>
            <td width="100%">
            <s:select theme="simple" name="eachDay" value="%{#attr.cForm.eachDay}"
			 list="#{ '1':'01', '2':'02', '3':'03', '4':'04', '5':'05', '6':'06', '7':'07', '8':'08', '9':'09', '10':'10', '11':'11', '12':'12', '13':'13', '14':'14', '15':'15', '16':'16', '17':'17', '18':'18', '19':'19', '20':'20', '21':'21', '22':'22', '23':'23', '24':'24', '25':'25', '26':'26', '27':'27', '28':'28', '29':'29', '30':'30', '31':'31'}" 
			 id="startDay" onchange="toggleRadio('recurrenceFrequencyMonthly', 1);">
           </s:select>
            <fmt:message key="resource.autodiscovery.schedule.monthly.OfTheMonth"/>
            </td>
          </tr>
        </table>
    </div>
      </td>
    </tr>
    <tr>
      <td class="BlockLabel" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
    <tr>
        <td colspan="2" class="BlockContent">
            <div id="recurrenceEnd">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td width="20%" class="BlockLabel" valign="top"><b><fmt:message key="resource.autodiscovery.schedule.RecurrenceEnd"/></b></td>
					<s:if test="%{#attr.noEnd=='true'}">
			
						<td width="80%" class="BlockContent"><s:radio checked="%{noEnd}" theme="simple" name="endTime"  list="#{'1':''}" value="%{#attr.cForm.none}" /> <fmt:message key="resource.autodiscovery.schedule.NoEnd"/></td>
					</s:if>

					<s:else>
						<td width="80%" class="BlockContent"><s:radio  theme="simple" name="endTime"  list="#{'1':''}" value="%{#attr.cForm.none}" /> <fmt:message key="resource.autodiscovery.schedule.NoEnd"/></td>
					</s:else>
				 
                </tr>
                <tr>
                  <td class="BlockLabel">&nbsp;</td>
                  <td class="BlockContent">
                        <table width="100%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                                <td nowrap>
								
									<s:if test="%{#attr.noEnd=='true'}">
			
										<s:radio   theme="simple" name="endTime" list="#{'0':''}" value="%{#attr.cForm.onDate}"/>
									</s:if>

									<s:else>
										<s:radio checked="true" theme="simple" name="endTime" list="#{'0':''}" value="%{#attr.cForm.onDate}"/>
									</s:else>
									
                                    <s:select theme="simple" name="endMonth" value="%{#attr.cForm.endMonth}" id="endMonth" onchange="toggleRadio('endTime', 1);"
                                        list="#{ '0':'01 (Jan)', '1':'02 (Feb)', '2':'03 (Mar)', '3':'04 (Apr)', '4':'05 (May)', '5':'06 (Jun)', '6':'07 (Jul)', '7':'08 (Aug)', '8':'09 (Sep)', '9':'10 (Oct)', '10':'11 (Nov)', '11':'12 (Dec)' }" >
										</s:select>
                                    /
                                    <s:select theme="simple" name="endDay" value="%{#attr.cForm.endDay}" id="endDay" onchange="toggleRadio('endTime', 1);"
                                        list="#{ '1':'01', '2':'02', '3':'03', '4':'04', '5':'05', '6':'06', '7':'07', '8':'08', '9':'09', '10':'10', '11':'11', '12':'12', '13':'13', '14':'14', '15':'15', '16':'16', '17':'17', '18':'18', '19':'19', '20':'20', '21':'21', '22':'22', '23':'23', '24':'24', '25':'25', '26':'26', '27':'27', '28':'28', '29':'29', '30':'30', '31':'31'}" >                                    
										</s:select>
                                    /
                                    <s:select theme="simple" name="endYear" value="%{#attr.cForm.endYear}" id="endYear" onchange="toggleRadio('endTime', 1);"
										list="#{#attr.startYearr:#attr.startYearr,
										#attr.startYearr+1:#attr.startYearr+1,
										#attr.startYearr+2:#attr.startYearr+2,
										#attr.startYearr+3:#attr.startYearr+3,
										#attr.startYearr+4:#attr.startYearr+4}"/>
                                       <!-- Value and text of options are replaced by script, see beginning of schedule.js 
                                            Dummy value attribute included since Tag Lib DTD requires it to exis  -->
                                       									
										   
                                </td>
                                <td><s:a href="#" onclick="cal('endMonth', 'endDay', 'endYear'); return false;"><img src='<s:url value="/images/schedule_iconCal.gif"/>' width="19" height="17"  hspace="5" border="0"/></s:a></td>
                                <td width="100%">&nbsp;</td>
                            </tr>
                            </table>
                  </td>
                </tr>
                <tr>
                    <td class="BlockLabel">&nbsp;</td>
                    <td class="BlockContent">&nbsp;</td>        
                </tr>
            </table>
      </div>
     </td>
    </tr>
</c:if>
    <tr>
     <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->

<jsu:script>
    <c:choose>
        <c:when test="${param.mode eq 'edit'}">
									<s:if test="%{#attr.TheControlForm.endYear=='' || #attr.TheControlForm.endMonth==''|| #attr.TheControlForm.endDay==''||#attr.TheControlForm.endYear==null || #attr.TheControlForm.endMonth==null|| #attr.TheControlForm.endDay==null}">
			
										init(<c:out escapeXml="false" value="${TheControlForm.startMonth}"/>, <c:out escapeXml="false" value="${TheControlForm.startDay}"/>, <c:out escapeXml="false" value="${TheControlForm.startYear}"/>, <c:out escapeXml="false" value="0"/>, <c:out escapeXml="false" value="0"/>, <c:out escapeXml="false" value="0"/>, <c:out escapeXml="false" value="\"${TheControlForm.recurInterval}\""/>);
									
									</s:if>

									<s:else>
										init(<c:out escapeXml="false" value="${TheControlForm.startMonth}"/>, <c:out escapeXml="false" value="${TheControlForm.startDay}"/>, <c:out escapeXml="false" value="${TheControlForm.startYear}"/>, <c:out escapeXml="false" value="${TheControlForm.endMonth}"/>, <c:out escapeXml="false" value="${TheControlForm.endDay}"/>, <c:out escapeXml="false" value="${TheControlForm.endYear}"/>, <c:out escapeXml="false" value="\"${TheControlForm.recurInterval}\""/>);
									
									</s:else> 
			
        </c:when>
        <c:otherwise>
            <%-- an error occurred, for a 'new' mode. still init the select boxes. --%>
            
                init();
            
        </c:otherwise>
    </c:choose>
    toggleRecurrence("startTime");
</jsu:script>
