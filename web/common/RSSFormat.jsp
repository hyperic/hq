<?xml version="1.0" encoding="utf-8" ?>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
<%@ page language="java" contentType="text/xml" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
   <channel>
      <title><c:out value="${rssFeed.title}"/></title>
      <link><c:out value="${rssFeed.baseUrl}"/></link>
      <description><fmt:message key="dashboard.template.title"/> <c:out value="${rssFeed.title}"/></description>
      <language>en-us</language>
      <pubDate><c:out value="${rssFeed.pubDate}"/></pubDate>

      <lastBuildDate><c:out value="${rssFeed.buildDate}"/></lastBuildDate>
      <docs><fmt:message key="common.url.help"/></docs>
      <generator><fmt:message key="about.Title"/></generator>
      <managingEditor><c:out value="${managingEditor}"/></managingEditor>
      <webMaster><fmt:message key="about.MoreInfo.LinkSupport"/></webMaster>
    <c:forEach items="${rssFeed.items}" var="item">
      <item>
         <title><c:out value="${item.title}"/></title>
         <link><![CDATA[<c:out value="${item.link}" escapeXml="false"/>]]></link>
         <description><![CDATA[<c:out value="${item.description}" escapeXml="false"/>]]></description>
         <pubDate><c:out value="${item.pubDate}"/></pubDate>
         <guid><![CDATA[<c:out value="${item.guid}"/>]]></guid>
      </item>
    </c:forEach>
   </channel>
</rss>
