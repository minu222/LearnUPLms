package com.lms.mainpages.board;

import java.util.Locale;

public enum BoardType {
    NOTICE("notice", "noticeBoard", "noticeDetail"),
    FAQ("faq", "faqBoard", "faqDetail"),
    FREE("free", "freeBoard", "freeDetail"),
    QNA("qna", "qnaBoard", "qnaDetail"),
    TEACHER_REVIEW("teacherReview", "teacherReview", "teacherReviewDetail"),
    COURSE_REVIEW("courseReview", "courseReview", "courseReviewDetail");

    private final String slug;       // URL 세그먼트 (/board/{slug})
    private final String listName;   // 목록 템플릿 파일명 (board/{listName}.html)
    private final String detailName; // 상세 템플릿 파일명 (board/{detailName}.html)

    BoardType(String slug, String listName, String detailName) {
        this.slug = slug;
        this.listName = listName;
        this.detailName = detailName;
    }

    public String slug() { return slug; }

    /** 목록 프래그먼트 경로: ex) board/noticeBoard :: content */
    public String listFragment()   { return "board/" + listName   + " :: content"; }

    /** 상세 프래그먼트 경로: ex) board/noticeDetail :: content */
    public String detailFragment() { return "board/" + detailName + " :: content"; }

    public static BoardType from(String raw) {
        String key = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (BoardType t : values()) {
            if (t.slug.equalsIgnoreCase(key)) return t;
        }
        throw new IllegalArgumentException("Unknown board type: " + raw);
    }
}
