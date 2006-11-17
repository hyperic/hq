<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>

<tiles:importAttribute name="displayLastCompleted"/>
<tiles:importAttribute name="lastCompleted" ignore="true"/>

<tiles:importAttribute name="displayMostFrequent"/>
<tiles:importAttribute name="nextScheduled" ignore="true"/>

<tiles:importAttribute name="displayNextScheduled"/>
<tiles:importAttribute name="mostFrequent" ignore="true"/>
            
<div class="effectsPortlet">
<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.Control"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

<!-- each sub-section can be hidden or visible.  They can't be re-ordered. -->

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="DashboardControlActionsContainer">
  <tr>
    <td>
      <c:if test="${displayLastCompleted}">  
        <!-- Recent Actions Contents -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td class="Subhead"><fmt:message key="dash.home.Subhead.Recent"/></td>
          </tr>
        </table>
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <c:choose>    
            <c:when test="${empty lastCompleted}">
              <tr class="ListRow">
                <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
            </c:when>
            <c:otherwise>     
              <tr>
                <td width="37%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
                <td width="21%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Type"/></td>
                <td width="21%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ControlAction"/></td>
                <td width="21%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.DateTime"/><html:img page="/images/tb_sortdown.gif" width="9" height="9" border="0"/></td>
              </tr>  
              <c:forEach items="${lastCompleted}" var="resource">
                <tr class="ListRow">                                                   
                  <td class="ListCell"><html:link page="/ResourceControlHistory.do?eid=${resource.entityType}:${resource.entityId}"><c:out value="${resource.entityName}"/></html:link></td>
                  <td class="ListCell"><hq:resourceTypeName typeId="${resource.entityType}"/></td>
                  <td class="ListCell"><c:out value="${resource.action}"/></td>
                  <td class="ListCell"><hq:dateFormatter value="${resource.startTime}"/></td>
                </tr>    
              </c:forEach>
            </c:otherwise>
          </c:choose>
        </table>
      </c:if>
    </td>
  </tr>
  <tr>
    <td>
      <c:if test="${displayNextScheduled}">
        <!-- Pending Actions Content Here -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
          <tr>
            <td class="Subhead"><fmt:message key="dash.home.Subhead.Pending"/></td>
          </tr>
        </table>    
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <c:choose>    
            <c:when test="${empty nextScheduled}">
              <tr class="ListRow">
                <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
            </c:when>
            <c:otherwise>              
                <tr>
                  <td width="37%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.ResourceName"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
                  <td width="21%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Type"/></td>
                  <td width="21%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ControlAction"/></td>
                  <td width="21%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.DateTime"/></td>
                </tr>                
                <c:forEach items="${nextScheduled}" var="pending">
                  <c:choose>
                    <c:when test="${pending.control.entityType == 3}">
                      <c:set var="typename" value="service"/>
                    </c:when>
                    <c:when test="${pending.control.entityType == 5}">
                      <c:set var="typename" value="group"/>
                    </c:when>
                    <c:otherwise>
                      <c:set var="typename" value="server"/>
                    </c:otherwise>
                  </c:choose>
                  <tr class="ListRow">                                        
                    <td class="ListCell"><html:link page="/resource/${typename}/Control.do?mode=view&eid=${pending.control.entityType}:${pending.control.entityId}"><c:out value="${pending.resource.name}"/></html:link></td>
                    <td class="ListCell"><hq:resourceTypeName typeId="${pending.control.entityType}"/></td>
                    <td class="ListCell"><c:out value="${pending.control.action}"/></td>
                    <td class="ListCell"><hq:dateFormatter value="${pending.control.nextFireTime}"/></td>
                  </tr>    
                </c:forEach>              
            </c:otherwise>
          </c:choose>  
        </table>
      </c:if>
    </td>
  </tr>
  <tr>
    <td>
      <c:if test="${displayMostFrequent}">
        <!-- On-Demand Control Frequency Contents -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
          <tr>
            <td class="Subhead"><fmt:message key="dash.home.Subhead.Quick"/></td>
          </tr>
        </table>  
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <c:choose>
            <c:when test="${empty mostFrequent}">
              <tr class="ListRow">
                <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
            </c:when>
            <c:otherwise>
              <tr class="ListRow">
                <td>
                  <table width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td width="37%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
                      <td width="21%" class="ListHeaderInactiveSorted" align="center"><fmt:message key="dash.home.TableHeader.ControlActions"/><html:img page="/images/tb_sortdown.gif" width="9" height="9" border="0"/></td>
                      <td width="42%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.FrequentActions"/></td>
                    </tr>
                    <c:forEach items="${mostFrequent}" var="resource">
                      <tr class="ListRow">
                        <td class="ListCell"><html:link page="/ResourceControlHistory.do?eid=${resource.type}:${resource.id}"><c:out value="${resource.name}"/></html:link></td>
                        <td class="ListCell" align="center"><c:out value="${resource.num}"/></td>
                        <td class="ListCell"><c:out value="${resource.action}"/></td>
                      </tr>
                    </c:forEach>              
                    <tiles:insert definition=".dashContent.seeAll"/>
                  </table>
                </td>
              </tr>
            </c:otherwise>
          </c:choose>
        </table>
      </c:if>
    </td> 
  </tr>
</table>
</div>
