package com.ssafy.live.model.service;

import com.ssafy.live.model.dto.Member;
import com.ssafy.live.model.dto.Page;
import com.ssafy.live.model.dto.SearchCondition;

public interface MemberService {

    int registMember(Member member) ;


    void reset() ;

    Page<Member> search(SearchCondition condition) ;

    Member login(String loginId, String password);

    Member findByLoginId(String loginId);

    boolean isLoginIdAvailable(String loginId);

    Member memberModify(Member member, String actorLoginId, String currentPassword);

    boolean memberDelete(String loginId, String actorLoginId, String currentPassword);

    String getRecoveryQuestion(String loginId);

    boolean verifyRecoveryAnswer(String loginId, String answer);

    boolean resetPassword(String loginId, String password);
}
