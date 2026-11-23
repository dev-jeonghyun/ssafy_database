<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>EnjoyTrip | 로그인</title>
</head>
<body>
    <%@ include file="/fragments/header.jsp"%>
    <div class="container">
        <h1>로그인</h1>
        <form action="${root }/member" method="post" class="m-3">
            <input type="hidden" name="action" value="login" />

            <div class="mb-3 row">
                <label for="loginId" class="col-sm-2 col-form-label">아이디</label>
                <div class="col-sm-10">
                    <input type="text" id="loginId" name="loginId" placeholder="아이디 입력" class="form-control" required maxlength="20" autocomplete="username" />
                </div>
            </div>

            <div class="mb-3 row">
                <label for="pass" class="col-sm-2 col-form-label">비밀번호</label>
                <div class="col-sm-10">
                    <input type="password" name="pass" id="pass" class="form-control" required />
                </div>
            </div>

            <div class="d-flex justify-content-end align-items-center">
                <button type="submit" class="btn btn-primary ms-3">로그인</button>
            </div>
            <a href="${root}/member?action=recovery-form">비밀번호 찾기</a>
        </form>
    </div>
    <%@ include file="/fragments/footer.jsp"%>
</body>
</html>
