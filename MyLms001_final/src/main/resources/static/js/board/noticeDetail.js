/**
 * 게시판 상세 공용 스크립트 (공지/자유 공용)
 * - API: GET /api/posts/{id}
 * - 상세 페이지 경로: /board/notice/{id} 또는 /board/free/{id}
 * - HTML 요구 요소(둘 중 아무거나 있으면 채움):
 *   - 제목: #title  또는 .board-title
 *   - 날짜: #date   또는 .post-info
 *   - 본문: #content 또는 .post-content
 *   - 목록 버튼: #backToList 또는 .button-group a.cancel-btn
 */
(async function () {
    'use strict';

    /** ========================= 유틸 ========================= */
    function getIdFromPath() {
        // URL 끝의 숫자 추출
        const parts = (location.pathname || '').split('/').filter(Boolean);
        const last = parts[parts.length - 1] || '';
        const id = last.replace(/\D/g, '');
        return id ? Number(id) : null;
    }

    function detectCategoryFromPath() {
        const path = location.pathname || '';
        return path.includes('/free/') ? 'free' : 'NOTICE';
    }

    function formatDate(isoLike) {
        if (!isoLike) return '';
        return String(isoLike).replace('T', ' ').slice(0, 16);
    }

    /** ========================= 요소 참조 ========================= */
    const elTitle   = document.getElementById('title')   || document.querySelector('.board-title');
    const elDate    = document.getElementById('date')    || document.querySelector('.post-info');
    const elContent = document.getElementById('content') || document.querySelector('.post-content');
    const elBack    = document.getElementById('backToList') || document.querySelector('.button-group a.cancel-btn');

    /** ========================= 데이터 로드 ========================= */
    const id = getIdFromPath();
    if (!id) {
        console.warn('[noticeDetail] URL에서 게시글 ID를 찾지 못했습니다.');
        return;
    }

    let data;
    try {
        const res = await fetch(`/api/posts/${id}`, {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`상세 조회 실패 (${res.status}) ${text}`);
        }
        data = await res.json();
    } catch (err) {
        console.error('[noticeDetail] 상세 로드 실패:', err);
        // 최소한의 오류 메시지 표시
        if (elContent) {
            elContent.innerHTML = '<p>게시글을 불러오는 중 오류가 발생했습니다.</p>';
        }
        return;
    }

    /** ========================= 렌더링 ========================= */
    // 제목
    if (elTitle) {
        // 기존 내용이 서버 렌더로 채워졌을 수 있으므로 덮어씀
        elTitle.textContent = data.title ?? '(제목 없음)';
    }

    // 날짜
    if (elDate) {
        const display = '등록일: ' + formatDate(data.createdAt);
        // post-info 영역일 경우 텍스트만 갱신
        if (elDate.id === 'date' || elDate.classList.contains('post-info')) {
            // 기존 텍스트 제거 후 표시
            elDate.textContent = display;
        } else {
            // 기타 영역이면 추가
            elDate.textContent = display;
        }
    }

    // 본문 (HTML 허용)
    if (elContent) {
        // 주의: 서버 저장 시 XSS 필터/화이트리스트 전제
        elContent.innerHTML = data.content ?? '';
    }

    // 목록으로 링크
    const detected = detectCategoryFromPath();            // 경로 기준 추정
    const href = (data.category === 'free' || detected === 'free') ? '/board/free' : '/board/notice';
    if (elBack) elBack.setAttribute('href', href);
})();
