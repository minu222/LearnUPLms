package com.lms.mainpages.service;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.repository.UserRepository;
import com.lms.mainpages.repository.instructorProfileRepository;
import com.lms.mainpages.web.ProfileUpdateForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Optional;

@Service("mainService")
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final instructorProfileRepository instructorProfileRepository;

    public Optional<User> login(String nickname, String rawPassword) {
        return userRepository.findByNickname(nickname)
                .filter(u -> Objects.equals(u.getPassword(), rawPassword));
        // 운영은 반드시 PasswordEncoder 사용 권장
    }

    @Transactional
    public long register(User u, String affiliation, String bio) {
        long userId = userRepository.insertUser(u);
        if (u.getRole() == User.Role.INSTRUCTOR) {
            userRepository.insertInstructorProfile((int) userId, affiliation, bio);
        }
        return userId;
    }

    /** 프로필 업데이트 (JDBC) — UserRepository의 기존 메서드에 맞춤 */
    @Transactional
    public void updateProfile(ProfileUpdateForm f, MultipartFile profileImage) {
        // 1) 사용자 존재 확인
        User u = userRepository.findById((int) f.getUser_id())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + f.getUser_id()));

        // 2) 비밀번호는 비어있으면 미변경, 값 있으면 변경
        String pwOrNull = (f.getPassword() != null && !f.getPassword().isBlank())
                ? f.getPassword()
                : null;

        // 3) 기존 레포지토리 메서드 사용해 한 번에 업데이트
        userRepository.updateProfileAllFields(
                (int) f.getUser_id(),
                firstNonBlank(f.getNickname(), u.getNickname()),
                firstNonBlank(f.getName(),     u.getName()),
                firstNonBlank(f.getEmail(),    u.getEmail()),
                firstNonBlank(f.getPhone(),    u.getPhone()),
                firstNonBlank(f.getAddress(),  u.getAddress()),
                pwOrNull // null이면 레포 메서드에서 비번 미변경
        );

        // 4) 강사 프로필 UPSERT
        if (isInstructor(u)) {
            instructorProfileRepository.upsert(
                    f.getUser_id(),                         // users.user_id = instructor_profile.instructor_id
                    nullToEmpty(f.getAffiliation()),
                    nullToEmpty(f.getBio())
            );
        }

        // 5) 프로필 이미지(선택)
        if (profileImage != null && !profileImage.isEmpty()) {
            // storageService.store(profileImage, f.getUser_id());
            // userRepository.updateProfileImagePath((int) f.getUser_id(), savedPath);
        }
    }

    private static boolean isInstructor(User u) {
        if (u == null || u.getRole() == null) return false;
        return u.getRole() == User.Role.INSTRUCTOR
                || "INSTRUCTOR".equalsIgnoreCase(String.valueOf(u.getRole()))
                || "TEACHER".equalsIgnoreCase(String.valueOf(u.getRole()));
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
