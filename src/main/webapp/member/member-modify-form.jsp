<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>EnjoyTrip | 회원 정보 수정</title>
</head>
<body>
    <%@ include file="/fragments/header.jsp"%>
    <div class="container">
        <h1>회원수정</h1>
        <form action="${root}/auth" method="post" class="m-3">
            <input type="hidden" name="action" value="member-modify" />
            <input type="hidden" name="mno" value="${member.mno }" />

            <div class="mb-3 row">
                <label for="name" class="col-sm-2 col-form-label">이름</label>
                <div class="col-sm-10">
                    <input type="text" name="name" id="name" class="form-control" required maxlength="40"
                        value="<c:out value='${member.name}' />" />
                </div>
            </div>

            <div class="mb-3 row">
                <label for="loginId" class="col-sm-2 col-form-label">아이디</label>
                <div class="col-sm-10">
                    <input type="text" name="loginId" id="loginId" class="form-control" readonly value="${member.loginId }" />
                </div>
            </div>

            <div class="mb-3 row">
                <label for="currentPassword" class="col-sm-2 col-form-label">현재 비밀번호</label>
                <div class="col-sm-10">
                    <input type="password" name="currentPassword" id="currentPassword" class="form-control" required autocomplete="current-password" />
                    <div class="form-text">관리자가 다른 회원을 수정할 때는 관리자 본인의 비밀번호를 입력합니다.</div>
                </div>
            </div>
            <div class="mb-3 row">
                <label for="pass" class="col-sm-2 col-form-label">새 비밀번호</label>
                <div class="col-sm-10">
                    <input type="password" name="pass" id="pass" class="form-control"
                        minlength="4" maxlength="128" autocomplete="new-password" />
                    <div class="form-text">비워두면 기존 비밀번호를 유지합니다.</div>
                </div>
            </div>
            <div class="mb-3 row">
                <label for="passConfirm" class="col-sm-2 col-form-label">새 비밀번호 확인</label>
                <div class="col-sm-10">
                    <input type="password" name="passConfirm" id="passConfirm" class="form-control" maxlength="128" autocomplete="new-password" />
                </div>
            </div>
            <div class="mb-3 row">
                <label for="question" class="col-sm-2 col-form-label">복구 질문</label>
                <div class="col-sm-10">
                    <input type="text" name="question" id="question" class="form-control" required maxlength="100"
                        value="<c:out value='${member.question}' />" />
                </div>
            </div>
            <div class="mb-3 row">
                <label for="answer" class="col-sm-2 col-form-label">새 복구 답변</label>
                <div class="col-sm-10">
                    <input type="text" name="answer" id="answer" class="form-control" maxlength="100" autocomplete="off" />
                    <div class="form-text">비워두면 기존 답변을 유지합니다. 질문을 변경할 때는 새 답변을 입력해주세요.</div>
                </div>
            </div>

            <div class="mb-3 row">
                <label for="role" class="col-sm-2 col-form-label">권한</label>
                <div class="col-sm-10">
                    <input type="text" name="role" id="role" class="form-control" value="${member.role }"
                        ${sessionScope.loginUser.role eq 'ADMIN' ? '' : 'readonly'} />
                </div>
            </div>

            <button type="submit" class="btn btn-primary">수정</button>
        </form>

        <c:if test="${!empty error }">
            <div class="alert alert-danger" role="alert"><c:out value="${error}" /></div>
        </c:if>
    </div>
    <%@ include file="/fragments/footer.jsp"%>
</body>
</html>
