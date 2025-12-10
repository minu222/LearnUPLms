// /static/js/paymentModal.js
// 로그인 상태: 모달 오픈 → "결제하기" 누르면 /api/orders/pay 호출
(function () {
    // ✅ 중복 초기화 가드
    if (window.__paymentModalInit__) return;
    window.__paymentModalInit__ = true;

    let paying = false;

    /* ----------------------- 유틸 ----------------------- */
    const onlyDigits = s => String(s || '').replace(/\D+/g, '');
    const clamp = (n, min, max) => Math.max(min, Math.min(max, n));

    function toast(msg) {
        try {
            const t = document.createElement('div');
            t.textContent = msg;
            t.style.cssText =
                'position:fixed;left:50%;top:70px;transform:translateX(-50%);' +
                'background:#333;color:#fff;padding:10px 14px;border-radius:8px;z-index:2147483647;opacity:.95';
            document.body.appendChild(t);
            setTimeout(() => t.remove(), 1600);
        } catch { alert(msg); }
    }

    function openModal(title, price, courseId) {
        const modal = document.getElementById('paymentModal');
        if (!modal) return;
        modal.style.display = 'flex';

        // 표기 & 데이터 보관
        const nameEl = document.getElementById('modalCourseName');
        const nameDetailEl = document.getElementById('modalCourseNameDetail');
        if (nameEl) nameEl.textContent = `결제 - ${title}`;
        if (nameDetailEl) nameDetailEl.textContent = title;

        modal.dataset.courseId = String(courseId || '');
        modal.dataset.title = title || '';
        modal.dataset.price = String(price ?? 0);

        // 입력 초기화
        const ids = ['paymentMethod','cardNumber','cardExpiry','cardCvc','bankName','bankAccount'];
        ids.forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
        document.querySelectorAll('.payment-form').forEach(f => f.style.display = 'none');
        // 은행 계좌 비활성화
        const bankAccount = document.getElementById('bankAccount');
        if (bankAccount) { bankAccount.disabled = true; }
    }

    function closeModal() {
        const modal = document.getElementById('paymentModal');
        if (!modal) return;
        modal.style.display = 'none';
        const paymentMethod = document.getElementById('paymentMethod');
        if (paymentMethod) paymentMethod.value = '';
        document.querySelectorAll('.payment-form').forEach(f => f.style.display = 'none');
    }

    // 외부에서 호출 (카드의 "구매하기"가 이 함수를 부름)
    window.goToPayment = async (title, price, _qty, _img, _inst, _desc, courseId) => {
        // 로그인 체크: 401 테스트를 위해 가벼운 핑
        const test = await fetch('/api/enrollments/my', { headers: { 'Accept':'application/json' }});
        if (test.status === 401) { toast('로그인이 필요합니다. 로그인 페이지로 이동합니다.'); location.href = '/user/login'; return; }
        openModal(title, price, courseId);
    };

    // 결제수단 폼 선택
    window.showPaymentForm = function () {
        document.querySelectorAll('.payment-form').forEach(form => form.style.display = 'none');
        const method = document.getElementById('paymentMethod')?.value;
        if (method) document.getElementById(method + 'Form')?.style?.setProperty('display','block');

        // 은행 선택시 계좌입력 활성/비활성
        const bankName = document.getElementById('bankName');
        const bankAccount = document.getElementById('bankAccount');
        if (bankName && bankAccount) {
            if (method === 'bank') {
                bankAccount.disabled = (bankName.value || '') === '';
            } else {
                bankAccount.disabled = true;
            }
        }
    };

    window.closePaymentModal = closeModal;

    /* ----------------- 입력 마스킹(자동 하이픈/슬래시) ----------------- */
    function setCaretToEnd(el) {
        try { el.selectionStart = el.selectionEnd = el.value.length; } catch {}
    }

    function maskCardNumberInput(e) {
        const el = e.target;
        let d = onlyDigits(el.value).slice(0, 16); // 최대 16자리
        // 4-4-4-4
        const parts = [];
        for (let i = 0; i < d.length; i += 4) parts.push(d.slice(i, i + 4));
        el.value = parts.join('-');
        setCaretToEnd(el);
    }

    function maskExpiryInput(e) {
        const el = e.target;
        let d = onlyDigits(el.value).slice(0, 4); // MMYY
        let mm = d.slice(0, 2);
        let yy = d.slice(2, 4);
        if (mm.length === 2) {
            let n = parseInt(mm, 10);
            if (isNaN(n)) n = 1;
            n = clamp(n, 1, 12);
            mm = String(n).padStart(2, '0');
        }
        el.value = yy ? `${mm}/${yy}` : (mm.length ? mm : '');
        if (el.value.length === 2 && d.length > 2) el.value += '/'; // 자연스런 삽입
        setCaretToEnd(el);
    }

    function maskCvcInput(e) {
        const el = e.target;
        el.value = onlyDigits(el.value).slice(0, 3);
        setCaretToEnd(el);
    }

    // 은행 계좌: 기본 포맷 3-4-4 (총 11자리, 하이픈 2개 포함하면 13자)
    function maskBankAccountInput(e) {
        const el = e.target;
        let d = onlyDigits(el.value).slice(0, 11); // 11 digits
        let out = '';
        if (d.length <= 3) out = d;
        else if (d.length <= 7) out = d.slice(0,3) + '-' + d.slice(3);
        else out = d.slice(0,3) + '-' + d.slice(3,7) + '-' + d.slice(7);
        el.value = out;
        setCaretToEnd(el);
    }

    function initMasksOnce() {
        if (window.__paymentMaskBound__) return;
        window.__paymentMaskBound__ = true;

        const cardNumber = document.getElementById('cardNumber');
        const cardExpiry = document.getElementById('cardExpiry');
        const cardCvc    = document.getElementById('cardCvc');
        const bankName   = document.getElementById('bankName');
        const bankAccount= document.getElementById('bankAccount');

        if (cardNumber) cardNumber.addEventListener('input', maskCardNumberInput);
        if (cardExpiry) cardExpiry.addEventListener('input', maskExpiryInput);
        if (cardCvc)    cardCvc.addEventListener('input', maskCvcInput);
        if (bankAccount) bankAccount.addEventListener('input', maskBankAccountInput);
        if (bankName && bankAccount) {
            bankName.addEventListener('change', () => {
                const methodSel = document.getElementById('paymentMethod');
                const isBank = methodSel && methodSel.value === 'bank';
                bankAccount.disabled = !isBank || (bankName.value || '') === '';
                if (!bankAccount.disabled) bankAccount.focus();
            });
        }
    }

    // DOM 준비 시 마스킹 바인딩
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMasksOnce);
    } else {
        initMasksOnce();
    }

    /* ----------------------- 결제 요청 ----------------------- */
    // 모달 내 "결제하기" 버튼이 이 함수 호출
    window.confirmPayment = async function () {
        const modal = document.getElementById('paymentModal');
        if (!modal) return;

        const agree1 = document.getElementById('agreeTerms1')?.checked;
        const agree2 = document.getElementById('agreeTerms2')?.checked;
        if (!agree1 || !agree2) { toast('약관에 모두 동의해야 결제할 수 있습니다.'); return; }

        const courseId = Number(modal.dataset.courseId || 0);
        const method = document.getElementById('paymentMethod')?.value || 'card';
        if (!courseId) { toast('유효하지 않은 강의입니다.'); return; }

        // 클라이언트 측 간단 검증
        if (method === 'card') {
            const cardNumber = document.getElementById('cardNumber')?.value || '';
            const cardExpiry = document.getElementById('cardExpiry')?.value || '';
            const cardCvc    = document.getElementById('cardCvc')?.value || '';
            if (!/^\d{4}-\d{4}-\d{4}-\d{4}$/.test(cardNumber)) { toast('카드번호를 확인해 주세요.'); return; }
            if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(cardExpiry))   { toast('유효기간을 확인해 주세요.'); return; }
            if (!/^\d{3}$/.test(cardCvc))                      { toast('CVC 3자리를 입력해 주세요.'); return; }
        } else if (method === 'bank') {
            const bankName = document.getElementById('bankName')?.value || '';
            const bankAccount = document.getElementById('bankAccount')?.value || '';
            if (!bankName) { toast('은행을 선택해 주세요.'); return; }
            if (!/^\d{3}-\d{4}-\d{4}$/.test(bankAccount)) { toast('계좌번호를 확인해 주세요.'); return; }
        }

        if (paying) return;
        paying = true;
        try {
            const res = await fetch('/api/orders/pay', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({ courseId, paymentMethod: method })
            });

            if (res.status === 401) { toast('로그인이 필요합니다.'); location.href = '/user/login'; return; }
            if (res.status === 409) { // 이미 수강/결제
                const j = await res.json().catch(()=>null);
                toast((j && j.message) || '이미 수강 중인 강의입니다.');
                return;
            }
            if (!res.ok) {
                const txt = await res.text().catch(()=> '');
                throw new Error(txt || '결제 실패');
            }

            const data = await res.json().catch(()=>null);
            if (!data || data.ok === false) throw new Error((data && data.message) || '결제 실패');

            toast('결제가 완료되었습니다. 나의 강의실에서 수강을 시작하세요!');
            closeModal();

            // 화면 갱신 이벤트 전파(수강목록/결제내역이 듣고 있으면 새로고침)
            document.dispatchEvent(new CustomEvent('payment:success', {
                detail: { orderId: data.orderId, paidAmount: data.paidAmount, courseId }
            }));
        } catch (e) {
            toast(e.message || '결제 처리 중 오류');
        } finally {
            paying = false;
        }
    };

    // ESC로 닫기
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') closeModal();
    });
})();
