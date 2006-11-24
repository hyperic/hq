<?xml version="1.0" encoding="ISO-8859-1"?>
<%@ page language="java" contentType="text/xml" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
<% response.setHeader("Pragma","no-cache");%>
<% response.setHeader("Cache-Control","no-store");%>
<% response.setDateHeader("Expires",-1);%>
<ajax-response>
  <response type="object" id="controlStatusUpdater"><controlStatus ctrlAction="<c:out value="${controlCurrentStatus.action}"/>" ctrlDesc="<c:out value="${controlCurrentStatus.description}"/>" ctrlStatus="<c:out value="${controlCurrentStatus.status}"/>" ctrlStart="<hq:dateFormatter value="${controlCurrentStatus.startTime}"/>" ctrlMessage="<c:out value="${controlCurrentStatus.message}"/>" ctrlSched="<hq:dateFormatter value="${controlCurrentStatus.dateScheduled}"/>" ctrlDuration="<hq:dateFormatter time="true" value="${controlCurrentStatus.duration}"/>"/></response>
</ajax-response>
