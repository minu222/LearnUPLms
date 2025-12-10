// /js/NoteModal.js
(function () {
    'use strict';

    function openModal() {
        const wrap = document.createElement('div');
        wrap.id = 'note-modal';
        wrap.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;z-index:10000;';
        wrap.innerHTML = `
      <div class="nt-card" role="dialog" aria-modal="true" aria-labelledby="nt-title">
        <style>
          .nt-card{width:560px;max-width:92vw;background:#fff;border-radius:14px;box-shadow:0 20px 60px rgba(0,0,0,.25);overflow:hidden;font-family:inherit}
          .nt-hd{padding:14px 18px;background:linear-gradient(90deg,#ffb400,#ffd36a);color:#222;font-weight:800}
          .nt-bd{padding:18px;display:grid;gap:12px}
          .nt-ft{padding:12px 18px;display:flex;gap:10px;justify-content:flex-end;border-top:1px solid #f0f0f0}
          .nt-label{font-size:12px;color:#555;margin-bottom:6px}
          .nt-hint{font-size:12px;color:#888;margin-top:4px}
          .nt-input,.nt-textarea{width:100%;padding:10px 12px;border:1px solid #e5e5e5;border-radius:10px;outline:none;transition:box-shadow .15s,border-color .15s}
          .nt-input:focus,.nt-textarea:focus{border-color:#ffb400;box-shadow:0 0 0 3px rgba(255,180,0,.18)}
          .nt-textarea{min-height:160px;resize:vertical}
          .nt-row{display:flex;flex-direction:column}
          .nt-btn{padding:9px 14px;border-radius:10px;border:1px solid transparent;background:#ffb400;color:#222;font-weight:700;cursor:pointer}
          .nt-btn:hover{filter:brightness(0.98)}
          .nt-btn-secondary{background:#fff;border-color:#e5e5e5}
        </style>

        <div class="nt-hd" id="nt-title">✉️ 쪽지 보내기</div>

        <div class="nt-bd">
          <div class="nt-row">
            <label class="nt-label" for="note-receiver-nick">받는사람 아이디(닉네임) 입력</label>
            <input id="note-receiver-nick" class="nt-input" type="text" placeholder="예: tiger, admin, luna ..." autocomplete="off">
            <div class="nt-hint">* 사용자 아이디 또는 닉네임을 입력해 주세요.</div>
          </div>

          <div class="nt-row">
            <label class="nt-label" for="note-title">제목</label>
            <input id="note-title" class="nt-input" type="text" placeholder="제목을 입력하세요">
          </div>

          <div class="nt-row">
            <label class="nt-label" for="note-body">내용</label>
            <textarea id="note-body" class="nt-textarea" placeholder="보낼 내용을 입력하세요. (Ctrl/Cmd + Enter 전송)"></textarea>
          </div>
        </div>

        <div class="nt-ft">
          <button id="note-cancel" class="nt-btn nt-btn-secondary">취소</button>
          <button id="note-send" class="nt-btn">보내기</button>
        </div>
      </div>`;

        document.body.appendChild(wrap);

        const card = wrap.querySelector('.nt-card');
        const $ = sel => wrap.querySelector(sel);
        const inputNick  = $('#note-receiver-nick');
        const inputTitle = $('#note-title');
        const inputBody  = $('#note-body');
        const btnSend = $('#note-send');
        const btnCancel = $('#note-cancel');

        // 닫기(오버레이, ESC, 취소 버튼)
        const close = () => wrap.remove();
        wrap.addEventListener('click', (e) => { if (e.target === wrap) close(); });
        card.addEventListener('click', (e) => e.stopPropagation());
        btnCancel.addEventListener('click', close);
        function escClose(e){ if (e.key === 'Escape') close(); }
        document.addEventListener('keydown', escClose);

        // 전송
        async function doSend() {
            const receiverNickname = (inputNick.value  || '').trim();
            const title            = (inputTitle.value || '').trim();
            const body             = (inputBody.value  || '').trim();

            [inputNick, inputTitle, inputBody].forEach(el => el.style.borderColor = '#e5e5e5');
            if (!receiverNickname) { inputNick.style.borderColor  = '#ff5a5a'; inputNick.focus();  return; }
            if (!title)            { inputTitle.style.borderColor = '#ff5a5a'; inputTitle.focus(); return; }
            if (!body)             { inputBody.style.borderColor  = '#ff5a5a'; inputBody.focus();  return; }

            const content = `[쪽지] ${title}\n${body}`;

            btnSend.disabled = true; btnSend.textContent = '전송중...';
            try {
                const res = await fetch('/api/messages', {
                    method: 'POST',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({ receiverNickname, content })
                });
                if (!res.ok) throw 0;
                alert('전송 완료');
                close();
            } catch {
                alert('전송 실패');
            } finally {
                btnSend.disabled = false; btnSend.textContent = '보내기';
                document.removeEventListener('keydown', escClose);
            }
        }

        btnSend.addEventListener('click', doSend);
        inputBody.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') doSend();
        });

        setTimeout(() => inputNick.focus(), 50);
    }

    window.openNoteModal = openModal;
})();
