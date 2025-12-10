package com.lms.mainpages.api.dto;

import java.time.LocalDateTime;

/** 네이티브/JPQL 결과를 인터페이스 기반 프로젝션으로 매핑 */

public interface PostSummary {
    Long getId();
    String getTitle();
    String getContent();
    String getCategory();
    LocalDateTime getCreatedAt(); // DB가 DATE/TIMESTAMP면 자동 매핑
}
