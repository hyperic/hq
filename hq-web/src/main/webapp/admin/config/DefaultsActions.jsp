<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>

<tiles:importAttribute name="typeName"/>
<tiles:importAttribute name="aetid"/>

<td class="ListCell" align="center">
	<html:link action="/resource/${typeName}/monitor/Config">
		<html:param name="mode" value="configure"/>
		<html:param name="aetid" value="${aetid}"/>
		<html:img page="/images/tbb_editMetricTemplate.gif" width="136" height="16" border="0"/>
	</html:link>
</td>