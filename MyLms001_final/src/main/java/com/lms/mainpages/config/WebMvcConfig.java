package com.lms.mainpages.config;

import com.lms.mainpages.auth.LoginRequiredInterceptor;
import com.lms.mainpages.board.free.FreeBoardListInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginRequiredInterceptor loginRequired;
    private final FreeBoardListInterceptor freeBoardListInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1) 로그인 체크가 항상 제일 먼저
        registry.addInterceptor(loginRequired)
                .order(0)
                .addPathPatterns("/board/**")              // ✅ 게시판 모든 경로 기본 차단
                .excludePathPatterns(
                        "/board/notice/**", "/board/faq/**", // 공개 보드
                        "/login", "/login/**",
                        "/user/login", "/user/login/**",
                        "/user/logout", "/user/logout/**",
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/favicon.ico", "/error"
                );

        // 2) 목록 데이터 주입 인터셉터는 그 다음
        registry.addInterceptor(freeBoardListInterceptor)
                .order(1)
                .addPathPatterns("/board/free/**");
    }
}
