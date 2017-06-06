<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:form action="alertOpenNMSFormAction">
	<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  		<s:if test="hasErrors()">
  			<tr>
    			<td colspan="4" align="left" class="ErrorField"><s:actionerror /></td>
  			</tr>
  		</s:if>
  		<tr>
    		<td class="BlockLabel"><fmt:message key="alert.config.edit.opennms.server"/></td>
    		<td class="BlockContent">
    		<c:if test="${OpenNMSForm.canModify}"  >
	    		<s:textfield name="server" value="%{#attr.OpenNMSForm.server}"/>
	    	</c:if>
	    	<c:if test="${not OpenNMSForm.canModify}"  >
	    		<s:textfield name="server" value="%{#attr.OpenNMSForm.server}" disabled="true" />
	    	</c:if>
    		</td>
    		<td class="BlockLabel"><fmt:message key="alert.config.edit.opennms.port"/></td>
    		<td class="BlockContent">
    			<c:if test="${OpenNMSForm.canModify}"  >
	    			<s:textfield name="port" value="%{#attr.OpenNMSForm.port}"/>
	    		</c:if>
				<c:if test="${not OpenNMSForm.canModify}"  >
	    			<s:textfield name="port" value="%{#attr.OpenNMSForm.port}" disabled="true"/>
	    		</c:if>
    		</td>
  		</tr>
	</table>
	<s:hidden theme="simple" value="%{#attr.ad}" name="ad"/>
	<s:hidden theme="simple" value="%{#attr.OpenNMSForm.id}" name="id"/>
	<s:hidden theme="simple" value="%{#attr.aetid}" name="aetid"/>
	<s:hidden theme="simple" value="%{#attr.eid}" name="eid"/>
	<c:if test="${OpenNMSForm.canModify}"  >
		<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent" style="border-bottom: 1px solid #D5D8DE;">
  			<tr>
  				<td align="center">
  					<input type="image" src='<s:url value="/images/tbb_set.gif"/>' border="0" titleKey="FormButtons.ClickToOk" name="ok" id="ok" style="padding-right: 5px;"/>
    				<input type="image" src='<s:url value="/images/tbb_remove.gif"/>' border="0" titleKey="FormButtons.ClickToDelete" name="delete" id="delete" style="padding-left: 5px;"/>
    			</td>
    		</tr>
		</table>
	</c:if>
</s:form>
