<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>EnjoyTrip | 함께 나누는 여행</title>
  <meta name="description" content="국내 관광지를 검색하고 여행 동선을 만들며 나만의 핫플을 기록하는 EnjoyTrip입니다.">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
  <link href="${pageContext.request.contextPath}/css/style.css?v=20261106-map-select-fix" rel="stylesheet">
</head>
<body data-context-path="${pageContext.request.contextPath}">
  <a href="#main-content" class="skip-link">본문으로 바로가기</a>
  <header class="masthead border-bottom">
    <nav class="navbar navbar-expand-lg container py-3" aria-label="주 메뉴">
      <a href="#home" class="navbar-brand fw-bold text-primary" aria-label="EnjoyTrip 홈"><svg class="brand-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true"><path d="M26 13c0 7-10 15-10 15S6 20 6 13a10 10 0 1 1 20 0Z" stroke="currentColor" stroke-width="2.5"/><path d="m12 13 3 3 5-6" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>EnjoyTrip</a>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbar-menu" aria-controls="navbar-menu" aria-expanded="false" aria-label="메뉴 열기"><span class="navbar-toggler-icon"><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg></span></button>
      <div class="collapse navbar-collapse" id="navbar-menu">
        <div class="navbar-nav me-auto gap-lg-3">
          <a href="#home" data-nav="home" class="nav-link">여행 만들기</a>
          <a href="#notices" data-nav="notices" class="nav-link">공지사항</a>
          <a href="#board" data-nav="board" class="nav-link">공유게시판</a>
        </div>
        <div id="guest-menu" class="d-flex flex-wrap align-items-center gap-3"><a href="#login" class="text-body-secondary text-decoration-none">로그인</a> <a href="#signup" class="btn btn-primary btn-sm">회원가입</a></div>
        <div id="member-menu" class="d-flex flex-wrap align-items-center gap-3" hidden><span id="member-name" class="small text-break"></span> <a href="#profile" class="text-decoration-none">내 정보</a> <button type="button" id="logout-button" class="btn btn-outline-secondary btn-sm">로그아웃</button></div>
      </div>
    </nav>
  </header>
  <main id="main-content" class="container py-4 py-md-5" tabindex="-1">
    <div id="feedback" role="status" aria-live="polite" tabindex="-1" hidden></div>
    <section id="home" data-page>
      <div class="hero">
        <div class="hero-heading"><h1 tabindex="-1">다음 여행, 어디로 떠날까요?</h1><p>장소를 찾고, 경로를 만들고, 나만의 핫플을 기록하세요.</p></div>
        <svg class="hero-symbol" viewBox="0 0 64 64" fill="none" aria-hidden="true"><circle cx="32" cy="32" r="27" stroke="currentColor" stroke-width="2"/><path d="m42 22-7 13-13 7 7-13 13-7Z" stroke="currentColor" stroke-width="2.5" stroke-linejoin="round"/><path d="M32 5v5m0 44v5M5 32h5m44 0h5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </div>
      <figure class="atlas">
        <div id="map">
          <iframe
            src="${pageContext.request.contextPath}/01_api/05_openapi_map.html?v=20261106-map-select-fix"
            title="지역별 관광지 지도 및 여행 계획"
            class="explorer-frame"
          ></iframe>
        </div>
      </figure>
    </section>
    <section id="notices" data-page data-kind="notices" hidden><h1 tabindex="-1">공지사항</h1><p>EnjoyTrip의 새로운 소식을 확인하세요.</p></section>
    <section id="board" data-page data-kind="posts" hidden><h1 tabindex="-1">공유게시판</h1><p>나만의 여행 경험과 유용한 정보를 함께 나눠보세요.</p></section>
    <section id="signup" data-page class="account-page" hidden>
      
      <h1 tabindex="-1">회원가입</h1><p>함께 여행 이야기를 나눌 계정을 만들어보세요.</p>
      <form id="signup-form">
        <label>아이디 <input name="loginId" required pattern="[a-z0-9_]{4,20}" maxlength="20" autocomplete="username" aria-describedby="signup-id-help signup-id-status"></label>
        <p id="signup-id-help">소문자 영문, 숫자, 밑줄 4~20자</p>
        <p id="signup-id-status" class="small" aria-live="polite"></p>
        <label>이름 <input name="name" required maxlength="40" autocomplete="name"></label>
        <label>비밀번호 <input name="password" type="password" required minlength="4" maxlength="128" autocomplete="new-password"></label>
        <label>비밀번호 확인 <input name="passwordConfirm" type="password" required minlength="4" maxlength="128" autocomplete="new-password"></label>
        <label>복구 질문 <input name="question" required maxlength="100" placeholder="예: 가장 좋아하는 여행지는?"></label>
        <label>복구 답변 <input name="answer" required maxlength="100" autocomplete="off"></label>
        <p>비밀번호 재설정 시 질문의 답변이 필요합니다.</p>
        <button type="submit">가입하기</button><a href="#login">이미 계정이 있나요?</a>
      </form>
    </section>
    <section id="login" data-page class="account-page" hidden>
      
      <h1 tabindex="-1">로그인</h1><p>다시 만나 반갑습니다.</p>
      <form id="login-form">
        <label>아이디 <input name="loginId" required maxlength="20" autocomplete="username"></label>
        <label>비밀번호 <input name="password" type="password" required maxlength="128" autocomplete="current-password"></label>
        <button type="submit">로그인</button>
        <div><a href="#recovery">비밀번호 찾기</a> <a href="#signup">회원가입</a></div>
      </form>
    </section>
    <section id="recovery" data-page class="account-page" hidden>
      
      <h1 tabindex="-1">비밀번호 찾기</h1><p>가입할 때 설정한 질문에 답하고 새 비밀번호를 설정하세요.</p>
      <form id="recovery-account-form">
        <label>아이디 <input name="loginId" required maxlength="20" autocomplete="username"></label>
        <button type="submit">복구 질문 확인</button>
      </form>
      <form id="recovery-reset-form" hidden>
        <p>복구 질문: <strong id="recovery-question"></strong></p>
        <label>답변 <input name="answer" required maxlength="100" autocomplete="off"></label>
        <label>새 비밀번호 <input name="password" type="password" required minlength="4" maxlength="128" autocomplete="new-password"></label>
        <label>새 비밀번호 확인 <input name="passwordConfirm" type="password" required minlength="4" maxlength="128" autocomplete="new-password"></label>
        <button type="submit">비밀번호 재설정</button>
      </form>
      <a href="#login">로그인으로 돌아가기</a>
    </section>
    <section id="profile" data-page class="account-page" hidden>
      
      <h1 tabindex="-1">내 정보</h1><p>회원 정보를 확인하고 수정할 수 있습니다.</p>
      <form id="profile-form">
        <label>아이디 <input name="loginId" readonly autocomplete="username"></label>
        <label>이름 <input name="name" required maxlength="40" autocomplete="name"></label>
        <label>현재 비밀번호 <input name="currentPassword" type="password" required maxlength="128" autocomplete="current-password"></label>
        <label>새 비밀번호 <input name="password" type="password" minlength="4" maxlength="128" autocomplete="new-password"></label>
        <label>새 비밀번호 확인 <input name="passwordConfirm" type="password" minlength="4" maxlength="128" autocomplete="new-password"></label>
        <label>복구 질문 <input name="question" required maxlength="100"></label>
        <label>새 복구 답변 <input name="answer" maxlength="100" autocomplete="off"></label>
        <p>새 비밀번호·답변을 비워두면 기존 값을 유지합니다. 질문 변경 시 새 답변을 입력하세요.</p>
        <button type="submit">정보 수정</button>
      </form>
      <article id="withdrawal-area" class="border-top mt-5 pt-4">
        <h2 class="h5">회원탈퇴</h2><p>탈퇴 후에도 작성한 글과 댓글은 유지되며 작성자는 ‘탈퇴한 회원’으로 표시됩니다.</p>
        <p id="admin-withdrawal-note" hidden>기본 관리자는 공지사항 관리를 위해 탈퇴할 수 없습니다.</p>
        <form id="withdrawal-form">
          <label>현재 비밀번호 <input name="password" type="password" required maxlength="128" autocomplete="current-password"></label>
          <button type="submit" class="btn btn-outline-danger">회원탈퇴</button>
        </form>
      </article>
    </section>
  </main>
  <footer class="border-top"><div class="container py-4 d-flex flex-wrap justify-content-between gap-2 text-secondary"><p class="mb-0">EnjoyTrip, 함께 발견하고 함께 나누는 여행</p><small>© 2026 EnjoyTrip</small></div></footer>

  <template id="board-template">
    <div data-view="list">
      <div class="board-toolbar d-flex justify-content-between align-items-center my-4"><p data-role="count" class="text-secondary mb-0"></p><button type="button" class="btn btn-primary" data-action="new" hidden>글 작성</button></div>
      <div class="table-responsive"><table class="table table-hover align-middle"><caption class="visually-hidden">게시글 목록</caption><thead><tr><th scope="col">제목</th><th scope="col">작성자</th><th scope="col">작성일</th></tr></thead><tbody data-role="rows"></tbody></table></div>
      <p data-role="empty" hidden>아직 등록된 글이 없습니다.</p>
    </div>
    <article data-view="detail" class="journal-article mt-4" hidden>
      <h2 data-role="title" class="h4 text-break"></h2><p data-role="meta" class="small text-secondary"></p><p data-role="body" class="post-body border-top pt-4"></p>
      <div class="board-actions d-flex gap-2 mb-3">
        <button type="button" class="btn btn-outline-secondary" data-action="list">목록으로</button>
        <button type="button" class="btn btn-outline-primary" data-action="edit" hidden>수정</button>
      </div>
      <form data-form="delete" hidden>
        <label data-role="delete-password-label">글 비밀번호 <input name="password" type="password" required maxlength="128" autocomplete="off"></label>
        <button type="submit" class="btn btn-outline-danger">글 삭제</button>
      </form>
      <section data-role="comments-area" class="border-top mt-4 pt-4" hidden>
        <h3 class="h5">댓글 <span data-role="comment-count"></span></h3><ul data-role="comments" class="list-group list-group-flush mb-3"></ul>
        <p data-role="comment-login">댓글 작성은 <a href="#login">로그인</a> 후 가능합니다.</p>
        <form data-form="comment" hidden>
          <label>댓글 <textarea name="body" required maxlength="1000" rows="3" placeholder="여행 이야기에 답해보세요."></textarea></label>
          <button type="submit">댓글 등록</button>
        </form>
      </section>
    </article>
    <div data-view="editor" class="writing-desk mt-4" hidden>
      <h2 data-role="editor-title" class="h4">글 작성</h2>
      <form data-form="editor">
        <label>제목 <input name="title" required maxlength="100"></label>
        <label>내용 <textarea name="body" required maxlength="5000" rows="10"></textarea></label>
        <label data-role="editor-password-label">글 비밀번호 <input name="password" type="password" required minlength="4" maxlength="128" autocomplete="off"></label>
        <p data-role="password-help">수정·삭제할 때 필요한 비밀번호입니다. 회원 비밀번호와 별도로 설정하세요.</p>
        <button type="submit">저장</button><button type="button" class="btn btn-outline-secondary" data-action="cancel">취소</button>
      </form>
    </div>
  </template>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous" defer></script>
  <script src="${pageContext.request.contextPath}/js/ui.js?v=20261106-map-select-fix" defer></script>
  <script src="${pageContext.request.contextPath}/js/main.js?v=20261106-map-select-fix" defer></script>
</body>
</html>
