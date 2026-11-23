<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>EnjoyTrip | 회원가입</title>
</head>
<body>
    <%@include file="/fragments/header.jsp"%>
    <div class="container">
        <h1>회원가입</h1>
        <form action="${root }/member" method="post" class="m-3">
            <input type="hidden" name="action" value="member-regist" />

            <div class="mb-3 row">
                <label for="name" class="col-sm-2 col-form-label">이름</label>
                <div class="col-sm-10">
                    <input type="text" name="name" id="name" class="form-control"
                        required maxlength="40" autocomplete="name" />
                </div>
            </div>

            <div class="mb-3 row">
                <label for="loginId" class="col-sm-2 col-form-label">아이디</label>
                <div class="col-sm-10">
                    <input type="text" name="loginId" id="loginId" class="form-control"
                        required pattern="[a-z0-9_]{4,20}" maxlength="20" autocomplete="username" />
                    <div class="form-text">소문자 영문·숫자·밑줄 4~20자</div>
                    <!-- TODO: 04. 오류 정보를 표시하기 위한 div를 확인해보자. -->
                    <div class="invalid-feedback">이미 사용 중인 아이디입니다.</div>
                </div>
            </div>

            <div class="mb-3 row">
                <label for="pass" class="col-sm-2 col-form-label">비밀번호</label>
                <div class="col-sm-10">
                    <input type="password" name="pass" id="pass" class="form-control"
                        required minlength="4" maxlength="128" autocomplete="new-password" />
                </div>
            </div>

            <div class="mb-3 row">
                <label for="passConfirm" class="col-sm-2 col-form-label">비밀번호 확인</label>
                <div class="col-sm-10">
                    <input type="password" name="passConfirm" id="passConfirm" class="form-control"
                        required minlength="4" maxlength="128" autocomplete="new-password" />
                </div>
            </div>
            <div class="mb-3 row">
                <label for="question" class="col-sm-2 col-form-label">복구 질문</label>
                <div class="col-sm-10">
                    <input type="text" name="question" id="question" class="form-control" required maxlength="100" />
                </div>
            </div>
            <div class="mb-3 row">
                <label for="answer" class="col-sm-2 col-form-label">복구 답변</label>
                <div class="col-sm-10">
                    <input type="text" name="answer" id="answer" class="form-control" required maxlength="100" autocomplete="off" />
                </div>
            </div>
            <button type="submit" class="btn btn-primary">등록</button>
        </form>
        <%
        Object error = request.getAttribute("error");
        if (error != null) {
            out.println("<div class='alert alert-danger' role='alert'>" + error + "</div>");
        }
        %>

        <%@include file="/fragments/footer.jsp"%>
    </div>
</body>
<!-- TODO: 05. #email에서 input event 발생 시 중복 여부를 확인(/member?action=email-check)하세요.-->
<!-- 현재 회원 기준: #loginId 및 /member?action=login-id-check 사용 -->
<!--  상황에 따라 is-valid와 is-invalid를 추가/삭제한다. -->
<!--  참조: https://getbootstrap.com/docs/5.3/forms/validation/#server-side -->
<script>
const loginIdInput = document.querySelector("#loginId");
const loginIdFeedback = loginIdInput.parentElement.querySelector(".invalid-feedback");
let loginIdCheckId = 0;

loginIdInput.addEventListener("input", async () => {
    const checkId = ++loginIdCheckId;
    loginIdInput.classList.remove("is-valid", "is-invalid");
    loginIdInput.setCustomValidity("");
    if (!loginIdInput.value || !loginIdInput.validity.valid) {
        return;
    }

    // 응답을 기다리는 동안 중복 확인이 끝나지 않은 주소로 제출하지 않는다.
    loginIdInput.setCustomValidity("아이디 중복 확인 중입니다.");
    const url = new URL("${root}/member", window.location.origin);
    url.searchParams.set("action", "login-id-check");
    url.searchParams.set("loginId", loginIdInput.value);

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error("아이디 중복 확인 실패");
        }
        const result = await response.json();
        // 빠르게 연속 입력한 경우 이전 입력에 대한 응답은 반영하지 않는다.
        if (checkId !== loginIdCheckId) {
            return;
        }
        const canUse = result.canUse === true;
        loginIdInput.classList.toggle("is-valid", canUse);
        loginIdInput.classList.toggle("is-invalid", !canUse);
        loginIdFeedback.textContent = "이미 사용 중인 아이디입니다.";
        loginIdInput.setCustomValidity(canUse ? "" : "이미 사용 중인 아이디입니다.");
    } catch (error) {
        if (checkId !== loginIdCheckId) {
            return;
        }
        loginIdInput.classList.add("is-invalid");
        loginIdFeedback.textContent = "중복 확인에 실패했습니다. 아이디를 다시 입력해주세요.";
        loginIdInput.setCustomValidity(loginIdFeedback.textContent);
    }
});
</script>

<!-- END -->
</html>
