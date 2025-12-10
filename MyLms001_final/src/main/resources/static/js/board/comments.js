/* =========================================================
   /js/board/comments.js  (통합 완성본)
   - 기존 기능 유지: 목록, 등록, 수정, 삭제, 대댓글
   - 추가: 최신→오래된 정렬, 페이지네이션(10개), 50자 제한,
           Enter 등록(Shift+Enter 줄바꿈), 입력 글자수 카운터,
           툴바(힌트+카운터) 자동 주입
========================================================= */
(function () {
    'use strict';

    /** ====== DOM ====== */
    const listEl    = document.getElementById('comment-list');
    const inputEl   = document.getElementById('comment-input');
    const submitBtn = document.getElementById('comment-submit');
    const boxEl     = document.getElementById('comments');

    // postId: window.__POST_ID__ 우선, 없으면 data-post-id
    let postId = Number(window.__POST_ID__ ?? 0);
    if ((!postId || Number.isNaN(postId)) && boxEl && boxEl.dataset.postId) {
        postId = Number(boxEl.dataset.postId);
    }

    if (!listEl || !submitBtn || !postId) {
        console.warn('comments.js: postId가 없거나 DOM 준비가 안 됨.', { postId });
        return;
    }

    /** ====== 설정 ====== */
    const PAGE_SIZE = 10;
    const MAX_LEN   = 50;

    /** ====== 상태 ====== */
    let allRows   = []; // 서버 원본(루트+자식)
    let flatRows  = []; // 화면 표시용 플랫(루트 기준 정렬 후 자식 이어붙임)
    let page      = 1;
    let pageCount = 1;

    /** ====== 유틸 ====== */
    const h = (s) => {
        const d = document.createElement('div');
        d.innerText = s ?? '';
        return d.innerHTML;
    };
    const dateText = (s) => {
        if (!s) return '';
        return String(s).replace('T', ' ').slice(0, 16);
    };
    const clampLen = (s) => (s.length > MAX_LEN ? s.slice(0, MAX_LEN) : s);
    const byNewToOld = (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    const byOldToNew = (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();

    /** ====== 입력 툴바 주입 (힌트+카운터) ====== */
    let counterEl = null;
    (function injectToolbar() {
        // #comment-form 또는 .fd-comment-form 내부에 삽입
        const form = document.getElementById('comment-form') || document.querySelector('.fd-comment-form');
        if (!form || form.querySelector('.fd-comment-toolbar')) return;

        const toolbar = document.createElement('div');
        toolbar.className = 'fd-comment-toolbar';
        toolbar.innerHTML = `
      <div class="fd-hint">Enter로 등록 • Shift+Enter 줄바꿈 • 최대 ${MAX_LEN}자</div>
      <div class="fd-counter"><span id="fdCount">0</span> / ${MAX_LEN}</div>
    `;

        // "댓글" 제목(h3)이 있으면 그 뒤에, 없으면 맨 앞에
        const title = form.querySelector('.fd-card-title, h3');
        if (title && title.nextSibling) title.parentNode.insertBefore(toolbar, title.nextSibling);
        else form.prepend(toolbar);

        counterEl = document.getElementById('fdCount');
    })();

    /** ====== 입력 제어 (50자 / Enter 제출) ====== */
    inputEl?.addEventListener('input', () => {
        const before = inputEl.value;
        const after  = clampLen(before);
        if (before !== after) {
            const pos = inputEl.selectionStart;
            inputEl.value = after;
            const np = Math.min(pos, MAX_LEN);
            inputEl.setSelectionRange(np, np);
        }
        if (counterEl) counterEl.textContent = String(inputEl.value.length);
    });

    // Enter로 등록 (Shift+Enter는 줄바꿈)
    inputEl?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submitBtn?.click();
        }
    });

    /** ====== 서버 I/O ====== */
    async function load() {
        if (!postId) return;
        listEl.innerHTML = '<div style="color:#666;padding:8px 0;">불러오는 중...</div>';
        try {
            const res = await fetch(`/api/comments?postId=${postId}`, {
                headers: { 'Accept': 'application/json' }
            });
            if (!res.ok) throw new Error('목록 조회 실패');
            const rows = await res.json();
            allRows = Array.isArray(rows) ? rows : [];
            rebuildFlat();
            render();
        } catch (e) {
            console.error(e);
            listEl.innerHTML = '<div style="color:#c00;">댓글을 불러오지 못했습니다.</div>';
            renderPaging(); // 에러여도 기존 페이징 제거
        }
    }

    async function postComment(body) {
        const res = await fetch('/api/comments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify(body)
        });
        if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/user/login'; return; }
        if (!res.ok) throw new Error('등록 실패');
        return res.json().catch(() => null);
    }

    async function patchComment(id, content) {
        const res = await fetch(`/api/comments/${id}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content })
        });
        if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/user/login'; return; }
        if (!res.ok) throw new Error('수정 실패');
    }

    async function deleteComment(id) {
        const res = await fetch(`/api/comments/${id}`, { method: 'DELETE' });
        if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/user/login'; return; }
        if (!res.ok) throw new Error('삭제 실패');
    }

    /** ====== 표시용 배열(플랫) 재구성 ======
     *  - 요구사항: "최근 → 오래된순"
     *  - 루트(부모) 댓글을 최신순으로 정렬하고,
     *    각 루트의 자식은 대화 흐름을 위해 오래된→최신으로 이어붙임.
     *    (루트 기준으로는 전체가 최신순으로 보인다)
     */
    function rebuildFlat() {
        const roots    = allRows.filter(r => r.parentCommentId == null).sort(byNewToOld);
        const children = allRows.filter(r => r.parentCommentId != null);

        flatRows = [];
        for (const root of roots) {
            flatRows.push({ ...root, __depth: 0 });
            const ch = children.filter(x => x.parentCommentId === root.id).sort(byOldToNew);
            for (const c of ch) flatRows.push({ ...c, __depth: 1 });
        }

        pageCount = Math.max(1, Math.ceil(flatRows.length / PAGE_SIZE));
        if (page > pageCount) page = pageCount;
    }

    /** ====== 렌더링 ====== */
    function render() {
        // 페이지 슬라이스
        const start = (page - 1) * PAGE_SIZE;
        const slice = flatRows.slice(start, start + PAGE_SIZE);

        const ul = document.createElement('ul');
        ul.style.listStyle = 'none';
        ul.style.padding = '0';
        ul.innerHTML = slice.map(itemToHTML).join('');

        // 이벤트 위임: 수정/삭제/답글
        ul.addEventListener('click', async (ev) => {
            const t = ev.target;
            const id = Number(t?.closest('[data-id]')?.dataset?.id || 0);
            if (!id) return;

            if (t.dataset.edit) {
                const target = allRows.find(r => r.id === id);
                const newText = prompt('내용 수정', target?.content ?? '');
                if (newText == null) return;
                try {
                    await patchComment(id, clampLen(newText.trim()));
                    await load(); page = 1; render();
                } catch { alert('수정에 실패했습니다.'); }
            }

            if (t.dataset.del) {
                if (!confirm('삭제하시겠습니까?')) return;
                try {
                    await deleteComment(id);
                    await load(); render();
                } catch { alert('삭제에 실패했습니다.'); }
            }

            if (t.dataset.reply) {
                const text = prompt('답글 입력');
                if (!text) return;
                try {
                    await postComment({
                        postId,
                        content: clampLen(text.trim()),
                        parentCommentId: id
                    });
                    await load(); page = 1; render();
                } catch { alert('답글 등록에 실패했습니다.'); }
            }
        });

        // 교체
        listEl.innerHTML = '';
        listEl.classList.add('fd-comment-list'); // 스타일 보장
        listEl.appendChild(ul);

        renderPaging();
    }

    function itemToHTML(r) {
        const indent = r.__depth === 1 ? 'margin-left:24px;' : '';
        return `
      <li data-id="${r.id}" style="padding:10px 0; border-top:1px solid #eee; ${indent}">
        <div style="display:flex;gap:8px;align-items:center;">
          <strong>${h(r.authorName ?? '익명')}</strong>
          <span style="color:#888;font-size:12px;">${dateText(r.createdAt)}</span>
        </div>
        <div style="margin:6px 0 8px 0;">${h(r.content ?? '')}</div>
       
      </li>
    `;
    }

    /** ====== 페이징 ====== */
    function renderPaging() {
        // 기존 페이징 제거
        const old = listEl.parentElement.querySelector('.fd-paging');
        if (old) old.remove();

        if (pageCount <= 1) return;

        const wrap = document.createElement('div');
        wrap.className = 'fd-paging';

        const prev = document.createElement('button');
        prev.className = 'fd-page-btn';
        prev.textContent = '〈';
        prev.disabled = page === 1;
        prev.onclick = () => { page = Math.max(1, page - 1); render(); };
        wrap.appendChild(prev);

        for (let i = 1; i <= pageCount; i++) {
            const b = document.createElement('button');
            b.className = 'fd-page-btn' + (i === page ? ' active' : '');
            b.textContent = String(i);
            b.onclick = () => { page = i; render(); };
            wrap.appendChild(b);
        }

        const next = document.createElement('button');
        next.className = 'fd-page-btn';
        next.textContent = '〉';
        next.disabled = page === pageCount;
        next.onclick = () => { page = Math.min(pageCount, page + 1); render(); };
        wrap.appendChild(next);

        listEl.parentElement.appendChild(wrap);
    }

    /** ====== 등록 버튼 ====== */
    submitBtn.addEventListener('click', async function () {
        const raw = (inputEl.value || '').trim();
        if (!raw) { alert('내용을 입력하세요.'); return; }

        try {
            await postComment({ postId, content: clampLen(raw) });
            inputEl.value = '';
            if (counterEl) counterEl.textContent = '0';
            await load();
            page = 1; // 최신 페이지
            render();
        } catch (e) {
            console.error(e);
            alert('댓글 등록에 실패했습니다.');
        }
    });

    /** ====== 최초 로드 ====== */
    load();
})();
