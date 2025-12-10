package com.lms.mainpages.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateForm {
    // 💡 템플릿에서 name="user_id" 로 보내므로 언더스코어 그대로 사용
    private long user_id;

    private String nickname;
    private String password;   // 비밀번호는 별도 정책 권장
    private String name;
    private String phone;
    private String address;
    private String email;

    // 강사 전용
    private String affiliation;
    private String bio;

    // 선택값
    private Boolean emailAgree;
    private Boolean noteAgree;
}