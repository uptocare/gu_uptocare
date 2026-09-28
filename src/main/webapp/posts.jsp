<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.uptocare.gu.data.PostRepository, com.uptocare.gu.model.Post" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    Post.Type type = "SHARE".equals(request.getParameter("type")) ? Post.Type.SHARE : Post.Type.NEWS;
    request.setAttribute("posts", PostRepository.findByType(type));
    request.setAttribute("pageTitle", type == Post.Type.NEWS ? "重要訊息" : "經驗分享");
    request.setAttribute("activeNav", type == Post.Type.NEWS ? "news" : "share");
%>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-4">
  <h1 class="h3 mb-4"><c:out value="${pageTitle}"/></h1>
  <c:choose>
    <c:when test="${empty posts}">
      <p class="text-body-secondary">目前尚無文章。</p>
    </c:when>
    <c:otherwise>
      <div class="row row-cols-1 row-cols-md-2 g-3">
        <c:forEach var="p" items="${posts}">
          <div class="col">
            <div class="card h-100">
              <div class="card-body">
                <small class="text-body-secondary">${p.dateText}</small>
                <c:if test="${p.pinned}"><span class="badge text-bg-danger ms-1">置頂</span></c:if>
                <h2 class="h5 card-title mt-1"><a href="${ctx}/post.jsp?id=${p.id}" class="stretched-link text-decoration-none"><c:out value="${p.title}"/></a></h2>
                <p class="card-text text-body-secondary"><c:out value="${p.summary}"/></p>
              </div>
            </div>
          </div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</div>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
