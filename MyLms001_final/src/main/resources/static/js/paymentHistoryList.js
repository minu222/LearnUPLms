// /js/paymentHistory.list.js
(function () {
    'use strict';
    const $ = (s, p = document) => p.querySelector(s);
    const tbody = $('#orderTable tbody');
    if (!tbody) { console.error('[paymentHistory] #orderTable tbody 없음'); return; }
    const pagination  = $('#pagination');
    const searchInput = $('#searchInput');

    let rows = [], view = [];
    let page = 1;
    const PAGE = 10;
    let currentFilter = 'all'; // 기본: 주문대기만

    const fmtWon = v => (Number(v || 0)).toLocaleString('ko-KR') + '원';
    const esc = s => String(s ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;')
        .replaceAll('>','&gt;').replaceAll('"','&quot;')
        .replaceAll("'","&#39;");
    function fmtDate(s){
        if(!s) return '';
        const d=new Date(s); if(isNaN(d)) return String(s).slice(0,19).replace('T',' ');
        const yy=d.getFullYear(),mm=String(d.getMonth()+1).padStart(2,'0'),
            dd=String(d.getDate()).padStart(2,'0'),hh=String(d.getHours()).padStart(2,'0'),
            mi=String(d.getMinutes()).padStart(2,'0');
        return `${yy}-${mm}-${dd} ${hh}:${mi}`;
    }

    async function load(){
        tbody.innerHTML = `<tr><td colspan="9" style="padding:16px;color:#666;">불러오는 중...</td></tr>`;
        try{
            const [oRes, cRes] = await Promise.all([
                fetch('/api/orders/my', {headers:{Accept:'application/json'}}),
                fetch('/api/cart',      {headers:{Accept:'application/json'}}),
            ]);
            if ([oRes,cRes].some(r=>r.status===401)){ alert('로그인이 필요합니다.'); location.href='/user/login'; return; }
            if (!oRes.ok) throw new Error('주문 목록 조회 실패');
            if (!cRes.ok) throw new Error('장바구니 목록 조회 실패');

            const orders = await oRes.json();
            const cart   = await cRes.json();

            const orderRows = (Array.isArray(orders)?orders:[]).map(o=>{
                const cnt = Number(o.item_count||0);
                const isSingle = cnt===1;
                const firstTitle = o.first_title || o.course_title ||
                    (Array.isArray(o.items)&&o.items[0]?.title) || null;
                return {
                    type:'order',
                    orderId:o.order_id,
                    title: (isSingle && firstTitle) ? firstTitle : `주문 #${o.order_id} (강의 ${cnt}건)`,
                    unit:  isSingle ? Number(o.total_amount||0) : null,
                    qty:   cnt||0,
                    total: Number(o.total_amount||0)||0,
                    method:o.payment_method||'-',
                    date:  o.created_at,
                    status:String(o.status||'').toLowerCase()==='paid' ? 'completed' : 'pending',
                    link:  `/myclass/payment?orderId=${encodeURIComponent(o.order_id)}`
                };
            });

            const cartRows = (Array.isArray(cart)?cart:[]).map(r=>({
                type:'cart',
                courseId: Number(r.course_id),
                title: r.title || '(제목 없음)',
                unit: r.is_free ? 0 : Number(r.price||0),
                qty: 1,
                total: r.is_free ? 0 : Number(r.price||0),
                method:'-',
                date: r.added_at || r.created_at,
                status:'pending',
                link:`/course/detail/${r.course_id}`
            }));

            rows = [...orderRows, ...cartRows];
            apply();
        }catch(e){
            tbody.innerHTML = `<tr><td colspan="9" style="padding:16px;color:#c00;">${esc(e.message||'오류가 발생했습니다.')}</td></tr>`;
        }
    }

    function apply(){
        const q=(searchInput?.value||'').trim().toLowerCase();
        view = rows.filter(r=>{
            if(currentFilter==='completed' && r.status!=='completed') return false;
            if(currentFilter==='pending'   && r.status!=='pending')   return false;
            return !q || r.title.toLowerCase().includes(q) || (r.method||'').toLowerCase().includes(q);
        });
        page=1; render();
    }

    function render(){
        const start=(page-1)*PAGE, list=view.slice(start,start+PAGE);
        if(!list.length){
            tbody.innerHTML=`<tr><td colspan="9" style="padding:16px;color:#888;">표시할 항목이 없습니다.</td></tr>`;
            return renderPager();
        }
        tbody.innerHTML = list.map((r,i)=>{
            const no=start+i+1;
            const unit=(r.unit==null) ? '-' : (r.unit===0 ? '무료' : fmtWon(r.unit));
            const total=r.total===0 ? '무료' : fmtWon(r.total);
            const statText=r.status==='completed'?'구매완료':'주문대기';
            const statClass=r.status==='completed'?'completed':'pending';
            const actionHtml = (r.type==='order')
                ? `<span class="badge badge-done">구매완료</span>`      // 주문완료면 텍스트만
                : `<button class="buy-now" data-course-id="${r.courseId}" data-title="${esc(r.title)}" data-price="${Number(r.total||0)}">구매하기</button>`; // 주문대기는 구매하기만

            return `
        <tr>
          <td>${no}</td>
          <td>${esc(r.title)}</td>
          <td>${unit}</td>
          <td>${r.qty}</td>
          <td>${total}</td>
          <td>${esc(r.method||'-')}</td>
          <td>${fmtDate(r.date)}</td>
          <td><span class="status ${statClass}">${statText}</span></td>
          <td>${actionHtml}</td>
        </tr>`;
        }).join('');
        renderPager();
    }

    function renderPager(){
        const pages=Math.max(1, Math.ceil(view.length/PAGE));
        if (page>pages) page=pages;
        let html=''; for(let i=1;i<=pages;i++) html+=`<button class="page-btn${i===page?' active':''}" data-p="${i}">${i}</button>`;
        pagination && (pagination.innerHTML=html);
    }

    // 결제 모달 로더
    function loadScript(src){ return new Promise((res,rej)=>{ const s=document.createElement('script'); s.src=src; s.onload=res; s.onerror=()=>rej(new Error('paymentModal.js 로드 실패: '+src)); document.head.appendChild(s); }); }
    async function ensurePaymentModal(){ if(typeof window.goToPayment==='function') return true; try{ await loadScript('/js/paymentModal.js?v='+Date.now()); }catch(e){ console.error(e); return false; } return typeof window.goToPayment==='function'; }

    // 이벤트
    document.addEventListener('click', async (e)=>{
        const p=e.target.closest('.page-btn'); if(p){ page=Number(p.dataset.p)||1; return render(); }

        const buy=e.target.closest('.buy-now');
        if(buy){
            const courseId=Number(buy.dataset.courseId||0);
            const title=buy.dataset.title||'';
            const price=Number(buy.dataset.price||0);
            try{
                const ping=await fetch('/api/enrollments/my',{headers:{Accept:'application/json'}});
                if(ping.status===401){ alert('로그인이 필요합니다.'); location.href='/user/login'; return; }
                const ok=await ensurePaymentModal(); if(!ok) throw new Error('결제 모듈 로드 오류');
                window.goToPayment(title, price, 1, '', '', '', courseId);
            }catch(err){ alert(err.message||'결제 모듈 로드 오류'); }
            return;
        }

        const f=e.target.closest('.filter-buttons button');
        if(f){ document.querySelectorAll('.filter-buttons button').forEach(b=>b.classList.remove('active'));
            f.classList.add('active'); currentFilter=f.getAttribute('data-filter')||'all'; page=1; apply(); }
    });

    searchInput && searchInput.addEventListener('input', ()=>{ page=1; apply(); });
    document.addEventListener('payment:success', load);

    (function ready(fn){ if(document.readyState==='loading') document.addEventListener('DOMContentLoaded', fn); else fn(); })(load);
})();
