// src/main/resources/static/js/myclass/studentCourses.js
// ✅ enrollments(수강등록) 기준으로 "수강중(progress)" 렌더
(function () {
    'use strict';

    // ===== 중복 로드 방지 가드 =====
    if (window.__STUDENT_COURSES_BOUND__) return;
    window.__STUDENT_COURSES_BOUND__ = true;

    /** ====== DOM ====== */
    const $id = (id) => document.getElementById(id);
    const courseGrid   = $id('courseGrid');
    const pagination   = $id('pagination');
    const filterSelect = $id('filter');
    const searchInput  = $id('searchInput');
    const searchBtn    = $id('searchBtn');

    /** ====== 상태 ====== */
    const PER_PAGE = 10;
    let currentPage = 1;
    let rawItems  = [];
    let viewItems = [];

    // ✅ 팝업/클릭 상태
    const _popupRefs = {};
    const _popupWatchTimer = {};
    const _clickLock = {};
    const _opening = {}; // 열기 중 레이스 가드
    const ENABLE_FALLBACK = false; // ❗ 팝업 핸들이 null일 때 새 탭/현재 탭 대체 동작 사용 여부(기본 비활성)

    /** ====== 유틸 ====== */
    function escapeHtml(s) {
        return String(s ?? '')
            .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
            .replaceAll('"','&quot;').replaceAll("'","&#39;");
    }
    function fmtDateOnly(s) {
        if (!s) return '';
        const d = new Date(s);
        if (!isFinite(d)) return String(s).slice(0,10);
        const yy = d.getFullYear();
        const mm = String(d.getMonth()+1).padStart(2,'0');
        const dd = String(d.getDate()).padStart(2,'0');
        return `${yy}-${mm}-${dd}`;
    }
    function buildPeriod(start_at, end_at) {
        const left  = fmtDateOnly(start_at);
        const right = fmtDateOnly(end_at);
        if (left && right) return `${left} ~ ${right}`;
        return right || left || '';
    }
    function formatWon(v) {
        const n = Number(v ?? 0) || 0;
        return n.toLocaleString('ko-KR') + '원';
    }

    /** ====== 서버에서 enrollments 불러오기 ====== */
    async function loadFromEnrollments() {
        if (courseGrid) courseGrid.innerHTML = '<p style="padding:16px;color:#666;">불러오는 중...</p>';
        try {
            const res = await fetch('/api/enrollments/my', { headers: { 'Accept': 'application/json' } });
            if (res.status === 401) throw new Error('로그인이 필요합니다.');
            if (!res.ok) throw new Error('목록 조회에 실패했습니다.');
            const data = await res.json();

            rawItems = Array.isArray(data) ? data.map(row => {
                const title    = row.title || '(제목 없음)';
                const teacher  = row.instructor_name || '';
                const period   = buildPeriod(row.enrolled_at, row.expired_at);

                // 🔹 로컬스토리지에서 진행률 가져오기
                const savedProgress = localStorage.getItem(`progress_${row.course_id}`);
                const progress = savedProgress ? Number(savedProgress) : 0;

                const isFree   = !!row.is_free && (row.is_free === 1 || String(row.is_free).toLowerCase() === 'true');

                const now = Date.now();
                const expiredAtMs = row.expired_at ? new Date(row.expired_at).getTime() : null;
                const canReview = expiredAtMs !== null && isFinite(expiredAtMs) && now >= expiredAtMs;

                return {
                    id: row.course_id,
                    title,
                    teacher,
                    period,
                    score: row.score ?? 0,
                    progress,              // 🔹 저장된 진행률 반영
                    status: 'progress',
                    priceText: isFree ? '무료' : formatWon(row.price),
                    purchaseDate: row.enrolled_at,
                    lastAccess:  row.enrolled_at,
                    image: row.image_url || `https://picsum.photos/300/150?random=${encodeURIComponent(row.course_id)}`,
                    canReview,
                    expiredAt: row.expired_at || null
                };
            }) : [];



            applyAndRender();
            console.log(data);
        } catch (e) {
            if (courseGrid) courseGrid.innerHTML = `<p style="padding:16px;color:#c00;">${escapeHtml(e.message || '오류가 발생했습니다.')}</p>`;
            pagination && (pagination.innerHTML = '');
        }


    }




    /** ====== 필터/검색 ====== */
    function getFiltered() {
        let items = [...rawItems];

        const keyword = (searchInput?.value || '').trim().toLowerCase();
        if (keyword) {
            items = items.filter(c =>
                c.title.toLowerCase().includes(keyword) ||
                (c.teacher || '').toLowerCase().includes(keyword)
            );
        }

        const filter = filterSelect?.value || 'all';
        if (filter === 'progress') {
            items = items.filter(c => c.status === 'progress');
        } else if (filter === 'completed') {
            items = items.filter(c => c.status === 'completed');
        } else if (filter === 'purchase') {
            items.sort((a, b) => new Date(b.purchaseDate) - new Date(a.purchaseDate));
        } else if (filter === 'recent') {
            items.sort((a, b) => new Date(b.lastAccess) - new Date(a.lastAccess));
        }
        return items;
    }
    function applyAndRender(resetPage = true) {
        viewItems = getFiltered();
        if (resetPage) currentPage = 1;
        render();
    }

    /** ====== 렌더 ====== */
    function render() { renderCourses(); renderPagination(); }

    function renderCourses() {
        if (!courseGrid) return;
        courseGrid.innerHTML = '';

        const total = viewItems.length;
        if (total === 0) { courseGrid.innerHTML = '<p>표시할 강의가 없습니다.</p>'; return; }

        const start = (currentPage - 1) * PER_PAGE;
        const pageCourses = viewItems.slice(start, start + PER_PAGE);

        pageCourses.forEach(c => {
            const reviewAttr  = c.canReview ? '' : 'aria-disabled="true" data-disabled="true"';
            const reviewTitle = c.canReview ? '' : 'title="강의가 만료된 이후에 작성할 수 있습니다."';

            const card = document.createElement('div');
            card.className = 'course-card';
            card.innerHTML = `
        <img src="${escapeHtml(c.image)}" alt="강의 썸네일">
        <h3>${escapeHtml(c.title)}</h3>
        <p>${c.teacher ? `강사: ${escapeHtml(c.teacher)}` : ''}</p>
        <p>학습기간: ${escapeHtml(c.period)}</p>
        <p>시험: ${c.score} 점</p>
        <p>진도율: ${c.progress}%</p>
        <div class="actions">
  <button type="button" class="btn-enter" data-popup="lecture" data-course-id="${encodeURIComponent(c.id)}">강의실 입장</button>
  <!-- 시험보기: 항상 노출 -->
  <button type="button" data-link="/exam/start/${encodeURIComponent(c.id)}">시험보기</button>
  <!-- 후기 버튼: 동일 스타일 적용 -->
  <button type="button" class="btn-review" ${reviewAttr} ${reviewTitle}
          data-link="/review/course/${encodeURIComponent(c.id)}">강의후기</button>
  <button type="button" class="btn-review" ${reviewAttr} ${reviewTitle}
          data-link="/review/instructor/${encodeURIComponent(c.id)}">강사후기</button>
</div>
      `;
            courseGrid.appendChild(card);
        });
    }

    function renderPagination() {
        if (!pagination) return;
        const total = viewItems.length;
        const pages = Math.max(1, Math.ceil(total / PER_PAGE));
        pagination.innerHTML = '';
        for (let i = 1; i <= pages; i++) {
            const btn = document.createElement('button');
            btn.textContent = String(i);
            if (i === currentPage) btn.classList.add('active');
            btn.addEventListener('click', () => {
                currentPage = i;        // 페이지 변경
                applyAndRender(false);  // resetPage=false → 현재 페이지 유지
            });

            pagination.appendChild(btn);
        }
    }

    /** ====== 팝업 유틸 ====== */
    function openCenteredPopup(url, name, w = 1100, h = 800) {
        // 열기 중 레이스 가드
        if (_opening[name]) return _popupRefs[name] || null;
        _opening[name] = true;

        try {
            // 이미 열려 있으면 재사용
            if (_popupRefs[name] && !_popupRefs[name].closed) {
                try {
                    _popupRefs[name].focus();
                    _popupRefs[name].location.href = url;
                    return _popupRefs[name];
                } catch (_) { /* 새로 열기 시도 */ }
            }

            const dualScreenLeft = window.screenLeft ?? window.screenX ?? 0;
            const dualScreenTop  = window.screenTop  ?? window.screenY ?? 0;
            const width  = window.outerWidth  ?? document.documentElement.clientWidth  ?? screen.width;
            const height = window.outerHeight ?? document.documentElement.clientHeight ?? screen.height;
            const left = dualScreenLeft + Math.max(0, (width - w) / 2);
            const top  = dualScreenTop  + Math.max(0, (height - h) / 2);

            const features = `popup=yes,noopener,resizable=yes,scrollbars=yes,width=${w},height=${h},left=${Math.round(left)},top=${Math.round(top)}`;
            const win = window.open(url, name, features);

            if (win) {
                _popupRefs[name] = win;

                // 닫힘 감시
                if (_popupWatchTimer[name]) clearInterval(_popupWatchTimer[name]);
                _popupWatchTimer[name] = setInterval(() => {
                    try {
                        if (!win || win.closed) {
                            clearInterval(_popupWatchTimer[name]);
                            delete _popupWatchTimer[name];
                            delete _popupRefs[name];
                        }
                    } catch (_) {
                        clearInterval(_popupWatchTimer[name]);
                        delete _popupWatchTimer[name];
                    }
                }, 500);

                return win;
            }

            // ❗ 일부 브라우저는 실제로 열려도 핸들이 null일 수 있음 → 여기서 “추가 동작”은 기본 비활성(중복 창 방지)
            if (ENABLE_FALLBACK === true) {
                // 필요 시만 수동으로 켜서 사용:
                // 1) 새 탭 시도
                try {
                    const a = document.createElement('a');
                    a.href = url;
                    a.target = '_blank';
                    a.rel = 'noopener';
                    a.style.display = 'none';
                    document.body.appendChild(a);
                    a.click();
                    requestAnimationFrame(() => a.remove());
                    return null; // 새 탭으로 처리(핸들 없음)
                } catch (_) {
                    // 2) 최후: 현재 탭
                    window.location.href = url;
                    return null;
                }
            }

            return null; // 기본: 아무 것도 추가로 하지 않음(중복 방지)
        } finally {
            // 짧은 지연 후 열기 상태 해제(연속 클릭 레이스 방지)
            setTimeout(() => { _opening[name] = false; }, 200);
        }
    }

    /** ====== 전역 클릭 위임 ====== */
    document.addEventListener('click', (e) => {
        const enterBtn = e.target.closest('[data-popup="lecture"]');
        if (enterBtn) {
            e.preventDefault();
            e.stopPropagation();

            const courseId = enterBtn.getAttribute('data-course-id') || '';
            const popupKey = `lectureRoom-${courseId}`;

            // 연속 호출 방지(조금 넉넉히)
            if (_clickLock[popupKey]) return;
            _clickLock[popupKey] = true;
            setTimeout(() => { _clickLock[popupKey] = false; }, 1200);

            const url = `/myclass/lectureroom?courseId=${encodeURIComponent(courseId)}&popup=1`;
            const win = openCenteredPopup(url, popupKey, 1100, 800);

            // 핸들이 없더라도(일부 브라우저) 추가 경고/대체 동작은 하지 않음 → 중복 창 방지
            if (win) { try { win.focus(); } catch (_) {} }
            return;
        }

        const go = e.target.closest('[data-link]');
        if (go) {
            if (go.getAttribute('aria-disabled') === 'true' || go.hasAttribute('data-disabled') || go.hasAttribute('disabled')) {
                e.preventDefault();
                e.stopPropagation();
                alert('강의가 만료된 이후에 작성할 수 있습니다.');
                return;
            }
            const href = go.getAttribute('data-link');
            if (href) { e.preventDefault(); window.location.href = href; }
        }
    });

    // 필터/검색 이벤트
    filterSelect && filterSelect.addEventListener('change', applyAndRender);
    searchBtn    && searchBtn.addEventListener('click', applyAndRender);
    searchInput  && searchInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') applyAndRender(); });

    // 초기 로드
    function init() { loadFromEnrollments(); }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();
