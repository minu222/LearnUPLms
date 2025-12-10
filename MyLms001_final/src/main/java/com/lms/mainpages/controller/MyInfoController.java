package com.lms.mainpages.controller;

import com.lms.mainpages.exceptoin.DuplicateFieldException;
import com.lms.mainpages.repository.UserRepository;
import com.lms.mainpages.web.ProfileUpdateForm;
import com.lms.mainpages.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/myclass/myInfo")
@RequiredArgsConstructor
public class MyInfoController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/update")
    public String update(@ModelAttribute ProfileUpdateForm form,
                         @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                         HttpSession session,
                         RedirectAttributes ra) {

        // (선택) 로그인 가드: 로그인 안 되어 있으면 로그인 페이지로
        if (session.getAttribute("loginUser") == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login?redirect=/myclass/myInfo";
        }

        log.info("Profile update request: user_id={}, nickname={}, name={}, phone={}, address={}, email={}, affiliation={}, bio={}",
                form.getUser_id(), form.getNickname(), form.getName(), form.getPhone(), form.getAddress(), form.getEmail(),
                form.getAffiliation(), form.getBio());

        try {
            // 내부에서 닉네임은 무시(업데이트 금지), 이메일 중복 시 DuplicateFieldException 던짐
            userService.updateProfile(form, profileImage);

            // ✅ 세션 사용자 최신화 (화면에 바로 반영되게)
            userRepository.findById((int) form.getUser_id())
                    .ifPresent(u -> session.setAttribute("loginUser", u));

            ra.addFlashAttribute("msg", "저장되었습니다.");
        } catch (DuplicateFieldException ex) {
            // 이메일/닉네임 중복을 폼 에러로 노출 (템플릿에서 emailError/nicknameError 표시)
            if ("email".equalsIgnoreCase(ex.getField())) {
                ra.addFlashAttribute("emailError", "중복된 이메일입니다.");
            } else if ("nickname".equalsIgnoreCase(ex.getField())) {
                ra.addFlashAttribute("nicknameError", "중복된 아이디입니다.");
            } else {
                ra.addFlashAttribute("errorMessage", "중복 오류가 발생했습니다.");
            }
            // 실패 시 성공 메시지 넣지 않음
            log.warn("Duplicate field on profile update: field={}, value={}", ex.getField(), ex.getValue());
        } catch (Exception e) {
            // 예기치 못한 오류는 사용자 친화 메시지 + 서버 로그
            log.error("Profile update failed", e);
            ra.addFlashAttribute("errorMessage", "저장을 처리하는 중 오류가 발생했습니다.");
        }

        // PRG 패턴: GET /myclass/myInfo 로 복귀 (템플릿에서 emailError/nicknameError를 표시)
        return "redirect:/myclass/myInfo";
    }
}