package com.ssafy.live.model.dto;

import java.io.Serializable;

public class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    private long mno;
    private String name;
    private String loginId;
    private String password;
    private String role;
    private String question;
    private String answer;

    public Member() {
    }

    public Member(String name, String loginId, String password) {
        this(0, name, loginId, password, "USER", null, null);
    }

    public Member(long mno, String name, String loginId, String password, String role) {
        this(mno, name, loginId, password, role, null, null);
    }

    public Member(long mno, String name, String loginId, String password, String role,
            String question, String answer) {
        this.mno = mno;
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.question = question;
        this.answer = answer;
    }

    public long getMno() {
        return mno;
    }

    public void setMno(long mno) {
        this.mno = mno;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "Member [mno=" + mno + ", name=" + name + ", loginId=" + loginId + ", role=" + role + "]";
    }

}
