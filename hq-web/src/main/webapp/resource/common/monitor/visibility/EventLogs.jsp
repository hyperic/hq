<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<c:set var="count" value="0"/>
<c:forEach var="timeTick" items="${timeIntervals}">
  <c:set var="eventsExist" value="${timeTick.eventsExist || eventsExist}"/>
</c:forEach>

<c:if test="${eventsExist}">
<tr>
  <td>
  	<jsu:importScript path="/js/timeline/api-2.0/timeline-api.js" />
  	<jsu:importScript path="/js/effects.js" />
	<jsu:script>
		var eventSource = new Timeline.DefaultEventSource();
		
		function timeLineOnLoad() {
		  var timelineDiv = document.getElementById("my-timeline");
		  var evt, dateEvent;
		  <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
		      <c:set var="count" value="${status.count - 1}"/>
		      <c:if test="${timeTick.eventsExist}">
		        dateEvent = new Date();
		        dateEvent.setUTCDate(1);
		        dateEvent.setUTCMonth(0);
		        dateEvent.setUTCFullYear(2008);
		        dateEvent.setUTCFullYear(2008);
		        dateEvent.setUTCHours(0);
		        dateEvent.setUTCMinutes(<c:out value="${count}"/>);
		        evt = new Timeline.DefaultEventSource.Event(
		             "<c:out value="${timeTick.time}"/>",
		             dateEvent,
		             null,
		             null,
		             null,
		             false,
		             "<fmt:message key="resource.common.monitor.label.elc"/>",
		             "Events(<c:out value="${count}"/>) details not yet loaded..."
		            );
		        eventSource.add(evt);
		      </c:if>
		  </c:forEach>
		
		  var bandInfos = [
		    Timeline.createBandInfo({
		        showEventText:  false,
		        eventSource:    eventSource,
		        width:          "100%",
		        trackHeight:    0.2,
		        intervalUnit:   Timeline.DateTime.HOUR, 
		        intervalPixels: timelineDiv.offsetWidth - 6,
		        date:           "Jan 1 2008 00:30:00 GMT"
		    })
		  ];
		  bandInfos[0].highlight = true;
		
		  tl = Timeline.create(timelineDiv, bandInfos);
		
		  setTimeout("fillEventLogs()", 5000);
		 }
		
		  function fillEventLogs() {
		  <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
		      <c:if test="${timeTick.eventsExist}">
		        <c:url var="ajaxUrl" value="/resource/common/monitor/visibility/EventDetails.do">
		          <c:param name="eid" value="${eid}"/>
		          <c:param name="begin" value="${timeTick.time}"/>
		        </c:url>
		        new Ajax.Request('<c:out value="${ajaxUrl}" escapeXml="false"/>',
		                         {method: 'get', onSuccess:showEventResponse});
		      </c:if>
		  </c:forEach>
		  }
		
		  function showEventResponse(originalRequest) {
		      var eventText = eval("(" + originalRequest.responseText + ")");
		      var eventId = eventText.id;
		      var evt = eventSource.getEvent(eventId);
		      var eventHtml = eventText.html;
		      if (eventHtml.length > 850) {
		        eventHtml = "<div class=\"bigEventDetails\">" + eventHtml +
		                   "</div>";
		      }
		      evt._description = eventHtml;
		  }
	</jsu:script>
	<jsu:script onLoad="true">
		timeLineOnLoad();
	</jsu:script>
  </td>
</tr>
<tr>
  <td colspan="<c:out value="${count + 2}"/>" valign="top">

      <div id="my-timeline" style="height: 20px; border: 1px solid #aaa"></div>
  </td>
  <td style="padding-left: 4px;"><fmt:message key="resource.common.monitor.label.elc"/></td>
</tr>

</c:if>
