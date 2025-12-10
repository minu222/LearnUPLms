/**
 * 게시판 목록 공용 스크립트 (공지/자유 공용)
 * - API: GET /api/posts?category=NOTICE|free&page=0&size=10&q=...
 * - 템플릿에서 window.postCategory 또는 data-category 로 카테고리 주입 가능
 * - HTML 요구 요소:
 *   - <table id="noticeTable"><tbody>...</tbody></table>
 *   - <div id="pagination"></div>
 *   - (선택) <input id="searchInput">, <button id="searchBtn">
 */
(function () {
    'use strict';

    /** ========================= 설정 ========================= */
    const rowsPerPage = 10;               // 서버 size와 통일
    let currentPage = 0;                  // 0-based
    let currentQuery = '';

    // 카테고리 결정: 전역 → data-attribute → 기본 NOTICE
    const catFromGlobal = (typeof window !== 'undefined' && window.postCategory) ? window.postCategory : null;
    const boardRoot = document.getElementById('boardRoot');
    const catFromData = boardRoot?.dataset?.category;
    const postCategory = (catFromGlobal || catFromData || 'NOTICE').trim();

    /** ========================= 요소 캐시 ========================= */
    const table = document.getElementById('noticeTable');
    const tbody = table ? table.querySelector('tbody') : null;
    const pagination = document.getElementById('pagination');
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');

    if (!tbody || !pagination) {
        console.warn('[noticeBoard] 필수 요소가 없습니다: #noticeTable tbody, #pagination');
    }

    /** ========================= API ========================= */
    async function fetchPosts(page = 0, q = '') {
        const params = new URLSearchParams({
            page: String(page),
            size: String(rowsPerPage),
            category: postCategory
        });
        if (q && q.trim()) params.append('q', q.trim());

        const res = await fetch('/api/posts?' + params.toString(), {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`목록 조회 실패 (${res.status}) ${text}`);
        }
        return res.json();
    }

    /** ========================= 렌더링 ========================= */
    function renderRows(items) {
        if (!tbody) return;
        tbody.innerHTML = '';

        const basePath = (postCategory === 'free') ? '/board/free/' : '/board/notice/';

        if (!items || items.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 3;
            td.textContent = '게시글이 없습니다.';
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }

        items.forEach(item => {
            const tr = document.createElement('tr');

            // 번호
            const tdId = document.createElement('td');
            tdId.textContent = item.id ?? '';
            tr.appendChild(tdId);

            // 제목 (상세 링크)
            const tdTitle = document.createElement('td');
            const a = document.createElement('a');
            a.href = basePath + item.id;
            a.textContent = item.title ?? '(제목 없음)';
            tdTitle.appendChild(a);
            tr.appendChild(tdTitle);

            // 등록일
            const tdDate = document.createElement('td');
            tdDate.textContent = formatDate(item.createdAt);
            tr.appendChild(tdDate);

            tbody.appendChild(tr);
        });
    }

    function renderPagination(pg) {
        if (!pagination) return;
        pagination.innerHTML = '';

        // 안전장치
        const totalPages = Math.max(0, pg?.totalPages ?? 0);
        const number = Math.max(0, pg?.number ?? 0);
        const isFirst = !!pg?.first;
        const isLast = !!pg?.last;

        // 이전
        const prev = document.createElement('button');
        prev.textContent = '이전';
        prev.disabled = isFirst || totalPages === 0;
        prev.addEventListener('click', () => go(number - 1));
        pagination.appendChild(prev);

        // 주변 페이지만 노출 (±2)
        const start = Math.max(0, number - 2);
        const end = Math.min(totalPages - 1, number + 2);
        for (let i = start; i <= end; i++) {
            const btn = document.createElement('button');
            btn.textContent = String(i + 1);
            if (i === number) btn.classList.add('active');
            btn.addEventListener('click', () => go(i));
            pagination.appendChild(btn);
        }

        // 다음
        const next = document.createElement('button');
        next.textContent = '다음';
        next.disabled = isLast || totalPages === 0;
        next.addEventListener('click', () => go(number + 1));
        pagination.appendChild(next);
    }

    /** ========================= 유틸 ========================= */
    function formatDate(isoLike) {
        if (!isoLike) return '';
        // "YYYY-MM-DDTHH:mm:ss" → "YYYY-MM-DD HH:mm"
        return String(isoLike).replace('T', ' ').slice(0, 16);
    }

    /** ========================= 동작 ========================= */
    async function go(page = 0) {
        try {
            currentPage = Math.max(0, page);
            const data = await fetchPosts(currentPage, currentQuery);
            renderRows(data.content || []);
            renderPagination(data);
        } catch (err) {
            console.error('[noticeBoard] 목록 갱신 실패:', err);
            if (tbody) {
                tbody.innerHTML = `
          <tr><td colspan="3">목록을 불러오는 중 오류가 발생했습니다.</td></tr>
        `;
            }
        }
    }

    // 검색 이벤트(선택)
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => {
            currentQuery = searchInput.value || '';
            go(0);
        });
        // Enter 키로도 검색
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                currentQuery = searchInput.value || '';
                go(0);
            }
        });
    }

    // 초기 로딩
    go(0);
})();
