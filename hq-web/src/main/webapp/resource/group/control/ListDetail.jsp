<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%-- Tile for displaying a detailed status list for a group control action,
     whether current or history.
    
    @param returnUrl The url displayed at the bottom and top of the table
    @param returnKey The i18n key for the link text
--%>
<tiles:importAttribute name="returnUrl"/>
<tiles:importAttribute name="returnKey"/>
<tiles:importAttribute name="noButtons" ignore="true"/>

<c:set var="section" value="group"/>

<c:set var="selfAction" value="/resource/${section}/Control.do?mode=${param.mode}&rid=${Resource.id}&type=${Resource.entityId.type}&bid=${param.bid}"/>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<%-- now add the context path --%>
<c:url var="selfActionUrl" value="${selfAction}"/>

<c:set var="listSize" value="${request.listSize}"/>
<c:set var="page" value="${hstDetailAttr}"/>
<c:set var="widgetInstanceName" value="controlAction"/>
<c:url var="fullReturnUrl" value="${returnUrl}">
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${Resource.entityId.type}"/>
</c:url>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
  var pageData = new Array();
  initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
  widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<tiles:insert definition=".page.title.resource.group">
  <tiles:put name="eid" beanName="entityId" beanProperty="appdefKey" />
</tiles:insert>
            
<tiles:insert definition=".page.return">
 <tiles:put name="returnUrl" beanName="fullReturnUrl"/>
 <tiles:put name="returnKey" beanName="returnKey"/>
</tiles:insert>

<!-- HISTORY FORM -->
<tiles:insert definition=".resource.common.control.currentStatus">
 <tiles:put name="tabKey" value="resource.group.ControlDetail.Tab"/>
 <tiles:put name="isDetail" value="true"/>
</tiles:insert>

<br/>

<html:form action="/resource/group/control/RemoveHistory">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>

<c:forEach items="${hstDetailAttr}" var="tmpDetail" end="1">
 <c:choose>
  <c:when test="${tmpDetail.entityType eq '2'}">
   <c:set var="resourceType" value="server"/>       
  </c:when>
  <c:when test="${tmpDetail.entityType eq '3'}">
   <c:set var="resourceType" value="service"/>    
  </c:when>
  <c:when test="${tmpDetail.entityType eq '5'}">
   <c:set var="resourceType" value="group"/>       
  </c:when>
 </c:choose>
</c:forEach>

<display:table cellspacing="0" cellpadding="0" width="100%" action="${selfActionUrl}"
               orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}" pageValue="pn" 
               page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
               items="${hstDetailAttr}" var="hstDetail"> 
  <display:column width="16%" property="entityName" sort="true" sortAttr="9"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Resource" 
                  href="/resource/${resourceType}/Control.do?mode=history&eid=${hstDetail.entityType}:${hstDetail.entityId}" />
  <display:column width="10%" property="status" sort="true" sortAttr="10"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Status" /> 
   <display:column width="14%" property="args" title="resource.server.ControlHistory.ListHeader.Arguments">
   </display:column>
  <display:column width="15%" property="startTime" sort="true" sortAttr="11"
                  defaultSort="true" title="resource.group.ControlDetail.ListHeader.Started">
   <display:datedecorator/>
  </display:column>
  <display:column width="10%" property="duration" sort="true" sortAttr="12"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Elapsed"> 
   <display:datedecorator isElapsedTime="true"/>
  </display:column> 
  <display:column width="35%" property="message"
                  defaultSort="false" title="common.header.Description"/>
</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listItems" beanName="page"/>
  <tiles:put name="noButtons" value="true"/>
  <tiles:put name="listSize" beanName="hstDetailAttr" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction" />
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="11"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
</tiles:insert>
<!-- /  -->

<c:if test="${empty noButtons}">
 <%-- delete the current control action --%>
 <html:hidden property="controlActions" value="${param.bid}"/>
 <tiles:insert definition=".form.buttons.deleteCancel"/>
</c:if>
</html:form>

<tiles:insert definition=".page.return">
 <tiles:put name="returnUrl" beanName="fullReturnUrl"/>
 <tiles:put name="returnKey" beanName="returnKey"/>
</tiles:insert>
            
