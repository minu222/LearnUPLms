// /js/myMessage.js
(function () {
    'use strict';

    // --- DOM ---
    const root     = document.getElementById('myMessagePage');
    const tbody    = document.getElementById('messageTbody');
    const tabInbox = document.getElementById('tabInbox');
    const tabSent  = document.getElementById('tabSent');
    const checkAll = document.getElementById('checkAll');
    const btnDel   = document.getElementById('msgBtnDeleteSelected'); // ← 고유 ID
    const btnEmail = document.getElementById('openEmailModal');
    const btnNote  = document.getElementById('openNoteModal');
    const search   = document.getElementById('searchInput');
    const btnFind  = document.getElementById('searchBtn');

    // --- 상태 ---
    let currentBox = 'inbox'; // 'inbox' | 'sent'
    let cache = [];

    // --- 유틸 ---
    const escapeHtml = s => (s || '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
    const roleLabel  = r => ({ admin:'관리자', instructor:'강사', student:'학생' }[(r||'').toLowerCase()] || (r||''));
    const fmt        = v => { const d = new Date(v); return isNaN(d) ? (v || '') : d.toLocaleString(); };
    const personCell = (nick,name,role) =>
        `<div class="person">
       <div class="nick" style="font-weight:600">${escapeHtml(nick || '')}</div>
       <div class="meta" style="font-size:12px;color:#666">${escapeHtml(name || '')}${name && role ? ' · ' : ''}${escapeHtml(roleLabel(role))}</div>
     </div>`;

    function extractTitle(content = '') {
        let t = content.replace(/^\s*\[(?:이메일|쪽지)\]\s*/i, '');
        const firstLine = t.split(/\r?\n/).find(line => line.trim().length > 0) || '';
        return firstLine.trim().slice(0, 120) || '(제목 없음)';
    }
    function extractBody(content = '') {
        let t = content.replace(/^\s*\[(?:이메일|쪽지)\]\s*/i, '');
        const lines = t.split(/\r?\n/);
        if (lines.length <= 1) return '';
        let usedTitle = false;
        const bodyLines = [];
        for (const line of lines) {
            if (!usedTitle && line.trim().length > 0) { usedTitle = true; continue; }
            bodyLines.push(line);
        }
        return bodyLines.join('\n').trim();
    }

    // --- 렌더 ---
    function render(rows) {
        if (!Array.isArray(rows) || rows.length === 0) {
            tbody.innerHTML = `<tr class="meta-row"><td colspan="6" style="padding:16px;color:#888;">데이터가 없습니다.</td></tr>`;
            return;
        }
        tbody.innerHTML = rows.map(m => {
            const title = escapeHtml(m.title || extractTitle(m.content));
            const readBtn = m.read
                ? `<button class="msg-btn-read" type="button" disabled>읽음</button>`
                : `<button class="msg-btn-read" type="button">읽음처리</button>`;
            return `
        <tr data-id="${m.id}" ${m.read ? '' : 'style="font-weight:600;background:#f7fffa"'}>
          <td><input type="checkbox" class="row-check"></td>
          <td><button class="title-link" data-id="${m.id}" type="button">${title}</button></td>
          <td>${personCell(m.senderNickname, m.senderName, m.senderRole)}</td>
          <td>${personCell(m.receiverNickname, m.receiverName, m.receiverRole)}</td>
          <td>${escapeHtml(fmt(m.sentAt))}</td>
          <td>
            ${readBtn}
            <button class="msg-btn-delete" type="button">삭제</button>
          </td>
        </tr>
      `;
        }).join('');
    }

    // --- 상세 모달 (닫기 버튼만, 항상 보임) ---
    function openDetailModal(message) {
        const wrap = document.createElement('div');
        wrap.style.cssText = `
      position:fixed; inset:0; background:rgba(0,0,0,.45);
      display:flex; align-items:center; justify-content:center; z-index:10000;
    `;
        const title = escapeHtml(message.title || extractTitle(message.content));
        const body  = escapeHtml(extractBody(message.content));

        wrap.innerHTML = `
      <div class="msg-card" role="dialog" aria-modal="true" aria-labelledby="msg-title" aria-describedby="msg-body">
        <style>
          .msg-card{
            width:760px; max-width:92vw; max-height:88vh;
            background:#fff; border-radius:16px; box-shadow:0 24px 80px rgba(0,0,0,.25);
            display:flex; flex-direction:column; overflow:hidden; font-family:inherit;
          }
          .msg-hd{
            position:sticky; top:0; z-index:2;
            display:flex; align-items:center; gap:8px;
            padding:14px 18px; color:#222; font-weight:800;
            background:linear-gradient(90deg,#ffb400,#ffd36a);
            border-bottom:1px solid rgba(0,0,0,.06);
          }
          .msg-title{
            margin:0; font-size:18px; line-height:1.3;
            white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
          }
          .msg-content{ flex:1; overflow:auto; padding:16px 18px; }
          .msg-meta{ display:flex; flex-wrap:wrap; gap:8px; margin-bottom:12px; }
          .pill{ background:#fff7e6; border:1px solid #ffe0a3; color:#6b4b00;
                 padding:4px 10px; border-radius:999px; font-size:12px; }
          .msg-bd{ white-space:pre-wrap; line-height:1.7; color:#222; }
          .msg-ft{
            position:sticky; bottom:0; z-index:2;
            padding:12px 16px; display:flex; gap:8px; justify-content:flex-end; align-items:center;
            background:linear-gradient(to top, #ffffff 92%, rgba(255,255,255,0));
            border-top:1px solid #f1f1f1;
          }
          .msg-btn{
            opacity:1 !important; visibility:visible !important; filter:none !important;
            text-indent:0 !important; width:auto !important; overflow:visible !important; white-space:nowrap !important;
          }
          .msg-btn:hover{ filter:brightness(0.98); }
          .msg-btn.primary{ background:#ffb400; border-color:#ffb400; color:#222; }
        </style>

        <div class="msg-hd">
          <h2 class="msg-title" id="msg-title" title="${title}">${title}</h2>
        </div>

        <div class="msg-content">
          <div class="msg-meta">
            <span class="pill">보낸사람: ${escapeHtml(message.senderNickname || '')}</span>
            <span class="pill">받는사람: ${escapeHtml(message.receiverNickname || '')}</span>
            <span class="pill">${escapeHtml(fmt(message.sentAt))}</span>
            <span class="pill">${message.read ? '읽음' : '안읽음'}</span>
          </div>
          <div class="msg-bd" id="msg-body">${body || '<em style="color:#888">본문이 없습니다.</em>'}</div>
        </div>

        <div class="msg-ft">
          ${(!message.read && currentBox === 'inbox') ? '<button class="msg-btn primary" id="detail-markread" type="button">읽음처리</button>' : ''}
          <button class="msg-btn" id="detail-close" type="button">닫기</button>
        </div>
      </div>
    `;
        document.body.appendChild(wrap);

        const close = () => wrap.remove();
        // 오버레이/ESC로는 닫지 않고, 닫기 버튼만 사용
        const card  = wrap.querySelector('.msg-card');
        card.addEventListener('click', (e) => e.stopPropagation());
        wrap.addEventListener('click', (e) => { /* overlay click 무시 */ });
        wrap.querySelector('#detail-close').addEventListener('click', (e) => { e.preventDefault(); e.stopPropagation(); close(); });

        const btnMark = wrap.querySelector('#detail-markread');
        if (btnMark) {
            btnMark.addEventListener('click', (e) => {
                e.preventDefault(); e.stopPropagation();
                cache = cache.map(m => m.id === message.id ? { ...m, read: true } : m);
                render(cache);
                btnMark.remove();
            });
        }
    }

    // --- 로드 (백엔드 그대로 사용) ---
    async function load(box = currentBox) {
        currentBox = box;
        toggleTabs(box);
        checkAll.checked = false;
        tbody.innerHTML = `<tr class="meta-row"><td colspan="6" style="padding:16px;color:#666;">불러오는 중...</td></tr>`;
        try {
            const res = await fetch(`/api/messages/my?box=${encodeURIComponent(box)}`, { headers: { Accept: 'application/json' } });
            if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/user/login'; return; }
            if (!res.ok) throw new Error('목록 조회 실패');
            cache = await res.json();
            render(cache);
        } catch (e) {
            console.error(e);
            tbody.innerHTML = `<tr class="meta-row"><td colspan="6" style="padding:16px;color:#d00;">불러오기 실패</td></tr>`;
        }
    }

    function toggleTabs(box) {
        tabInbox?.classList.toggle('active', box === 'inbox');
        tabSent ?.classList.toggle('active', box === 'sent' );
    }

    // --- 읽음: 프런트에서 고정
    function markReadUI(id, btn, tr) {
        btn.textContent = '읽음';
        btn.disabled = true;
        tr.style.fontWeight = '';
        tr.style.background = '';
        cache = cache.map(m => m.id === id ? { ...m, read: true } : m);
        // fetch(`/api/messages/${id}/read`, { method: 'PATCH' }).catch(()=>{});
    }

    // --- 삭제: UI에서만 제거
    function deleteUI(id) {
        cache = cache.filter(m => m.id !== id);
        render(cache);
        // fetch(`/api/messages/${id}?box=${currentBox}`, { method:'DELETE' }).catch(()=>{});
    }

    function deleteSelectedUI() {
        const ids = Array.from(tbody.querySelectorAll('.row-check:checked'))
            .map(ch => Number(ch.closest('tr').dataset.id));
        if (ids.length === 0) { alert('삭제할 항목을 선택하세요.'); return; }
        cache = cache.filter(m => !ids.includes(m.id));
        render(cache);
    }

    function applyFilter() {
        const kw = (search.value || '').toLowerCase();
        const filtered = cache.filter(m => {
            const title = (m.title || extractTitle(m.content)).toLowerCase();
            const pool = [
                title,
                m.content || '',
                m.senderNickname || '',
                m.receiverNickname || '',
                m.senderName || '',
                m.receiverName || '',
                roleLabel(m.senderRole || ''),
                roleLabel(m.receiverRole || '')
            ].join(' ').toLowerCase();
            return pool.includes(kw);
        });
        render(filtered);
    }

    // --- 이벤트 ---
    tabInbox?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); load('inbox'); });
    tabSent ?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); load('sent');  });

    checkAll?.addEventListener('change', e => {
        e.stopPropagation();
        tbody.querySelectorAll('.row-check').forEach(ch => ch.checked = e.target.checked);
    });

    btnDel ?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); deleteSelectedUI(); });
    btnFind?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); applyFilter(); });
    search ?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); e.stopPropagation(); applyFilter(); } });

    // 테이블 내부 위임 (전역 핸들러 차단)
    tbody.addEventListener('click', (e) => {
        const btn = e.target.closest('button, .title-link');
        if (!btn) return;
        e.preventDefault(); e.stopPropagation();

        const tr = e.target.closest('tr[data-id]');
        if (!tr) return;
        const id = Number(tr.dataset.id);

        if (btn.classList.contains('title-link')) {
            const msg = cache.find(m => m.id === id);
            if (msg) openDetailModal(msg);
            return;
        }
        if (btn.classList.contains('msg-btn-delete')) {
            deleteUI(id);
            return;
        }
        if (btn.classList.contains('msg-btn-read') && !btn.disabled) {
            markReadUI(id, btn, tr);
            return;
        }
    });

    // 모달 버튼(외부 스크립트 있으면 그걸 사용)
    btnEmail?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); (window.openEmailModal||alert)('EmailModal.js가 없습니다.'); });
    btnNote ?.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); (window.openNoteModal ||alert)('NoteModal.js가 없습니다.'); });

    // 초기 로드
    load('inbox');
})();
