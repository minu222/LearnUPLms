package com.lms.mainpages.controller;

import com.lms.mainpages.web.ProfileUpdateForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute ProfileUpdateForm form,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                HttpSession session,
                                RedirectAttributes ra) {
        // 1) 로그인 사용자 확인 (세션에 따라 조정)
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        // 2) TODO: 서비스로 위임하여 DB 업데이트 수행
        // userService.updateProfile(form, profileImage);

        // 3) 파일 업로드 처리 예시 (원하는 저장소/전략에 맞게 구현)
        // if (profileImage != null && !profileImage.isEmpty()) {
        //     storageService.store(profileImage, form.getUser_id());
        // }

        ra.addFlashAttribute("message", "회원정보가 저장되었습니다.");
        return "redirect:/myclass/myInfo";
    }
}
