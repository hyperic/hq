<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<jsu:script>
  	var help = "<hq:help/>";
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PortletTitle" nowrap><fmt:message key="dash.home.ControlActions.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="1" height="32" alt="" border="0"/></td>
    <td width="1%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan="2">
      <html:form action="/dashboard/ModifyControlActions" >
<div id="narrowlist_false">
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>
</div>
      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>

      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr valign="top">
          <td width="20%" class="BlockLabel" rowspan="3"><fmt:message key="dash.settings.FormLabel.ControlRange"/></td>
          <td width="5%" class="BlockContent" nowrap>
             <html:checkbox property="useLastCompleted" disabled="${not sessionScope.modifyDashboard}"/>
             <fmt:message key="dash.settings.controlActions.last"/>   
          </td>
             <td width="75%" class="BlockContent">
                  <html:select property="lastCompleted" disabled="${not sessionScope.modifyDashboard}" >
                      <html:option value="1">1</html:option>
                      <html:option value="5">5</html:option>
                      <html:option value="10">10</html:option>
                      <html:option value="15">15</html:option>
                  </html:select>
                  <fmt:message key="dash.settings.controlActions.completed"/>
                  <html:select property="past" disabled="${not sessionScope.modifyDashboard}">
                      <html:option value="1800000">30
                          <fmt:message key="admin.settings.Minutes"/>
                      </html:option>
                      <html:option value="3600000">
                          <fmt:message key="admin.settings.Hour"/>
                      </html:option>
                      <html:option value="43200000">12
                          <fmt:message key="admin.settings.Hours"/>
                      </html:option>
                      <html:option value="86400000">
                          <fmt:message key="admin.settings.Day"/>
                      </html:option>
                      <html:option value="604800000">
                          <fmt:message key="admin.settings.Week"/>
                      </html:option>
                      <html:option value="2419200000">
                          <fmt:message key="admin.settings.Month"/>
                      </html:option>
                  </html:select>
                 <br>
             </td>
        </tr>
        <tr>
            <td class="BlockContent" nowrap>
                <html:checkbox property="useMostFrequent" disabled="${not sessionScope.modifyDashboard}"/>
                <fmt:message key="dash.settings.controlActions.last"/>
            </td>
          <td class="BlockContent">
           <html:select property="mostFrequent" disabled="${not sessionScope.modifyDashboard}">
               <html:option value="1">1</html:option>
               <html:option value="5">5</html:option>
               <html:option value="10">10</html:option>
               <html:option value="15">15</html:option>
           </html:select>
           <fmt:message key="dash.settings.controlActions.most.frequent"/><br>
          </td>
        </tr>
        <tr>
          <td colspan="3" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
      <tiles:insert definition=".form.buttons">
      <c:if test='${not sessionScope.modifyDashboard}'>
        <tiles:put name="cancelOnly" value="true"/>
        <tiles:put name="noReset" value="true"/>
      </c:if>
      </tiles:insert>
      </html:form>
    </td>
  </tr>
  <tr> 
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
