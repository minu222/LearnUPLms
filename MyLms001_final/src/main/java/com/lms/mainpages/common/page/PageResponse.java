package com.lms.mainpages.common.page;

import lombok.*;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class PageResponse<T> {
    private List<T> content;
    private int number;           // 현재 페이지(0-base)
    private int size;             // 페이지 크기
    private long totalElements;   // 총 개수
    private int totalPages;       // 총 페이지
    private boolean first;
    private boolean last;
}
