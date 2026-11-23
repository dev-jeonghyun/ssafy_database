"use strict";

// 회원 정보와 로그인 상태는 서버 세션에서만 관리한다.
let data = { users: [], notices: [], posts: [] };
let serverUser = null;
let sessionReady = false;
let recoveryTargetId = null;
let recoveryQuestionId = 0;
let storageError = false;
const boardStates = { notices: { mode: "list", id: null }, posts: { mode: "list", id: null } };
const $ = (selector, root = document) => root.querySelector(selector);
const fields = form => Object.fromEntries(new FormData(form));
const encode = value => btoa(String.fromCharCode(...new TextEncoder().encode(value)));
const currentUser = () => serverUser;
const isAdmin = user => user?.role === "ADMIN";
const ownsPost = (user, post) => Boolean(user && post && user.id === post.authorId);
const canManagePost = (user, post, password) => ownsPost(user, post);
const newest = items => [...items].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
const authorName = id => currentUser()?.id === id ? currentUser().name : data.users.find(user => user.id === id)?.name ?? "탈퇴한 회원";
const dateLabel = value => new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeZone: "Asia/Seoul" }).format(new Date(value));

function setCurrentUser(user) {
  serverUser = user;
  const frame = document.querySelector(".explorer-frame");
  frame?.contentWindow?.postMessage({type:"enjoytrip-session-change"}, location.origin);
  if (user) {
    const author = { id: user.id, name: user.name };
    data.users = [...data.users.filter(item => item.id !== user.id), author];
  }
}

async function memberRequest(action, values = null) {
  const url = new URL(document.body.dataset.contextPath + "/api/member", window.location.origin);
  url.searchParams.set("action", action);
  const options = { credentials: "same-origin", headers: { Accept: "application/json" } };
  if (values === null) {
    options.method = "GET";
  } else {
    options.method = "POST";
    options.headers["X-Requested-With"] = "XMLHttpRequest";
    options.body = new URLSearchParams(values);
  }
  let response;
  try {
    response = await fetch(url, options);
  } catch {
    throw new Error("서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }
  if (!response.headers.get("content-type")?.includes("application/json")) {
    throw new Error("서버에서 회원 요청을 처리하지 못했습니다. 페이지를 새로고침한 뒤 다시 시도해주세요.");
  }
  const result = await response.json();
  if (!response.ok) {
    if (response.status === 401 && action !== "login") {
      setCurrentUser(null);
      render();
      if (location.hash === "#profile") location.hash = "login";
    }
    throw new Error(result.message || "회원 요청을 처리하지 못했습니다.");
  }
  return result;
}

async function refreshUser() {
  const result = await memberRequest("current");
  setCurrentUser(result.user);
}

function message(text, error = false) {
  const box = $("#feedback");
  box.textContent = text;
  box.className = "alert " + (error ? "alert-danger" : "alert-success");
  box.setAttribute("role", error ? "alert" : "status");
  box.hidden = false;
  box.focus();
}

async function run(action) {
  try { await action(); } catch (error) { message(error.message, true); }
}

function nonblank(value, label) {
  const trimmed = value.trim();
  if (!trimmed) throw new Error(label + "을(를) 입력해주세요.");
  return trimmed;
}

function checkedPassword(password, confirmation) {
  if (password.length < 4 || password.length > 128) throw new Error("비밀번호는 4~128자로 입력해주세요.");
  if (password !== confirmation) throw new Error("비밀번호 확인이 일치하지 않습니다.");
  return password;
}

function requireUser() {
  const user = currentUser();
  if (!user) throw new Error("로그인이 필요합니다.");
  return user;
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function actionButton(text, action, id) {
  const button = element("button", "btn btn-link text-start text-break p-0", text);
  button.type = "button";
  button.dataset.action = action;
  if (id) button.dataset.id = id;
  return button;
}

function boardRoot(kind) { return $(`[data-page][data-kind="${kind}"]`); }

function renderBoard(kind) {
  const root = boardRoot(kind);
  const state = boardStates[kind];
  const user = currentUser();
  const post = data[kind].find(item => item.id === state.id);
  if (state.mode === "detail" && !post) state.mode = "list";
  if (state.mode === "editor" && (!user || (kind === "notices" ? !isAdmin(user) : post && !ownsPost(user, post)))) state.mode = "list";
  root.querySelectorAll("[data-view]").forEach(view => { view.hidden = view.dataset.view !== state.mode; });
  $("[data-action=new]", root).hidden = !user || (kind === "notices" && !isAdmin(user));
  const hasSamples = data[kind].some(item => item.isSample);
  $("[data-role=count]", root).textContent = `총 ${data[kind].length}개의 글${hasSamples ? " (예시 글 포함)" : ""}`;
  const rows = $("[data-role=rows]", root);
  rows.replaceChildren();
  for (const item of newest(data[kind])) {
    const row = element("tr");
    const title = element("td");
    title.append(actionButton(item.title, "open", item.id));
    row.append(title, element("td", "text-break", authorName(item.authorId)), element("td", "text-nowrap text-secondary", dateLabel(item.createdAt)));
    rows.append(row);
  }
  $("[data-role=empty]", root).hidden = data[kind].length > 0;
  if (state.mode === "editor") {
    const form = $("[data-form=editor]", root);
    form.reset();
    form.elements.title.value = post?.title ?? "";
    form.elements.body.value = post?.body ?? "";
    form.elements.password.required = kind === "posts";
    $("[data-role=editor-password-label]", root).hidden = kind !== "posts";
    $("[data-role=password-help]", root).hidden = kind !== "posts";
    $("[data-role=password-help]", root).textContent = post ? "글을 작성할 때 설정한 비밀번호를 입력하세요." : "수정·삭제할 때 필요한 비밀번호입니다. 회원 비밀번호와 별도로 설정하세요.";
    $("[data-role=editor-title]", root).textContent = post ? "글 수정" : "글 작성";
  }
  if (state.mode !== "detail") return;
  $("[data-role=title]", root).textContent = post.title;
  $("[data-role=meta]", root).textContent = `${authorName(post.authorId)} · ${dateLabel(post.createdAt)}${post.isSample ? " (예시 글)" : ""}${post.updatedAt !== post.createdAt ? " · 수정됨" : ""}`;
  $("[data-role=body]", root).textContent = post.body;
  const allowed = kind === "notices" ? isAdmin(user) : ownsPost(user, post);
  $("[data-action=edit]", root).hidden = !allowed;
  const deleteForm = $("[data-form=delete]", root);
  deleteForm.reset();
  deleteForm.hidden = !allowed;
  deleteForm.elements.password.required = kind === "posts";
  $("[data-role=delete-password-label]", root).hidden = kind !== "posts";
  $("[data-role=comments-area]", root).hidden = kind !== "posts";
  if (kind !== "posts") return;
  const comments = $("[data-role=comments]", root);
  comments.replaceChildren();
  $("[data-role=comment-count]", root).textContent = `(${post.comments.length})`;
  for (const comment of post.comments) {
    const row = element("li", "list-group-item px-0");
    row.append(element("small", "text-secondary", `${authorName(comment.authorId)} · ${dateLabel(comment.createdAt)}`), element("p", "post-body my-2", comment.body));
    if (ownsPost(user, comment)) row.append(actionButton("댓글 삭제", "delete-comment", comment.id));
    comments.append(row);
  }
  if (!comments.children.length) comments.append(element("li", "list-group-item text-secondary px-0", "첫 번째 댓글을 남겨보세요."));
  $("[data-form=comment]", root).hidden = !user;
  $("[data-role=comment-login]", root).hidden = Boolean(user);
}

function render() {
  const user = currentUser();
  $("#guest-menu").hidden = Boolean(user);
  $("#member-menu").hidden = !user;
  $("#member-name").textContent = user ? `${user.name}님` : "";
  for (const kind of ["notices", "posts"]) renderBoard(kind);
}

function fillProfile() {
  const user = requireUser();
  const form = $("#profile-form");
  form.reset();
  for (const key of ["loginId", "name", "question"]) form.elements.namedItem(key).value = user[key];
  $("#withdrawal-form").reset();
  $("#withdrawal-form").hidden = isAdmin(user);
  $("#admin-withdrawal-note").hidden = !isAdmin(user);
}

function navigate() {
  let page = location.hash.slice(1) || "home";
  const pages = [...document.querySelectorAll("[data-page]")];
  if (!pages.some(section => section.id === page)) page = "home";
  if (page === "profile" && !currentUser()) { location.hash = "login"; return; }
  pages.forEach(section => {
    section.hidden = section.id !== page;
    if (section.hidden && section.dataset.kind) boardStates[section.dataset.kind] = { mode: "list", id: null };
  });
  document.querySelectorAll("[data-nav]").forEach(link => {
    const active = link.dataset.nav === page;
    link.classList.toggle("active", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  if (page === "profile") fillProfile();
  if (page !== "recovery") {
    recoveryTargetId = null;
    ++recoveryQuestionId;
    $("#recovery-reset-form").reset();
    $("#recovery-reset-form").hidden = true;
  }
  document.title = `EnjoyTrip | ${$("h1", $(`#${page}`)).textContent}`;
  render();
  $("#main-content").focus();
  const collapse = $("#navbar-menu");
  if (collapse && typeof bootstrap !== "undefined") bootstrap.Collapse.getInstance(collapse)?.hide();
}

function openBoard(kind, mode, id = null) {
  boardStates[kind] = { mode, id };
  renderBoard(kind);
  const page = kind === "notices" ? "notices" : "board";
  if (location.hash !== `#${page}`) location.hash = page;
  else $("h1", boardRoot(kind)).focus();
}

function verifyManagement(kind, post, password) {
  const user = requireUser();
  if (kind === "notices") {
    if (!isAdmin(user)) throw new Error("공지사항은 관리자만 변경할 수 있습니다.");
  } else if (!canManagePost(user, post, password)) {
    throw new Error("작성자 계정과 글 비밀번호를 확인해주세요.");
  }
}


async function communityRequest(action, values = null) {
  const url = new URL(document.body.dataset.contextPath + "/api/enjoytrip", location.origin);
  url.searchParams.set("action", action);
  const options = {credentials:"same-origin", headers:{Accept:"application/json"}};
  if (values !== null) { options.method="POST"; options.headers["Content-Type"]="application/json"; options.headers["X-Requested-With"]="XMLHttpRequest"; options.body=JSON.stringify(values); }
  const response=await fetch(url,options);
  if (!response.headers.get("content-type")?.includes("application/json")) throw new Error("DB 게시판에 연결하지 못했습니다.");
  const result=await response.json();
  if (!response.ok) throw new Error(result.message || "게시판 요청에 실패했습니다.");
  return result;
}
async function refreshCommunity() { data=await communityRequest("community"); }
async function submitBoard(kind, form) {
  const values=fields(form), state=boardStates[kind], post=data[kind].find(item=>item.id===state.id);
  requireUser();
  if (form.dataset.form==="editor") {
    if (post) verifyManagement(kind,post,values.password);
    const result=await communityRequest("save-post",{kind,id:state.id,title:nonblank(values.title,"제목"),body:nonblank(values.body,"내용"),password:values.password});
    await refreshCommunity(); form.reset(); openBoard(kind,"detail",result.id); render(); message(post?"글을 수정했습니다.":"새 글을 등록했습니다.");
  } else if (form.dataset.form==="delete") {
    if (!post) throw new Error("게시글을 찾을 수 없습니다.");
    if (!confirm("이 글을 삭제할까요? 포함된 댓글도 삭제됩니다.")) return;
    await communityRequest("delete-post",{id:post.id,password:values.password});
    await refreshCommunity(); openBoard(kind,"list"); render(); message("글을 삭제했습니다.");
  } else if (form.dataset.form==="comment") {
    if (!post) throw new Error("댓글을 작성할 게시글을 찾을 수 없습니다.");
    await communityRequest("add-comment",{postId:post.id,body:nonblank(values.body,"댓글")});
    await refreshCommunity(); form.reset(); render(); message("댓글을 등록했습니다.");
  }
}

async function boardAction(kind, button) {
  const state = boardStates[kind];
  const post = data[kind].find(item => item.id === state.id);
  switch (button.dataset.action) {
    case "open": openBoard(kind, "detail", button.dataset.id); break;
    case "list": openBoard(kind, "list"); break;
    case "new":
      if (kind === "notices" && !isAdmin(requireUser())) throw new Error("공지사항은 관리자만 작성할 수 있습니다.");
      requireUser();
      openBoard(kind, "editor");
      break;
    case "edit":
      if (!post || (kind === "notices" ? !isAdmin(requireUser()) : !ownsPost(requireUser(), post))) throw new Error("이 글을 수정할 권한이 없습니다.");
      openBoard(kind, "editor", post.id);
      break;
    case "cancel": openBoard(kind, post ? "detail" : "list", post?.id); break;
    case "delete-comment": {
      const user = requireUser();
      const comment = post?.comments?.find(item => item.id === button.dataset.id);
      if (kind !== "posts" || !ownsPost(user, comment)) throw new Error("본인이 작성한 댓글만 삭제할 수 있습니다.");
      if (!confirm("댓글을 삭제할까요?")) return;
      await communityRequest("delete-comment", { id: comment.id });
      await refreshCommunity();
      render();
      message("댓글을 삭제했습니다.");
      break;
    }
  }
}

function setupForms() {
  const onSubmit = (id, handler) => $(id).addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    if (form.dataset.busy === "true") return;
    if (!sessionReady) return message("로그인 상태를 확인하고 있습니다. 잠시 후 다시 시도해주세요.", true);
    const values = fields(form);
    const buttons = [...form.querySelectorAll("button[type=submit], button:not([type])")];
    form.dataset.busy = "true";
    buttons.forEach(button => { button.disabled = true; });
    try {
      await run(() => handler(form, values));
    } finally {
      form.dataset.busy = "false";
      buttons.forEach(button => { button.disabled = false; });
    }
  });

  const loginIdInput = $("#signup-form").elements.loginId;
  const loginIdStatus = $("#signup-id-status");
  let loginIdCheck = 0;
  loginIdInput.addEventListener("input", async () => {
    const check = ++loginIdCheck;
    loginIdInput.setCustomValidity("");
    loginIdInput.classList.remove("is-valid", "is-invalid");
    loginIdStatus.textContent = "";
    if (!loginIdInput.value || !loginIdInput.validity.valid) return;
    loginIdInput.setCustomValidity("아이디 중복 확인 중입니다.");
    loginIdStatus.textContent = "아이디 중복 확인 중입니다.";
    try {
      const url = new URL(document.body.dataset.contextPath + "/api/member", window.location.origin);
      url.searchParams.set("action", "login-id-check");
      url.searchParams.set("loginId", loginIdInput.value);
      const response = await fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } });
      if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error("아이디 중복 확인에 실패했습니다.");
      const result = await response.json();
      if (check !== loginIdCheck) return;
      const canUse = result.canUse === true;
      loginIdInput.setCustomValidity(canUse ? "" : "이미 사용 중이거나 사용할 수 없는 아이디입니다.");
      loginIdInput.classList.toggle("is-valid", canUse);
      loginIdInput.classList.toggle("is-invalid", !canUse);
      loginIdStatus.textContent = canUse ? "사용할 수 있는 아이디입니다." : "이미 사용 중이거나 사용할 수 없는 아이디입니다.";
    } catch {
      if (check !== loginIdCheck) return;
      loginIdInput.setCustomValidity("아이디 중복 확인에 실패했습니다. 다시 입력해주세요.");
      loginIdInput.classList.add("is-invalid");
      loginIdStatus.textContent = "아이디 중복 확인에 실패했습니다. 다시 입력해주세요.";
    }
  });

  onSubmit("#signup-form", async (form, values) => {
    const password = checkedPassword(values.password, values.passwordConfirm);
    const result = await memberRequest("register", {
      loginId: values.loginId.trim(), name: nonblank(values.name, "이름"), password,
      passwordConfirm: values.passwordConfirm, question: nonblank(values.question, "복구 질문"), answer: nonblank(values.answer, "복구 답변")
    });
    form.reset();
    ++loginIdCheck;
    loginIdInput.setCustomValidity("");
    loginIdInput.classList.remove("is-valid", "is-invalid");
    loginIdStatus.textContent = "";
    $("#login-form").elements.loginId.value = values.loginId.trim();
    location.hash = "login";
    message(result.message);
  });
  onSubmit("#login-form", async (form, values) => {
    const result = await memberRequest("login", { loginId: values.loginId.trim(), password: values.password });
    setCurrentUser(result.user);
    form.reset();
    location.hash = "home";
    render();
    $("#feedback").hidden = true;
  });
  onSubmit("#recovery-account-form", async (form, values) => {
    recoveryTargetId = null;
    $("#recovery-reset-form").hidden = true;
    const loginId = values.loginId.trim();
    const questionRequest = ++recoveryQuestionId;
    const url = new URL(document.body.dataset.contextPath + "/api/member", window.location.origin);
    url.searchParams.set("action", "recovery-question");
    url.searchParams.set("loginId", loginId);
    const response = await fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } });
    if (!response.headers.get("content-type")?.includes("application/json")) throw new Error("복구 질문을 가져오지 못했습니다.");
    const result = await response.json();
    if (questionRequest !== recoveryQuestionId || form.elements.loginId.value.trim() !== loginId) return;
    if (!response.ok) throw new Error(result.message || "복구 질문을 가져오지 못했습니다.");
    recoveryTargetId = result.loginId;
    $("#recovery-question").textContent = result.question;
    $("#recovery-reset-form").reset();
    $("#recovery-reset-form").hidden = false;
    $("#recovery-reset-form").elements.answer.focus();
  });
  $("#recovery-account-form").elements.loginId.addEventListener("input", () => {
    recoveryTargetId = null;
    ++recoveryQuestionId;
    $("#recovery-reset-form").reset();
    $("#recovery-reset-form").hidden = true;
  });
  onSubmit("#recovery-reset-form", async (form, values) => {
    if (!recoveryTargetId) throw new Error("아이디부터 다시 확인해주세요.");
    checkedPassword(values.password, values.passwordConfirm);
    await memberRequest("recovery-verify", { loginId: recoveryTargetId, answer: values.answer.trim() });
    const result = await memberRequest("password-reset", { password: values.password, passwordConfirm: values.passwordConfirm });
    recoveryTargetId = null;
    ++recoveryQuestionId;
    form.reset();
    form.hidden = true;
    $("#recovery-account-form").reset();
    await refreshUser();
    render();
    location.hash = "login";
    message(result.message);
  });
  onSubmit("#profile-form", async (form, values) => {
    const user = requireUser();
    const question = nonblank(values.question, "복구 질문");
    if (question !== user.question && !values.answer.trim()) throw new Error("질문을 변경할 때는 새 답변도 입력해주세요.");
    if (values.password || values.passwordConfirm) checkedPassword(values.password, values.passwordConfirm);
    const result = await memberRequest("profile", {
      loginId: user.loginId, name: nonblank(values.name, "이름"), currentPassword: values.currentPassword,
      password: values.password, passwordConfirm: values.passwordConfirm, question, answer: values.answer.trim()
    });
    setCurrentUser(result.user);
    fillProfile();
    render();
    message(result.message);
  });
  onSubmit("#withdrawal-form", async (form, values) => {
    const user = requireUser();
    if (isAdmin(user)) throw new Error("관리자 계정은 이 화면에서 탈퇴할 수 없습니다.");
    if (!confirm("정말 탈퇴할까요? 계정은 삭제되며 작성한 글과 댓글은 남습니다.")) return;
    const result = await memberRequest("delete", { currentPassword: values.password });
    setCurrentUser(null);
    await refreshCommunity();
    form.reset();
    $("#profile-form").reset();
    location.hash = "home";
    render();
    message(result.message);
  });
  $("#logout-button").addEventListener("click", async () => {
    const button = $("#logout-button");
    if (button.disabled) return;
    button.disabled = true;
    try {
      await run(async () => {
        await memberRequest("logout", {});
        setCurrentUser(null);
        $("#profile-form").reset();
        $("#withdrawal-form").reset();
        document.querySelectorAll('input[type="password"]').forEach(input => { input.value = ""; });
        location.hash = "home";
        render();
        $("#feedback").hidden = true;
      });
    } finally {
      button.disabled = false;
    }
  });
}

// 게시판과 회원 상태를 DB API에서 읽는다.
async function init() {
  for (const kind of ["notices", "posts"]) {
    const root = boardRoot(kind);
    root.append($("#board-template").content.cloneNode(true));
    root.addEventListener("click", event => {
      const button = event.target.closest("[data-action]");
      if (button) run(() => boardAction(kind, button));
    });
    root.addEventListener("submit", event => {
      event.preventDefault();
      run(() => submitBoard(kind, event.target));
    });
  }
  document.querySelectorAll("form input, form textarea").forEach(control => control.classList.add("form-control"));
  document.querySelectorAll("form button:not([class])").forEach(button => { button.className = "btn btn-primary"; });
  setupForms();
  try {
    await refreshCommunity();
  } catch {
    storageError = true;
    data = { users: [], notices: [], posts: [] };
  }
  try {
    await refreshUser();
  } catch (error) {
    message(error.message, true);
  } finally {
    sessionReady = true;
  }
  window.addEventListener("hashchange", navigate);
  navigate();
  if (storageError) message("DB 게시판 정보를 읽지 못했습니다. DB 연결 설정을 확인해주세요.", true);
}

init();
