<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.uptocare.gu.data.PostRepository, com.uptocare.gu.model.Post, com.uptocare.gu.gdoc.GoogleDocService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    Post post = null;
    try {
        post = PostRepository.findById(Integer.parseInt(request.getParameter("id")));
    } catch (NumberFormatException e) {
        // 無效 id，當作找不到
    }
    if (post == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    if (post.getDocUrl() != null) {
        try {
            request.setAttribute("docHtml", GoogleDocService.render(post.getDocUrl()));
        } catch (Exception e) {
            log("讀取 Google 文件失敗 id=" + post.getId(), e);
            request.setAttribute("docError", e.getMessage());
        }
    }
    request.setAttribute("post", post);
    request.setAttribute("pageTitle", post.getTitle());
    request.setAttribute("activeNav", post.getType() == Post.Type.NEWS ? "news" : "share");
%>
<%@ include file="/WEB-INF/jspf/header.jspf" %>
<c:set var="typeName" value="${post.type == 'NEWS' ? '重要訊息' : '經驗分享'}"/>

<header class="article-hero ${post.type == 'NEWS' ? 'is-news' : 'is-share'}">
  <div class="container article-width">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb small mb-3">
        <li class="breadcrumb-item"><a href="${ctx}/">首頁</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/posts.jsp?type=${post.type}">${typeName}</a></li>
      </ol>
    </nav>
    <span class="badge rounded-pill article-badge">${typeName}</span>
    <h1 class="article-title mt-2"><c:out value="${post.title}"/></h1>
    <c:if test="${not empty post.summary}">
      <p class="article-summary"><c:out value="${post.summary}"/></p>
    </c:if>
    <div class="article-meta">
      <i class="bi bi-person-circle"></i> 鄧豪偉 醫師
      <span class="mx-2">·</span>
      <i class="bi bi-calendar3"></i> ${post.dateText}
    </div>
  </div>
</header>

<div class="container article-width py-4">
  <div class="row">
    <div class="col-12">
      <c:choose>
        <c:when test="${not empty docHtml}">
          <nav id="toc" class="article-toc d-none" aria-label="本文目錄">
            <div class="fw-bold mb-2"><i class="bi bi-list-ul"></i> 本文目錄</div>
            <ol></ol>
          </nav>
          <%-- docHtml 已由 GoogleDocService 過濾，只保留安全的標籤 --%>
          <article class="doc-content">${docHtml}</article>
        </c:when>
        <c:when test="${not empty docError}">
          <div class="alert alert-warning">
            <i class="bi bi-exclamation-triangle"></i> 文章內容暫時無法載入，請稍後再試。
          </div>
        </c:when>
        <c:otherwise>
          <p class="text-body-secondary">內容準備中。</p>
        </c:otherwise>
      </c:choose>

      <hr class="my-5">
      <div class="d-flex justify-content-between align-items-center">
        <a href="${ctx}/posts.jsp?type=${post.type}" class="btn btn-outline-secondary">
          <i class="bi bi-arrow-left"></i> 回${typeName}
        </a>
        <button type="button" class="btn btn-link text-body-secondary" onclick="window.print()">
          <i class="bi bi-printer"></i> 列印
        </button>
      </div>
    </div>
  </div>
</div>

<script>
  // 文章有 3 個以上小標題時，自動產生目錄
  (function () {
    var heads = document.querySelectorAll('.doc-content h2, .doc-content h3');
    var toc = document.getElementById('toc');
    if (!toc || heads.length < 3) return;
    var list = toc.querySelector('ol'), lastTop = null;
    heads.forEach(function (h, i) {
      if (!h.id) h.id = 'sec-' + (i + 1);
      var li = document.createElement('li');
      var a = document.createElement('a');
      a.href = '#' + h.id;
      a.textContent = h.textContent;
      li.appendChild(a);
      if (h.tagName === 'H3' && lastTop) {
        var sub = lastTop.querySelector('ul') || lastTop.appendChild(document.createElement('ul'));
        sub.appendChild(li);
      } else {
        list.appendChild(li);
        lastTop = li;
      }
    });
    toc.classList.remove('d-none');
  })();
</script>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
