<!-- /fragments/header.jsp -->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<c:set var="root" value="${pageContext.request.contextPath }" />
<link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css" rel="stylesheet">
<link href="${pageContext.request.contextPath}/css/style.css?v=20261030-travel-ui" rel="stylesheet">
<header class="container legacy-header">
    <a href="${root }" class="navbar-brand" aria-label="EnjoyTrip 홈"><svg class="brand-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true"><path d="M26 13c0 7-10 15-10 15S6 20 6 13a10 10 0 1 1 20 0Z" stroke="currentColor" stroke-width="2.5"/><path d="m12 13 3 3 5-6" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>EnjoyTrip</a>
    <h1 class="visually-hidden">Welcome to SSAFY</h1>
    <nav class="legacy-nav" aria-label="회원 메뉴">
        <span> <a href="${root }">홈으로</a>
        </span> |
        <c:if test="${empty loginUser }">
            <span> <a href="${root }/member?action=login-form">로그인</a>
            </span> |
            <span> <a href="${root }/member?action=member-regist-form">회원 가입</a>
            </span> |
            <span> <a href="${root }/member?action=member-list-reset">목록 초기화</a>
            </span>
            <span> | <a href="${root }/auth?action=member-list">멤버 목록</a>
            </span>
        </c:if>
        <c:if test="${!empty loginUser }">
            <span> <a href="${root }/auth?action=member-list">멤버 목록</a>
            </span> |
            <!-- TODO: 13-01. 상세정보를 위한 링크를 확인하세요. -->
            <c:url var="loginUserDetailUrl" value="/auth">
                <c:param name="action" value="member-detail" />
                <c:param name="loginId" value="${loginUser.loginId}" />
            </c:url>
            <span> <a href="<c:out value='${loginUserDetailUrl}' />" class="mx-3">${loginUser.name }</a>
            </span> |
            <span> <a href="${root }/member?action=logout">로그아웃</a>
            </span>
        </c:if>
    </nav>
    <hr />
</header>
<script>
const showAlertMessage = (msg) => {
    if (msg && msg != "null") {
        alert(msg);
    }
}

    showAlertMessage(`${alertMsg}`)
    /*
       <c:remove var="alertMsg" scope="session"/>
     */
</script>
