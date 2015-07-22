<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<jsu:script>
  	var help = "<hq:help/>";
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle">
    <td rowspan="99"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0" /></td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0" /></td>
    <td width="67%" class="PageTitle" nowrap><fmt:message key="dash.home.ControlActions.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="202" height="32" alt="" border="0" /></td>
    <td width="1%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0" /></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
	<td colspan="3"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0" /></td>
  </tr>
  <tr valign="top"> 
    <td colspan="2">
      <s:form action="updateControlActionsModifyPortlet" >
<div id="narrowlist_false">
	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insertDefinition>
</div>
	  <tiles:insertDefinition name=".ng.dashContent.admin.generalSettings">
        <tiles:putAttribute name="portletName" value="${portletName}"/>
      </tiles:insertDefinition>

      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr valign="top">
          <td width="20%" class="BlockLabel" rowspan="3"><fmt:message key="dash.settings.FormLabel.ControlRange"/></td>
          <td width="5%" class="BlockContent" nowrap>
			 <s:checkbox theme="simple" name="useLastCompleted"  id="useLastCompleted" value="%{#attr.useLastCompleted}"  disabled="%{!#attr.modifyDashboard}" />
             <fmt:message key="dash.settings.controlActions.last"/>   
          </td>
             <td width="75%" class="BlockContent">
			 <s:select theme="simple" name="lastCompleted" disabled="%{#!attr.modifyDashboard}"  list="#{ '1':'1', '5':'5', '10':'10', '15':'15' }" value="%{#attr.lastCompleted}"  />
             <fmt:message key="dash.settings.controlActions.completed"/>
			 <s:select theme="simple" name="past" disabled="%{#!attr.modifyDashboard}" value="%{#attr.past}" list="#{ '1800000':'30 ' + getText('admin.settings.Minutes'), '3600000':getText('admin.settings.Hour'), '43200000':'12 '+getText('admin.settings.Hours'), '86400000':getText('admin.settings.Day') ,'604800000':getText('admin.settings.Week') ,'2419200000':getText('admin.settings.Month') }" />
                 <br>
             </td>
        </tr>
        <tr>
            <td class="BlockContent" nowrap>
				<s:checkbox theme="simple" name="useMostFrequent"  id="useMostFrequent" value="%{#attr.useMostFrequent}"  disabled="%{!#attr.modifyDashboard}" />
                <fmt:message key="dash.settings.controlActions.last"/>
            </td>
          <td class="BlockContent">
		  <s:select theme="simple" name="mostFrequent" disabled="%{#!attr.modifyDashboard}"  list="#{ '1':'1', '5':'5', '10':'10', '15':'15' }" value="%{#attr.mostFrequent}"  />
           <fmt:message key="dash.settings.controlActions.most.frequent"/><br>
          </td>
        </tr>
        <tr>
		  <td colspan="3" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /> </td>
        </tr>
      </table>
	  <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelControlActionsModifyPortlet" />
			<tiles:putAttribute name="resetAction"   value="resetControlActionsModifyPortlet" />
		  </c:if>
      </tiles:insertDefinition>
      </s:form>
    </td>
  </tr>
  <tr> 
	<td colspan="3"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0" /></td>
  </tr>
</table>
