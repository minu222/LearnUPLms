// src/main/resources/static/js/courseLecture.js
// 강의 목록 탭(개인강의/다수강의/VOD) 렌더 + 구매/장바구니 상태 플래그 + 페이지네이션(리스트 5개/카드 9개)
// 카드/리스트 클릭시 상세 이동 막음 (버튼만 동작)
(function () {
    'use strict';

    // ✅ 스크립트 중복 로드 방지
    if (window.__courseLectureInit__) return;
    window.__courseLectureInit__ = true;

    /** ====================== 설정 ====================== */
    const API_URL = '/api/courses';
    const LIST_ITEMS = 5;  // 리스트형 페이지당 개수
    const CARD_ITEMS = 9;  // 카드형 페이지당 개수 (요청 반영)

    // 탭 → DB 카테고리 매핑
    const TAB_TO_CATEGORY = {
        vod: 'VOD',
        personal: '개인강의',
        multi: '다수강의',
    };

    // DOM
    const $id = (id) => document.getElementById(id);
    const listSection = $id('listSection');
    const cardSection = $id('cardSection');
    const searchInput = $id('searchInput');
    const sortFilter  = $id('sortFilter');

    // 페이지네이션 컨테이너 (없으면 자동 생성)
    const listPager = $id('listPagination') || createPagerAfter(listSection, 'listPagination');
    const cardPager = $id('cardPagination') || createPagerAfter(cardSection, 'cardPagination');

    // 상태
    let currentTab = 'vod';
    let lastFetched = [];    // 서버 원본
    let fetchSeq = 0;        // 오래된 응답 무시용

    // 페이지 상태
    let listPage = 1;
    let cardPage = 1;

    // ✅ 상태 플래그: 이미 구매/장바구니
    let purchasedSet = new Set();
    let inCartSet    = new Set();

    // ✅ 장바구니 중복 요청 잠금 (코스ID 기준)
    const inflightCart = new Set();

    /** ====================== 유틸 ====================== */
    function escapeHtml(s) {
        return String(s ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function formatCurrency(v) {
        if (v === null || v === undefined) return '';
        const num = typeof v === 'number' ? v : Number(String(v).replace(/[^\d.-]/g, ''));
        if (!isFinite(num)) return '';
        return num.toLocaleString('ko-KR');
    }

    function buildImg(course) {
        return (
            course.image_url ||
            `https://picsum.photos/300/140?random=${course.course_id || Math.floor(Math.random() * 10000)}`
        );
    }

    function buildPeriod(course) {
        const left = (course.created_at || '').toString().slice(0, 10);
        const right = (course.expiry_date || '').toString().slice(0, 10);
        if (left && right) return `${left} ~ ${right}`;
        return right || left || '';
    }

    function popularityScore(c) {
        const students = Number(c.student_count ?? 0);
        const rating = Number(c.avg_rating ?? 0);
        return students * 1 + rating * 10;
    }

    function showToast(msg) {
        try {
            const t = document.createElement('div');
            t.textContent = msg;
            t.style.cssText =
                'position:fixed;left:50%;top:70px;transform:translateX(-50%);' +
                'background:#333;color:#fff;padding:10px 14px;border-radius:8px;' +
                'z-index:2147483647;opacity:.95';
            document.body.appendChild(t);
            setTimeout(() => t.remove(), 1600);
        } catch {
            alert(msg);
        }
    }

    // 작은 배지 유틸
    function makeBadge(text, cls) {
        const s = document.createElement('span');
        s.className = `badge badge-flag ${cls || 'badge-gray'}`;
        s.textContent = text;
        s.style.marginLeft = '6px';
        return s;
    }

    // 페이지네이션 컨테이너 생성
    function createPagerAfter(anchorEl, id) {
        const p = document.createElement('div');
        p.id = id;
        p.className = 'pagination';
        p.style.cssText = 'display:flex;gap:6px;flex-wrap:wrap;align-items:center;justify-content:center;margin:14px 0;';
        if (anchorEl && anchorEl.parentNode) {
            anchorEl.parentNode.insertBefore(p, anchorEl.nextSibling);
        } else {
            document.body.appendChild(p);
        }
        return p;
    }

    // 페이지네이션 렌더
    function renderPager({ total, perPage, current, container, onChange }) {
        if (!container) return;
        container.innerHTML = '';
        const totalPages = Math.max(1, Math.ceil(total / perPage));

        if (totalPages <= 1) return; // 한 페이지면 숨김

        function addBtn(label, page, disabled = false, isActive = false) {
            const b = document.createElement('button');
            b.type = 'button';
            b.textContent = label;
            b.style.cssText =
                'min-width:34px;height:34px;padding:0 8px;border:1px solid #ddd;background:#fff;border-radius:8px;cursor:pointer;';
            if (disabled) {
                b.disabled = true;
                b.style.opacity = '0.5';
                b.style.cursor = 'not-allowed';
            }
            if (isActive) {
                b.style.fontWeight = '700';
                b.style.borderColor = '#ffb400';
            }
            b.addEventListener('click', () => onChange(page));
            container.appendChild(b);
        }

        function addEllipsis() {
            const s = document.createElement('span');
            s.textContent = '…';
            s.style.cssText = 'padding:0 4px;color:#777;';
            container.appendChild(s);
        }

        addBtn('«', 1, current === 1);
        addBtn('‹', Math.max(1, current - 1), current === 1);

        // 페이지 번호 축약 표시
        const windowSize = 2;
        const pages = [];
        for (let p = 1; p <= totalPages; p++) {
            if (p === 1 || p === totalPages || (p >= current - windowSize && p <= current + windowSize)) {
                pages.push(p);
            }
        }
        let prev = 0;
        pages.forEach((p) => {
            if (prev && p - prev > 1) addEllipsis();
            addBtn(String(p), p, false, p === current);
            prev = p;
        });

        addBtn('›', Math.min(totalPages, current + 1), current === totalPages);
        addBtn('»', totalPages, current === totalPages);
    }

    /** ====================== 서버 통신 ====================== */
    async function fetchCoursesFromDB({ tab = currentTab, q = searchInput?.value || '' } = {}) {
        const category = TAB_TO_CATEGORY[tab] || 'VOD';
        const params = new URLSearchParams();
        params.set('status', 'published');
        params.set('category', category);
        if (q && q.trim()) params.set('q', q.trim());

        const url = `${API_URL}?${params}`;
        const mySeq = ++fetchSeq;

        setLoading(true);
        try {
            const res = await fetch(url, { method: 'GET', headers: { Accept: 'application/json' } });
            if (mySeq !== fetchSeq) return []; // 뒤늦은 응답 무시

            if (res.status === 401) {
                setLoading(false);
                throw new Error('로그인이 필요합니다.');
            }
            if (!res.ok) {
                const txt = await res.text().catch(() => '');
                throw new Error(txt || '목록 조회 실패');
            }

            const data = await res.json();
            if (!Array.isArray(data)) {
                if (data && data.ok === false && data.message) throw new Error(data.message);
                return [];
            }
            return data;
        } catch (err) {
            console.error('[courseLecture] fetch error:', err);
            showToast(err.message || '목록을 불러오지 못했습니다.');
            return [];
        } finally {
            if (mySeq === fetchSeq) setLoading(false);
        }
    }

    // ✅ 내 구매/장바구니 플래그 조회
    async function loadFlags() {
        try {
            const [eRes, cRes] = await Promise.all([
                fetch('/api/enrollments/my', { headers: { Accept: 'application/json' } }),
                fetch('/api/cart',           { headers: { Accept: 'application/json' } })
            ]);
            if ([eRes, cRes].some(r => r.status === 401)) return;

            const enrolls = eRes.ok ? await eRes.json() : [];
            const cart    = cRes.ok ? await cRes.json()   : [];

            purchasedSet = new Set((Array.isArray(enrolls) ? enrolls : [])
                .map(x => Number(x.course_id ?? x.courseId)).filter(Boolean));
            inCartSet = new Set((Array.isArray(cart) ? cart : [])
                .map(x => Number(x.course_id ?? x.courseId)).filter(Boolean));

            applyFlagsToPage();
        } catch (e) {
            console.warn('[courseLecture] loadFlags error', e);
        }
    }

    function setLoading(isLoading) {
        if (!listSection || !cardSection) return;
        if (isLoading) {
            listSection.innerHTML = `<div style="padding:18px;color:#666;">불러오는 중...</div>`;
            cardSection.innerHTML = `<div style="padding:18px;color:#666;">불러오는 중...</div>`;
        }
    }


// 유틸 함수 추가
    function syncPagerVisibility() {
        const lp = document.getElementById('listPagination');
        const cp = document.getElementById('cardPagination');
        if (!lp && !cp) return;

        const listShown = listSection && getComputedStyle(listSection).display !== 'none';
        const cardShown = cardSection && getComputedStyle(cardSection).display !== 'none';

        if (lp) lp.style.display = listShown ? 'flex' : 'none';
        if (cp) cp.style.display = cardShown ? 'flex' : 'none';
    }

    /** ====================== 렌더 ====================== */
    function render() {
        if (!listSection || !cardSection) return;

        const q = (searchInput?.value || '').toLowerCase();
        const sort = sortFilter?.value || '';

        // 검색 필터
        let filtered = lastFetched.filter((c) => {
            const title = (c.title || '').toLowerCase();
            const desc = (c.description || '').toLowerCase();
            const inst = (c.instructor_name || '').toLowerCase();
            return !q || title.includes(q) || desc.includes(q) || inst.includes(q);
        });

        // 정렬
        if (sort === 'latest') {
            filtered.sort(
                (a, b) => new Date(b.updated_at || b.created_at || 0) - new Date(a.updated_at || a.created_at || 0)
            );
        } else if (sort === 'oldest') {
            filtered.sort(
                (a, b) => new Date(a.updated_at || a.created_at || 0) - new Date(b.updated_at || b.created_at || 0)
            );
        } else if (sort === 'popular') {
            filtered.sort((a, b) => popularityScore(b) - popularityScore(a));
        }

        // ===== 리스트형 =====
        const listTotal = filtered.length;
        const listTotalPages = Math.max(1, Math.ceil(listTotal / LIST_ITEMS));
        if (listPage > listTotalPages) listPage = listTotalPages;
        const listStart = (listPage - 1) * LIST_ITEMS;
        const listSlice = filtered.slice(listStart, listStart + LIST_ITEMS);

        listSection.innerHTML = '';
        if (listSlice.length === 0) {
            listSection.innerHTML = `<div style="padding:18px;color:#888;">표시할 강의가 없습니다.</div>`;
        } else {
            listSlice.forEach((c) => {
                const img = buildImg(c);
                const period = buildPeriod(c);
                const priceNum = c.is_free ? 0 : Number(c.price ?? 0) || 0;
                const price = c.is_free ? '무료' : c.price != null ? `${formatCurrency(c.price)}원` : '';
                const level = escapeHtml(c.level || '');

                const div = document.createElement('div');
                div.className = 'lecture';
                // 상세이동 방지용
                div.style.cursor = 'default';

                // 결제/장바구니용 데이터
                div.dataset.title = c.title || '';
                div.dataset.price = String(priceNum);
                div.dataset.img = img;
                div.dataset.instructor = c.instructor_name || '';
                div.dataset.desc = c.description || '';
                div.dataset.qty = '1';
                div.dataset.courseId = String(c.course_id || '');

                div.innerHTML = `
          <img src="${img}" alt="강의 이미지">
          <div class="lecture-content">
            <h3>${escapeHtml(c.title || '')}</h3>
            <p>${escapeHtml(c.description || '')}</p>
            <div class="info">
              ${c.instructor_name ? `강사: ${escapeHtml(c.instructor_name)} | ` : ''}${level ? `${level} | ` : ''}${period}${price ? ` | ${price}` : ''}
            </div>
            <div class="actions">
              <button type="button" class="btn-buy">구매하기</button>
              <button type="button" class="btn-cart">장바구니</button>
            </div>
          </div>
        `;
                // ✅ 상세 페이지로 이동하는 클릭 핸들러 제거 (버튼만 동작)
                listSection.appendChild(div);
            });
        }

        renderPager({
            total: listTotal,
            perPage: LIST_ITEMS,
            current: listPage,
            container: listPager,
            onChange: (p) => { listPage = p; render(); scrollIntoView(listSection); }
        });

        // ===== 카드형 =====
        const cardTotal = filtered.length;
        const cardTotalPages = Math.max(1, Math.ceil(cardTotal / CARD_ITEMS));
        if (cardPage > cardTotalPages) cardPage = cardTotalPages;
        const cardStart = (cardPage - 1) * CARD_ITEMS;
        const cardSlice = filtered.slice(cardStart, cardStart + CARD_ITEMS);

        cardSection.innerHTML = '';
        if (cardSlice.length === 0) {
            cardSection.innerHTML = `<div style="padding:18px;color:#888;">표시할 강의가 없습니다.</div>`;
        } else {
            cardSlice.forEach((c) => {
                const img = buildImg(c);
                const priceNum = c.is_free ? 0 : Number(c.price ?? 0) || 0;

                const div = document.createElement('div');
                div.className = 'card';
                // 상세이동 방지용
                div.style.cursor = 'default';

                // 결제/장바구니용 데이터
                div.dataset.title = c.title || '';
                div.dataset.price = String(priceNum);
                div.dataset.img = img;
                div.dataset.instructor = c.instructor_name || '';
                div.dataset.desc = c.description || '';
                div.dataset.qty = '1';
                div.dataset.courseId = String(c.course_id || '');

                div.innerHTML = `
          <img src="${img}" alt="강의 이미지">
          <div class="card-content">
            <h3>${escapeHtml(c.title || '')}</h3>
            <p>${escapeHtml(c.description || '')}</p>
            <div class="level">${c.instructor_name ? `강사: ${escapeHtml(c.instructor_name)}` : ''}</div>
            <div class="actions">
              <button type="button" class="btn-buy">구매하기</button>
              <button type="button" class="btn-cart">장바구니</button>
            </div>
          </div>
        `;
                // ✅ 상세 페이지로 이동하는 클릭 핸들러 제거 (버튼만 동작)
                cardSection.appendChild(div);
            });
        }

        renderPager({
            total: cardTotal,
            perPage: CARD_ITEMS,
            current: cardPage,
            container: cardPager,
            onChange: (p) => { cardPage = p; render(); scrollIntoView(cardSection); }
        });

        // ✅ 렌더 후 현재 페이지의 강의 카드들에 플래그 적용
        applyFlagsToPage();
    }
    syncPagerVisibility();

    function scrollIntoView(el) {
        try {
            el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } catch {}
    }

    // ✅ 현재 DOM에 있는 모든 강의 카드/리스트에 구매/장바구니 플래그 반영
    function applyFlagsToPage() {
        const cards = document.querySelectorAll('.lecture[data-course-id], .card[data-course-id]');
        cards.forEach(card => markCourseEl(card));
    }

    function markCourseEl(card) {
        const cid = Number(card?.dataset?.courseId || 0);
        if (!cid) return;
        const isPurchased = purchasedSet.has(cid);
        const isInCart = inCartSet.has(cid);

        const buyBtn  = card.querySelector('.btn-buy');
        const cartBtn = card.querySelector('.btn-cart');

        // 이전 배지 제거
        card.querySelectorAll('.badge-flag').forEach(n => n.remove());

        if (buyBtn) {
            buyBtn.disabled = false;
            buyBtn.textContent = '구매하기';
            buyBtn.removeAttribute('title');

            if (isPurchased) {
                buyBtn.disabled = true;
                buyBtn.textContent = '구매완료';
                buyBtn.title = '이미 구매한 강의입니다.';
                buyBtn.insertAdjacentElement('afterend', makeBadge('구매완료', 'badge-done'));
            } else if (isInCart) {
                buyBtn.disabled = true;
                buyBtn.textContent = '구매 불가';
                buyBtn.title = '이미 장바구니에 있는 강의입니다.';
                buyBtn.insertAdjacentElement('afterend', makeBadge('장바구니에 있음', 'badge-gray'));
            }
        }

        if (cartBtn) {
            cartBtn.disabled = false;
            cartBtn.textContent = '장바구니';
            cartBtn.removeAttribute('title');

            if (isPurchased) {
                cartBtn.disabled = true;
                cartBtn.textContent = '장바구니 불가';
                cartBtn.title = '이미 구매한 강의입니다.';
            } else if (isInCart) {
                cartBtn.disabled = true;
                cartBtn.textContent = '장바구니 있음';
                cartBtn.title = '이미 장바구니에 있는 강의입니다.';
            }
        }
    }

    /** ====================== 탭/보기 전환 ====================== */
    async function changeTab(tab) {
        currentTab = tab;
        // 탭 버튼 active 토글
        document.querySelectorAll('.tabs button').forEach((b) => b.classList.remove('active'));
        const btn = document.querySelector(`.tabs button[onclick="changeTab('${tab}')"]`);
        if (btn) btn.classList.add('active');

        // 페이지 리셋
        listPage = 1;
        cardPage = 1;

        lastFetched = await fetchCoursesFromDB({ tab, q: searchInput?.value || '' });
        render();
        // 탭 전환 시 최신 플래그도 새로 받아 적용
        await loadFlags();
    }

    function showList() {
        const list = $id('listSection');
        const card = $id('cardSection');
        const lp   = $id('listPagination') || listPager;
        const cp   = $id('cardPagination') || cardPager;
        if (list && card) {
            list.style.display = 'flex';
            card.style.display = 'none';
        }
        if (lp) lp.style.display = 'flex';
        if (cp) cp.style.display = 'none';
        syncPagerVisibility();
    }

    function showCard() {
        const list = $id('listSection');
        const card = $id('cardSection');
        const lp   = $id('listPagination') || listPager;
        const cp   = $id('cardPagination') || cardPager;
        if (list && card) {
            list.style.display = 'none';
            card.style.display = 'grid';
        }
        if (lp) lp.style.display = 'none';
        if (cp) cp.style.display = 'flex';

        syncPagerVisibility();
    }

    // 전역 노출(HTML의 onclick 대비)
    window.changeTab = changeTab;
    window.showList = showList;
    window.showCard = showCard;

    /** ====================== 초기화 ====================== */
    async function init() {
        // 초기 페이지 리셋
        listPage = 1;
        cardPage = 1;

        lastFetched = await fetchCoursesFromDB({ tab: currentTab, q: searchInput?.value || '' });
        render();
        await loadFlags(); // ✅ 최초 플래그 로딩 후 표시

        searchInput?.addEventListener(
            'input',
            debounce(async () => {
                listPage = 1; cardPage = 1;
                lastFetched = await fetchCoursesFromDB({ tab: currentTab, q: searchInput.value });
                render();
                await loadFlags(); // 검색 후에도 최신 플래그 반영
            }, 250)
        );

        sortFilter?.addEventListener('change', async () => {
            listPage = 1; cardPage = 1;
            render();
            await loadFlags();
        });

        // 기본은 리스트/카드 모두 보이도록 두되, CSS/탭 UI에 맞춰 토글 사용
        // 필요시 showList() 또는 showCard()를 초기로 호출 가능
    }

    function debounce(fn, ms) {
        let t;
        return function (...args) {
            clearTimeout(t);
            t = setTimeout(() => fn.apply(this, args), ms);
        };
    }

    document.addEventListener('DOMContentLoaded', init);

    /** ====================== 결제 모달 로더 ====================== */
    async function ensurePaymentModal() {
        if (typeof window.goToPayment === 'function') return true;
        await loadScript('/js/paymentModal.js?v=' + Date.now());
        return (typeof window.goToPayment === 'function');
    }
    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const s = document.createElement('script');
            s.src = src;
            s.onload = () => resolve();
            s.onerror = () => reject(new Error('paymentModal.js 로드 실패: ' + src));
            document.head.appendChild(s);
        });
    }

    /** ====================== 구매/장바구니 버튼 전역 위임 ====================== */
    // 구매하기
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-buy');
        if (!btn) return;

        const card = btn.closest('.lecture, .card');
        if (!card) return;

        const cid = Number(card.dataset.courseId || 0);
        if (purchasedSet.has(cid)) { showToast('이미 구매한 강의입니다.'); return; }
        if (inCartSet.has(cid))    { showToast('이미 장바구니에 있는 강의입니다.'); return; }

        const { title, price, img, instructor, desc, qty } = card.dataset;

        try {
            // 가벼운 로그인 체크
            const ping = await fetch('/api/enrollments/my', { headers:{Accept:'application/json'} });
            if (ping.status === 401) { showToast('로그인이 필요합니다.'); setTimeout(()=>location.href='/user/login', 400); return; }

            const ok = await ensurePaymentModal();
            if (!ok) throw new Error('결제 모듈 로드를 확인해주세요.');

            window.goToPayment(
                title,
                Number(price || 0),
                Number(qty || 1) || 1,
                img,
                instructor,
                desc,
                cid
            );
        } catch (err) {
            showToast(err.message || '결제 모듈이 아직 준비되지 않았습니다.');
        }
    });

    // 장바구니
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-cart');
        if (!btn) return;

        const card = btn.closest('.lecture, .card');
        if (!card) return;

        const cid = Number(card.dataset.courseId || 0);
        if (purchasedSet.has(cid)) { showToast('이미 구매한 강의입니다.'); return; }
        if (inCartSet.has(cid))    { showToast('이미 장바구니에 있는 강의입니다.'); return; }
        if (!cid) return;

        // ✅ 동일 코스에 대한 동시/중복 요청 차단
        if (inflightCart.has(cid)) return;
        inflightCart.add(cid);
        const prevDisabled = btn.disabled;
        btn.disabled = true;

        try {
            const res = await fetch('/api/cart', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({ courseId: cid })
            });
            if (res.status === 401) {
                showToast('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
                setTimeout(()=>location.href='/user/login', 400);
                return;
            }
            if (res.status === 409) { // 서버에서 중복 장바구니 차단 시
                inCartSet.add(cid);
                markCourseEl(card);
                showToast('이미 장바구니에 있는 강의입니다.');
                return;
            }
            if (!res.ok) throw new Error(await res.text().catch(()=> '장바구니 담기에 실패했습니다.'));

            // 성공 시 inCartSet 갱신 & UI 반영
            inCartSet.add(cid);
            markCourseEl(card);
            showToast('장바구니에 담았습니다.');
        } catch (err) {
            showToast(err.message || '오류가 발생했습니다.');
        } finally {
            inflightCart.delete(cid);
            // 이미 장바구니 상태면 비활성 유지, 아니면 원복
            btn.disabled = inCartSet.has(cid) ? true : prevDisabled;
        }
    });

})();
