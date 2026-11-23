<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>EnjoyTrip | 비밀번호 찾기</title>
</head>
<body>
    <%@ include file="/fragments/header.jsp"%>
    <div class="container">
        <h1>비밀번호 찾기</h1>
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}" /></div>
        </c:if>
        <c:choose>
            <c:when test="${resetAllowed}">
                <p>답변을 확인했습니다. 5분 안에 새 비밀번호를 설정해주세요.</p>
                <form action="${root}/member" method="post" class="m-3">
                    <input type="hidden" name="action" value="password-reset" />
                    <label for="pass" class="form-label">새 비밀번호</label>
                    <input type="password" name="pass" id="pass" class="form-control mb-3"
                        required minlength="4" maxlength="128" autocomplete="new-password" />
                    <label for="passConfirm" class="form-label">새 비밀번호 확인</label>
                    <input type="password" name="passConfirm" id="passConfirm" class="form-control mb-3"
                        required minlength="4" maxlength="128" autocomplete="new-password" />
                    <button type="submit" class="btn btn-primary">비밀번호 변경</button>
                </form>
            </c:when>
            <c:when test="${not empty recoveryQuestion}">
                <p>아이디: <c:out value="${recoveryLoginId}" /></p>
                <p>복구 질문: <c:out value="${recoveryQuestion}" /></p>
                <form action="${root}/member" method="post" class="m-3">
                    <input type="hidden" name="action" value="recovery-verify" />
                    <input type="hidden" name="loginId" value="<c:out value='${recoveryLoginId}' />" />
                    <label for="answer" class="form-label">복구 답변</label>
                    <input type="text" name="answer" id="answer" class="form-control mb-3" required maxlength="100" autocomplete="off" />
                    <button type="submit" class="btn btn-primary">답변 확인</button>
                </form>
            </c:when>
            <c:otherwise>
                <form action="${root}/member" method="post" class="m-3">
                    <input type="hidden" name="action" value="recovery-question" />
                    <label for="loginId" class="form-label">아이디</label>
                    <input type="text" name="loginId" id="loginId" class="form-control mb-3" required maxlength="20" autocomplete="username" />
                    <button type="submit" class="btn btn-primary">복구 질문 확인</button>
                </form>
            </c:otherwise>
        </c:choose>
        <a href="${root}/member?action=recovery-form">아이디부터 다시 확인</a>
        <a href="${root}/member?action=login-form" class="ms-3">로그인으로 돌아가기</a>
    </div>
    <%@ include file="/fragments/footer.jsp"%>
</body>
</html>
