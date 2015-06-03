<%@ page language="java" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<tiles:importAttribute name="typeName"/>
<tiles:importAttribute name="aetid"/>

<td class="ListCell" align="center">
	<s:a href="/resource/%{#attr.typeName}/monitor/Config.do?mode=configure&aetid=%{#attr.aetid}">
		<s:param name="mode" >configure</s:param>
		<s:param name="aetid" value="%{#attr.aetid}"/>
		<img src='<s:url value="/images/tbb_editMetricTemplate.gif"/>' width="136" height="16" border="0"/>
	</s:a>
</td>