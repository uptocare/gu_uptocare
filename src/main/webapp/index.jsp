<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.uptocare.gu.data.PostRepository, com.uptocare.gu.model.Post" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    request.setAttribute("newsList", PostRepository.latest(Post.Type.NEWS, 5));
    request.setAttribute("shareList", PostRepository.latest(Post.Type.SHARE, 3));
%>
<c:set var="activeNav" value="home" scope="request"/>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="hero py-5">
  <div class="container">
    <div class="row align-items-center g-4">
      <div class="col-md-4 text-center">
        <img src="${ctx}/images/doctor.svg" class="img-fluid rounded-circle shadow doctor-photo" alt="鄧豪偉 醫師">
      </div>
      <div class="col-md-8">
        <h1 class="display-6 fw-bold">楊牧穎 醫師</h1>
        <p class="lead text-body-secondary">泌尿外科醫師 · 泌尿道腫瘤照護</p>
        <p>這裡分享泌尿道腫瘤的治療新知、門診重要訊息，以及臨床上的經驗與省思，希望陪伴病友與家屬走得更安心。</p>
        <a href="${ctx}/about.jsp" class="btn btn-primary">認識醫師</a>
        <a href="${ctx}/posts.jsp?type=NEWS" class="btn btn-outline-primary ms-2">最新訊息</a>
      </div>
    </div>
  </div>
</section>

<div class="container">
  <div class="row g-4">
    <div class="col-lg-7">
      <h2 class="h4 border-start border-4 border-danger ps-2 mb-3">重要訊息</h2>
      <div class="list-group">
        <c:forEach var="p" items="${newsList}">
          <a href="${ctx}/post.jsp?id=${p.id}" class="list-group-item list-group-item-action">
            <div class="d-flex justify-content-between">
              <span>
                <c:if test="${p.pinned}"><span class="badge text-bg-danger me-1">置頂</span></c:if>
                <c:out value="${p.title}"/>
              </span>
              <small class="text-body-secondary text-nowrap ms-2">${p.dateText}</small>
            </div>
          </a>
        </c:forEach>
      </div>
      <a href="${ctx}/posts.jsp?type=NEWS" class="d-inline-block mt-2">看全部訊息 &rarr;</a>
    </div>

    <div class="col-lg-5">
      <h2 class="h4 border-start border-4 border-primary ps-2 mb-3">經驗分享</h2>
      <c:forEach var="p" items="${shareList}">
        <div class="card mb-3">
          <div class="card-body">
            <h3 class="h6 card-title"><a href="${ctx}/post.jsp?id=${p.id}" class="stretched-link text-decoration-none"><c:out value="${p.title}"/></a></h3>
            <p class="card-text small text-body-secondary mb-0"><c:out value="${p.summary}"/></p>
          </div>
        </div>
      </c:forEach>
      <a href="${ctx}/posts.jsp?type=SHARE">看全部分享 &rarr;</a>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
