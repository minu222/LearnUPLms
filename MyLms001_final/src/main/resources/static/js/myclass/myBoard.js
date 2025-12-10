// /static/js/myBoard.js
// 내 게시글: 목록 조회 / 선택 삭제 / 제목 클릭 수정
// - 백엔드 API: GET/PUT/DELETE /api/my/posts
// - 다른 페이지 스크립트 에러의 영향 최소화를 위해 IIFE + 안전 가드 사용

(function () {
    function init() {
        // ----- 필수 엘리먼트 조회 -----
        const tbody = document.getElementById('board-list');
        const selectAllCheckbox = document.getElementById('selectAll');
        const deleteSelectedBtn = document.getElementById('deleteSelectedBtn');
        const pagination = document.getElementById('pagination');

        const modal = document.getElementById('boardModal');
        const modalTitle = document.getElementById('modalTitle');
        const modalContent = document.getElementById('modalContent');
        const saveBtn = document.getElementById('saveModalBtn');
        const closeBtn = document.getElementById('closeModalBtn');

        // 이 페이지가 아니면 종료 (다른 화면에서 스크립트가 로드되더라도 안전)
        if (!tbody || !pagination) {
            console.debug('[myBoard] not this page - skip');
            return;
        }

        // ----- CSRF 헤더 (Spring Security 사용 시) -----
        const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
        const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

        const baseHeaders = { 'Accept': 'application/json', 'Content-Type': 'application/json' };
        if (CSRF_TOKEN && CSRF_HEADER) baseHeaders[CSRF_HEADER] = CSRF_TOKEN;

        // ----- 상태 -----
        let allPosts = [];   // 서버 원본
        let viewPosts = [];  // 필터/정렬 적용본
        let page = 1;
        const PAGE_SIZE = 10;

        // ----- 유틸 -----
        const escapeHtml = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
        const fmtDate = (iso) => !iso ? '' : (iso.length >= 10 ? iso.slice(0,10) : iso);
        const categoryToType = (cat) => (String(cat || '').toLowerCase() === 'qna' ? 'inquiry' : 'free');
        const typeToLabel = (t) => t === 'inquiry' ? '문의게시판' : '자유게시판';

        function rowHtml(idx, p) {
            const type = categoryToType(p.category);
            const createdAt = p.createdAt || '';
            return `
        <tr data-id="${p.id}" data-type="${type}" data-created="${createdAt}">
          <td><input type="checkbox" class="row-checkbox"></td>
          <td>${idx}</td>
          <td class="board-title-link">${escapeHtml(p.title || '(제목없음)')}</td>
          <td>${fmtDate(createdAt)}</td>
          <td>${typeToLabel(type)}</td>
        </tr>
      `;
        }

        function render() {
            if (!Array.isArray(viewPosts)) viewPosts = [];
            const totalPages = Math.max(1, Math.ceil(viewPosts.length / PAGE_SIZE));
            if (page > totalPages) page = totalPages;

            const start = (page - 1) * PAGE_SIZE;
            const slice = viewPosts.slice(start, start + PAGE_SIZE);

            if (slice.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="padding:16px;color:#999;">내가 작성한 게시글이 없습니다.</td></tr>`;
            } else {
                tbody.innerHTML = slice.map((p, i) => rowHtml(start + i + 1, p)).join('');
            }

            if (selectAllCheckbox) selectAllCheckbox.checked = false;

            // 페이지네이션
            pagination.innerHTML = Array.from({ length: totalPages }, (_, i) => {
                const active = (i + 1) === page ? 'class="active"' : '';
                return `<button ${active} data-page="${i + 1}">${i + 1}</button>`;
            }).join('');
        }

        function applyCurrentFilterAndSort(forceFilter) {
            const activeBtn = document.querySelector('.filter-btn.active');
            const mode = forceFilter || activeBtn?.dataset.filter || 'all';

            // 1) 기본: 서버 결과 기반
            let base = [...allPosts];

            // 2) 유형 필터
            if (mode === 'inquiry' || mode === 'free') {
                base = base.filter(p => categoryToType(p.category) === mode);
            }

            // 3) 정렬
            if (mode === 'newest') {
                base.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')));
            } else if (mode === 'oldest') {
                base.sort((a, b) => String(a.createdAt || '').localeCompare(String(b.createdAt || '')));
            }

            viewPosts = base;
        }

        async function load() {
            tbody.innerHTML = `<tr><td colspan="5" style="padding:16px;color:#666;">불러오는 중...</td></tr>`;
            try {
                const res = await fetch('/api/my/posts', { headers: { 'Accept': 'application/json' }});
                const raw = await res.text();
                console.log('[myBoard] status', res.status, 'len', raw.length, 'sample:', raw.slice(0, 120));

                if (res.status === 401) {
                    alert('로그인이 필요합니다.');
                    location.href = '/user/login?redirect=/myclass/myBoard';
                    return;
                }

                let data = [];
                try {
                    data = raw ? JSON.parse(raw) : [];
                } catch (e) {
                    console.error('[myBoard] JSON parse failed. Raw response starts with:', raw.slice(0, 300));
                    throw e;
                }

                allPosts = Array.isArray(data) ? data : [];
                applyCurrentFilterAndSort('all');
                page = 1;
                render();
            } catch (e) {
                console.error('[myBoard] load() 실패', e);
                tbody.innerHTML = `<tr><td colspan="5" style="padding:16px;color:#b00020;">목록을 불러오지 못했습니다.</td></tr>`;
            }
        }

        // ----- 전체 선택 -----
        selectAllCheckbox?.addEventListener('change', () => {
            const visibleRows = Array.from(tbody.querySelectorAll('tr')).filter(r => r.style.display !== 'none');
            visibleRows.forEach(r => {
                const cb = r.querySelector('.row-checkbox');
                if (cb) cb.checked = selectAllCheckbox.checked;
            });
        });

        // ----- 선택 삭제 -----
        deleteSelectedBtn?.addEventListener('click', async () => {
            const visibleRows = Array.from(tbody.querySelectorAll('tr')).filter(r => r.style.display !== 'none');
            const targets = visibleRows.filter(r => r.querySelector('.row-checkbox')?.checked);
            if (targets.length === 0) return alert('삭제할 게시글을 선택해주세요.');
            if (!confirm(`${targets.length}개의 게시글을 삭제하시겠습니까?`)) return;

            try {
                for (const tr of targets) {
                    const id = tr.dataset.id;
                    const res = await fetch(`/api/my/posts/${id}`, { method: 'DELETE', headers: baseHeaders });
                    if (!res.ok) {
                        console.error('[myBoard] delete fail id=', id, 'status=', res.status);
                        alert(`ID ${id} 삭제 실패`);
                        return;
                    }
                    // 메모리 제거
                    allPosts = allPosts.filter(p => String(p.id) !== String(id));
                }
                applyCurrentFilterAndSort(); // 현재 필터 유지
                render();
                alert('삭제 완료');
            } catch (e) {
                console.error('[myBoard] deleteSelected error', e);
                alert('삭제 중 오류가 발생했습니다.');
            }
        });

        // ----- 필터/정렬/페이지/제목 클릭(수정) -----
        document.body.addEventListener('click', async (e) => {
            // 필터 버튼
            if (e.target.classList.contains('filter-btn')) {
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');
                applyCurrentFilterAndSort(e.target.dataset.filter);
                page = 1;
                render();
            }

            // 페이지네이션
            if (e.target.parentElement && e.target.parentElement.id === 'pagination') {
                const p = Number(e.target.dataset.page);
                if (!isNaN(p)) { page = p; render(); }
            }

            // 제목 클릭 → 수정 모달
            if (e.target.classList.contains('board-title-link')) {
                const id = e.target.closest('tr')?.dataset.id;
                if (id) openEditModal(id);
            }
        });

        async function openEditModal(id) {
            if (!modal || !modalTitle || !modalContent || !saveBtn || !closeBtn) return;

            try {
                const res = await fetch(`/api/my/posts/${id}`, { headers: { 'Accept': 'application/json' }});
                if (res.status === 403) return alert('본인 글만 수정할 수 있습니다.');
                if (!res.ok) throw new Error('조회 실패');
                const p = await res.json();

                modalTitle.textContent = p.title || '(제목없음)';
                modalContent.value = p.content || '';
                modal.style.display = 'flex';

                saveBtn.onclick = async () => {
                    const payload = JSON.stringify({ title: modalTitle.textContent, content: modalContent.value });
                    const u = await fetch(`/api/my/posts/${id}`, { method: 'PUT', headers: baseHeaders, body: payload });
                    if (!u.ok) {
                        console.error('[myBoard] update fail id=', id, 'status=', u.status);
                        alert('수정 실패');
                        return;
                    }
                    const idx = allPosts.findIndex(x => String(x.id) === String(id));
                    if (idx >= 0) allPosts[idx].content = modalContent.value;
                    applyCurrentFilterAndSort();
                    render();
                    modal.style.display = 'none';
                    alert('수정 완료');
                };

                const close = () => { modal.style.display = 'none'; };
                closeBtn.onclick = close;
                // 오버레이 클릭 닫기
                modal.addEventListener('click', (ev) => { if (ev.target === modal) close(); }, { once:true });

            } catch (e) {
                console.error('[myBoard] openEditModal error', e);
                alert('게시글을 불러오지 못했습니다.');
            }
        }

        // 시작
        load();
    }

    // DOMContentLoaded 여부와 상관없이 실행 보장
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
