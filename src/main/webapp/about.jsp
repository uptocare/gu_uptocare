<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="關於醫師" scope="request"/>
<c:set var="activeNav" value="about" scope="request"/>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-4">
  <div class="row g-4">
    <div class="col-md-4 text-center">
      <img src="${ctx}/images/doctor.svg" class="img-fluid rounded-circle shadow doctor-photo mb-3" alt="鄧豪偉 醫師">
      <h1 class="h4">鄧豪偉 醫師</h1>
      <p class="text-body-secondary">HaoWei Teng, MD</p>
    </div>
    <div class="col-md-8">
      <%-- 以下為範例內容，請依實際資料修改 --%>
      <h2 class="h5">專長</h2>
      <ul>
        <li>泌尿道腫瘤（攝護腺癌、膀胱癌、腎臟癌…）藥物治療</li>
        <li>免疫治療、標靶治療、化學治療</li>
        <li>基因檢測與精準醫療</li>
      </ul>

      <h2 class="h5 mt-4">學經歷</h2>
      <ul>
        <li>（請填寫）</li>
      </ul>

      <h2 class="h5 mt-4">門診時間</h2>
      <div class="table-responsive">
        <table class="table table-bordered text-center align-middle">
          <thead class="table-light">
            <tr><th></th><th>一</th><th>二</th><th>三</th><th>四</th><th>五</th></tr>
          </thead>
          <tbody>
            <tr><th>上午</th><td></td><td>●</td><td></td><td></td><td></td></tr>
            <tr><th>下午</th><td></td><td></td><td></td><td>●</td><td></td></tr>
          </tbody>
        </table>
      </div>
      <p class="small text-body-secondary">門診時間如有異動，以醫院掛號系統及本站「重要訊息」為準。</p>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
