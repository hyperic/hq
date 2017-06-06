<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>


<jsu:importScript path="/js/pageLayout.js" />
<jsu:script>
	var noDelete = true;
	var imagePath = "/images/";
</jsu:script>
<!--  GENERAL PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.group.Control.Behavior.Tab"/>
</tiles:insertDefinition>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.Control.Behavior.Label.Occur"/></td>
  <td width="80%" class="BlockContent">
   <s:if test="%{#attr.parallel=='true'}">
			<s:radio checked="%{parallel}" theme="simple" name="inParallel" id="inParallel" list="#{'true':getText('resource.group.Control.Behavior.Content.Parallel')}" value="%{inParallel}"/>
       </s:if>

		<s:else>
			<s:radio  theme="simple" name="inParallel" id="inParallel" list="#{'true':getText('resource.group.Control.Behavior.Content.Parallel')}" value="%{inParallel}"/>
       </s:else>


 </tr>
 <tr valign="top">
  <td width="20%" class="BlockLabel">&nbsp;</td>
  <td width="80%" class="BlockContent">
  <s:if test="%{#attr.parallel=='true'}">
			<s:radio theme="simple" name="inParallel" id="inParallel" list="#{'false':getText('resource.group.Control.Behavior.Content.Order')}" value="%{inParallel}"/>
       </s:if>

		<s:else>
			<s:radio checked="%{parallel}" theme="simple" name="inParallel" id="inParallel" list="#{'false':getText('resource.group.Control.Behavior.Content.Order')}" value="%{inParallel}"/>
       </s:else>
    </td>
 </tr>
 <tr valign="top">
  <td width="20%" class="BlockLabel">&nbsp;</td>
  <td width="80%" class="BlockContent">
   <table width="100%" border="0" cellspacing="0" cellpadding="2">
    <tr valign="top"> 
     <td rowspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="20" height="20" border="0"/></td>
     <td><img src='<s:url value="/images/schedule_return.gif"/>' width="17" height="21" border="0"/></td>
     <td>
     
	   <s:select theme="simple" name="resourceOrdering" multiple = "true" value="%{#attr.gForm.resourceOrdering}" id="leftSel" style="WIDTH: 200px;" size="10" onchange="replaceButtons(this, 'left')" onclick="replaceButtons(this, 'left')" 
	   list="%{#attr.gForm.resourceOrderingOptions}">
       </s:select>
     </td>
     <td>&nbsp;</td>
     <td width="100%" id="leftNav">
      <div id="leftUp"><img src='<s:url value="/images/dash_movecontent_up-off.gif"/>' width="20" height="20" alt="Click to Save Changes" border="0"/></div>
			<img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" border="0"/>
			<div id="leftDown"><img src='<s:url value="/images/dash_movecontent_dn-off.gif"/>' width="20" height="20" alt="Click to Save Changes" border="0"/></div>
     </td>
    </tr>
   </table>
  </td>
 </tr>
 <tr>
  <td colspan="2" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
 </tr>
</table>
<!--  /  -->
